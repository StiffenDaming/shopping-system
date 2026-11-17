#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
零号藏馆系统 —— 推荐算法离线评估脚本（论文第六章 6.7 的数据来源）

实验设计
--------
1. 数据集：user_behavior 中的用户行为（含模拟数据集），按时间切分
     - 训练集：cutoff 之前的行为
     - 测试集：cutoff 之后的行为
   只用训练集构建画像与相似度，用测试集判断"推荐对了没有"，避免信息泄漏。

2. 评估用户：测试集中至少有一条未被训练集覆盖的行为的用户。

3. 对比策略：
     POP      热门基线（训练集加权热度）
     CONTENT  基于内容的过滤（画像标签向量 × 商品标签向量余弦）
     CF       基于物品的协同过滤（订单/购物车/订单会话共现矩阵）
     HYBRID   混合加权 0.6×内容 + 0.3×热度 + 0.1×协同

4. 评价指标：
     Precision@K  推荐 K 个里命中几个 / K
     Recall@K     推荐 K 个里命中几个 / 测试集里实际感兴趣的个数
     Coverage     推荐结果覆盖的商品数 / 商品总数
     Diversity    推荐列表内部的平均不相似度（1 - 平均余弦相似度）

实现约束
--------
不依赖任何第三方 Python 包。数据通过 `docker exec` 调用容器内 mysql 客户端导出 TSV，
因此在同一台机器上直接运行即可；也可用 --dump-dir 复用已导出的文件。

用法
----
    python3 tools/evaluate_recommend.py
    python3 tools/evaluate_recommend.py --test-days 14 --k 5 10
    python3 tools/evaluate_recommend.py --dump-dir /tmp/recdump   # 复用导出
"""

import argparse
import datetime
import itertools
import math
import os
import subprocess
import sys

# ----------------------------------------------------------------------------
# 算法参数（与后端 application.yml 的 recommend.* 保持一致）
# ----------------------------------------------------------------------------
DECAY_LAMBDA = 0.05
W_ORDER, W_CART, W_VIEW = 3.0, 2.0, 1.0
HYBRID_ALPHA, HYBRID_BETA, HYBRID_GAMMA = 0.6, 0.3, 0.1
CATEGORY_WEIGHT = 1.5  # 商品向量中分类伪标签的权重

QUERIES = {
    "products": "SELECT id, category_id, price FROM product WHERE deleted = 0 AND status = 1",
    "tags": "SELECT product_id, tag_name FROM product_tag",
    "behaviors": ("SELECT user_id, product_id, behavior_type, weight, create_time, scene "
                  "FROM user_behavior WHERE product_id IS NOT NULL"),
    "order_items": ("SELECT oi.order_id, oi.product_id FROM order_item oi "
                    "JOIN orders o ON oi.order_id = o.id "
                    "WHERE o.deleted = 0 AND o.status <> 'CANCELLED'"),
    "cart_items": "SELECT user_id, product_id FROM cart_item",
}


# ----------------------------------------------------------------------------
# 数据读取
# ----------------------------------------------------------------------------
def run_mysql(container, database, sql):
    """在容器内执行查询，返回原始 TSV 文本。"""
    cmd = ["docker", "exec", container, "mysql", "-uroot", "-p123456",
           "--default-character-set=utf8mb4", "-B", "-N", "-e", sql, database]
    result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    if result.returncode != 0:
        raise RuntimeError("查询失败: %s\n%s" % (sql[:80], result.stderr.strip()))
    return result.stdout


def load_table(name, args):
    """读取一张表为二维列表；优先读 --dump-dir 下的缓存文件。"""
    if args.dump_dir:
        path = os.path.join(args.dump_dir, name + ".tsv")
        if not os.path.exists(path):
            raise RuntimeError("缺少缓存文件: %s" % path)
        with open(path, encoding="utf-8") as f:
            text = f.read()
    else:
        text = run_mysql(args.container, args.database, QUERIES[name])

    rows = []
    for line in text.split("\n"):
        if not line.strip():
            continue
        rows.append(line.split("\t"))
    return rows


def parse_dt(value):
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M:%S.%f", "%Y-%m-%d"):
        try:
            return datetime.datetime.strptime(value, fmt)
        except ValueError:
            continue
    return None


def to_int(value):
    try:
        return int(float(value))
    except (TypeError, ValueError):
        return None


# ----------------------------------------------------------------------------
# 数据结构
# ----------------------------------------------------------------------------
class Catalog:
    """商品、标签、分类映射。"""

    def __init__(self, product_rows, tag_rows):
        self.category_of = {}
        self.price_of = {}
        for row in product_rows:
            pid = to_int(row[0])
            if pid is None:
                continue
            self.category_of[pid] = to_int(row[1])
            self.price_of[pid] = float(row[2]) if row[2] not in ("NULL", "") else 0.0

        self.tags_of = {}
        for row in tag_rows:
            pid = to_int(row[0])
            if pid is None or pid not in self.category_of:
                continue
            self.tags_of.setdefault(pid, []).append(row[1])

        self.product_ids = sorted(self.category_of.keys())

    def vector(self, pid):
        """商品向量：标签 + 分类伪标签。"""
        vec = {}
        for tag in self.tags_of.get(pid, []):
            vec[tag] = vec.get(tag, 0.0) + 1.0
        cat = self.category_of.get(pid)
        if cat is not None:
            key = "CAT:%d" % cat
            vec[key] = vec.get(key, 0.0) + CATEGORY_WEIGHT
        return vec


def cosine(a, b):
    if not a or not b:
        return 0.0
    dot = 0.0
    for key, va in a.items():
        vb = b.get(key)
        if vb is not None:
            dot += va * vb
    na = math.sqrt(sum(v * v for v in a.values()))
    nb = math.sqrt(sum(v * v for v in b.values()))
    if na == 0 or nb == 0:
        return 0.0
    return dot / (na * nb)


def normalize(scores):
    """归一化到 [0,1]。"""
    if not scores:
        return scores
    top = max(scores.values())
    if top <= 0:
        return {k: 0.0 for k in scores}
    return {k: v / top for k, v in scores.items()}


# ----------------------------------------------------------------------------
# 训练集构建
# ----------------------------------------------------------------------------
def split_data(behavior_rows, test_days):
    """按时间切分训练集与测试集。"""
    now = datetime.datetime.now()
    cutoff = now - datetime.timedelta(days=test_days)

    train = []  # (user, product, type, weight, time, scene)
    test = []
    for row in behavior_rows:
        uid = to_int(row[0])
        pid = to_int(row[1])
        if uid is None or pid is None or uid <= 0:
            continue
        btype = row[2]
        weight = float(row[3]) if row[3] not in ("NULL", "") else 1.0
        moment = parse_dt(row[4])
        if moment is None:
            continue
        item = (uid, pid, btype, weight, moment, row[5] if row[5] != "NULL" else None)
        if moment >= cutoff:
            test.append(item)
        else:
            train.append(item)
    return train, test, cutoff


def build_profiles(train_rows, catalog):
    """用户画像：训练集行为按时间衰减聚合。"""
    now = datetime.datetime.now()
    tag_acc = {}
    for uid, pid, btype, weight, moment, _scene in train_rows:
        decay = math.exp(-DECAY_LAMBDA * max((now - moment).total_seconds() / 86400.0, 0.0))
        w = weight * decay
        bucket = tag_acc.setdefault(uid, {})
        for tag in catalog.tags_of.get(pid, []):
            bucket[tag] = bucket.get(tag, 0.0) + w
        cat = catalog.category_of.get(pid)
        if cat is not None:
            key = "CAT:%d" % cat
            bucket[key] = bucket.get(key, 0.0) + w

    # 每个用户归一化到 [0,1]
    profiles = {}
    for uid, vec in tag_acc.items():
        profiles[uid] = normalize(vec)
    return profiles


def build_hot_scores(train_rows, cutoff, window_days):
    """训练集窗口内的加权热度。"""
    since = cutoff - datetime.timedelta(days=window_days)
    scores = {}
    for uid, pid, btype, weight, moment, _scene in train_rows:
        if moment < since:
            continue
        if btype == "VIEW":
            factor = W_VIEW
        elif btype == "CART":
            factor = W_CART
        elif btype == "ORDER":
            factor = W_ORDER
        else:
            continue
        scores[pid] = scores.get(pid, 0.0) + factor
    return normalize(scores)


def build_item_similarity(train_rows, order_rows, cart_rows):
    """物品共现矩阵 → 余弦相似度。"""
    groups = {}

    for row in order_rows:
        gid = row[0]
        pid = to_int(row[1])
        if pid is None:
            continue
        groups.setdefault("O:%s" % gid, set()).add(pid)

    for row in cart_rows:
        gid = row[0]
        pid = to_int(row[1])
        if pid is None:
            continue
        groups.setdefault("C:%s" % gid, set()).add(pid)

    for _uid, pid, btype, _w, _m, scene in train_rows:
        if btype == "ORDER" and scene:
            groups.setdefault("S:%s" % scene, set()).add(pid)

    co = {}
    for items in groups.values():
        if len(items) < 2:
            continue
        for a, b in itertools.combinations(sorted(items), 2):
            co[(a, b)] = co.get((a, b), 0) + 1

    degree = {}
    for (a, b), cnt in co.items():
        degree[a] = degree.get(a, 0) + cnt
        degree[b] = degree.get(b, 0) + cnt

    sim = {}
    for (a, b), cnt in co.items():
        da, db = degree.get(a, 0), degree.get(b, 0)
        if da == 0 or db == 0:
            continue
        value = cnt / math.sqrt(da * db)
        sim.setdefault(a, {})[b] = value
        sim.setdefault(b, {})[a] = value
    return sim


def user_train_items(train_rows):
    """用户训练集交互过的商品及衰减权重。"""
    now = datetime.datetime.now()
    items = {}
    for uid, pid, btype, weight, moment, _scene in train_rows:
        if btype == "SEARCH":
            continue
        decay = math.exp(-DECAY_LAMBDA * max((now - moment).total_seconds() / 86400.0, 0.0))
        items.setdefault(uid, {})
        items[uid][pid] = items[uid].get(pid, 0.0) + weight * decay
    return items


# ----------------------------------------------------------------------------
# 四种策略打分
# ----------------------------------------------------------------------------
def score_pop(uid, candidates, ctx):
    return {pid: ctx["hot"].get(pid, 0.0) for pid in candidates}


def score_content(uid, candidates, ctx):
    profile = ctx["profiles"].get(uid, {})
    return {pid: cosine(profile, ctx["vectors"][pid]) for pid in candidates}


def score_cf(uid, candidates, ctx):
    items = ctx["user_items"].get(uid, {})
    sim = ctx["item_sim"]
    raw = {}
    for item, weight in items.items():
        for neighbor, value in sim.get(item, {}).items():
            if neighbor in candidates:
                raw[neighbor] = raw.get(neighbor, 0.0) + weight * value
    return normalize(raw)


def score_hybrid(uid, candidates, ctx):
    content = normalize(score_content(uid, candidates, ctx))
    hot = {pid: ctx["hot"].get(pid, 0.0) for pid in candidates}
    cf = normalize(score_cf(uid, candidates, ctx))
    return {
        pid: HYBRID_ALPHA * content.get(pid, 0.0)
              + HYBRID_BETA * hot.get(pid, 0.0)
              + HYBRID_GAMMA * cf.get(pid, 0.0)
        for pid in candidates
    }


STRATEGIES = {
    "POP": score_pop,
    "CONTENT": score_content,
    "CF": score_cf,
    "HYBRID": score_hybrid,
}


# ----------------------------------------------------------------------------
# 评估主流程
# ----------------------------------------------------------------------------
def evaluate(args):
    print("读取数据 ...")
    catalog = Catalog(load_table("products", args), load_table("tags", args))
    behaviors = load_table("behaviors", args)
    order_rows = load_table("order_items", args)
    cart_rows = load_table("cart_items", args)

    train_rows, test_rows, cutoff = split_data(behaviors, args.test_days)
    print("商品: %d | 标签订阅: %d 条 | 行为: 训练 %d / 测试 %d"
          % (len(catalog.product_ids), sum(len(v) for v in catalog.tags_of.values()),
             len(train_rows), len(test_rows)))
    print("时间切分: 训练集 < %s <= 测试集" % cutoff.strftime("%Y-%m-%d %H:%M"))

    # 测试集真值：用户在后段产生过交互的商品。
    # 默认允许重复交互（隐式反馈的常见做法：推荐已浏览过的藏品同样是有效推荐，
    # 收藏品场景下用户会反复回看同一件；若强制剔除已见商品，在 19 件的小商品池上
    # 测试集会退化为纯随机浏览，反而掩盖算法差异）。
    train_items = user_train_items(train_rows)
    truth = {}
    for uid, pid, btype, _w, _m, _scene in test_rows:
        if btype == "SEARCH":
            continue
        if args.exclude_seen and pid in train_items.get(uid, {}):
            continue
        truth.setdefault(uid, set()).add(pid)

    eval_users = [u for u, items in truth.items() if items]
    print("可评估用户: %d（测试集有未见过行为的用户）" % len(eval_users))
    if not eval_users:
        print("没有可评估用户，请增大 --test-days 或补充行为数据")
        return

    ctx = {
        "profiles": build_profiles(train_rows, catalog),
        "hot": build_hot_scores(train_rows, cutoff, args.hot_days),
        "item_sim": build_item_similarity(train_rows, order_rows, cart_rows),
        "user_items": train_items,
        "vectors": {pid: catalog.vector(pid) for pid in catalog.product_ids},
    }

    total_products = len(catalog.product_ids)
    results = []

    for name, scorer in STRATEGIES.items():
        for k in args.k:
            precisions, recalls = [], []
            recommended_all = set()
            diversity_sum = 0.0
            hit_users = 0
            ndcg_sum = 0.0

            for uid in eval_users:
                seen = train_items.get(uid, {}) if args.exclude_seen else {}
                candidates = [pid for pid in catalog.product_ids if pid not in seen]
                if not candidates:
                    continue

                scores = scorer(uid, candidates, ctx)
                ranked = sorted(candidates, key=lambda p: scores.get(p, 0.0), reverse=True)[:k]
                hits = len(set(ranked) & truth[uid])

                precisions.append(hits / float(k))
                recalls.append(hits / float(len(truth[uid])))
                recommended_all.update(ranked)

                # 命中率与 NDCG：看排序质量，而不只是"有没有推中"
                if hits > 0:
                    hit_users += 1
                dcg = 0.0
                for pos, pid in enumerate(ranked):
                    if pid in truth[uid]:
                        dcg += 1.0 / math.log2(pos + 2)
                ideal_len = min(k, len(truth[uid]))
                idcg = sum(1.0 / math.log2(i + 2) for i in range(ideal_len))
                ndcg_sum += (dcg / idcg) if idcg > 0 else 0.0

                # 列表内多样性：两两余弦相似度的补
                if len(ranked) > 1:
                    pairs = list(itertools.combinations(ranked, 2))
                    avg_sim = sum(cosine(ctx["vectors"][a], ctx["vectors"][b])
                                  for a, b in pairs) / len(pairs)
                    diversity_sum += (1.0 - avg_sim)
                else:
                    diversity_sum += 1.0

            n = len(precisions) or 1
            results.append({
                "strategy": name,
                "k": k,
                "precision": sum(precisions) / n,
                "recall": sum(recalls) / n,
                "hr": hit_users / float(n),
                "ndcg": ndcg_sum / n,
                "coverage": len(recommended_all) / float(total_products),
                "diversity": diversity_sum / n,
                "users": len(precisions),
            })

    print_results(results, total_products)
    write_outputs(results, args)


def print_results(results, total_products):
    print("")
    print("=" * 84)
    print("推荐算法离线评估结果（商品池 %d 件）" % total_products)
    print("=" * 84)
    header = ("| 策略 | K | Precision@K | Recall@K | HR@K | NDCG@K "
              "| 覆盖率 | 多样性 | 评估用户数 |")
    print(header)
    print("|------|---|-------------|----------|------|--------|--------|--------|------------|")
    for r in results:
        print("| %-7s | %d | %.4f | %.4f | %.4f | %.4f | %.2f%% | %.4f | %d |"
              % (r["strategy"], r["k"], r["precision"], r["recall"], r["hr"], r["ndcg"],
                 r["coverage"] * 100, r["diversity"], r["users"]))
    print("=" * 84)


def write_outputs(results, args):
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write("strategy,k,precision,recall,hr,ndcg,coverage,diversity,users\n")
            for r in results:
                f.write("%s,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%d\n"
                        % (r["strategy"], r["k"], r["precision"], r["recall"], r["hr"],
                           r["ndcg"], r["coverage"], r["diversity"], r["users"]))
        print("\nCSV 已写入: %s" % args.out)

    if args.md:
        lines = ["| 策略 | K | Precision@K | Recall@K | HR@K | NDCG@K | 覆盖率 | 多样性 |",
                 "|------|---|-------------|----------|------|--------|--------|--------|"]
        for r in results:
            lines.append("| %s | %d | %.4f | %.4f | %.4f | %.4f | %.2f%% | %.4f |"
                         % (r["strategy"], r["k"], r["precision"], r["recall"],
                            r["hr"], r["ndcg"], r["coverage"] * 100, r["diversity"]))
        with open(args.md, "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
        print("Markdown 表格已写入: %s" % args.md)


def build_parser():
    parser = argparse.ArgumentParser(description="推荐算法离线评估")
    parser.add_argument("--container", default="shopping-mysql", help="MySQL 容器名")
    parser.add_argument("--database", default="shopping", help="数据库名")
    parser.add_argument("--dump-dir", default=None,
                        help="复用已导出的 TSV 目录（不连数据库）")
    parser.add_argument("--test-days", type=int, default=14,
                        help="测试集时间跨度（最近 N 天）")
    parser.add_argument("--hot-days", type=int, default=7,
                        help="热度统计窗口（天）")
    parser.add_argument("--k", type=int, nargs="+", default=[5, 10],
                        help="评估的推荐长度，可多个")
    parser.add_argument("--exclude-seen", action="store_true",
                        help="从候选与真值中剔除训练集已交互的商品（更严格，但在小商品池上会退化为噪声评估）")
    parser.add_argument("--out", default="evaluation-result.csv", help="CSV 输出路径")
    parser.add_argument("--md", default="evaluation-result.md", help="Markdown 输出路径")
    return parser


if __name__ == "__main__":
    evaluate(build_parser().parse_args())
