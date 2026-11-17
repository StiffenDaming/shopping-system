#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
零号藏馆系统 —— 用户行为模拟数据集生成脚本

用途：
    系统处于开发阶段，真实用户行为数据不足，无法支撑推荐算法的对比实验。
    本脚本按"用户存在潜在品类偏好"的假设生成模拟行为流，注入 user_behavior 表，
    用于第六章的离线评估（内容过滤 / 协同过滤 / 混合加权）。

设计要点：
    1. 每个模拟用户有一组潜在品类偏好（偏斜分布），行为围绕偏好品类产生；
    2. 行为按 浏览 70% / 加购 20% / 下单 10% 分布，符合真实电商漏斗；
    3. 下单以"会话"形式出现（1~3 件同类藏品，共享 scene 前缀），
       这样协同过滤才能从中构建物品共现矩阵；
    4. 时间分布在最近 60 天且向近期倾斜，用于验证时间衰减效果。

说明：
    - 只写入 user_behavior 与 sys_user（sim 前缀），不污染 orders/order_item 等业务表；
    - 重复执行会先清理旧的模拟数据，保证幂等。

用法：
    python3 tools/generate_sim_data.py --users 200 --out /mnt/c/Users/Public/sim.sql
"""

import argparse
import datetime
import json
import random
import urllib.request

API_BASE = "http://127.0.0.1:8080"
USER_ID_BASE = 1000
DEFAULT_PASSWORD_HASH = "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2"


def fetch_products(api_base):
    """从后端读取全部上架商品，避免脚本与数据库表结构耦合。"""
    url = api_base + "/api/products/list?page=1&size=100"
    with urllib.request.urlopen(url, timeout=10) as response:
        payload = json.loads(response.read().decode("utf-8"))
    records = payload["data"]["records"]
    if not records:
        raise RuntimeError("未获取到商品数据，请先启动后端")
    return records


def build_arg_parser():
    parser = argparse.ArgumentParser(description="生成推荐算法实验用的模拟行为数据")
    parser.add_argument("--users", type=int, default=200, help="模拟用户数量")
    parser.add_argument("--days", type=int, default=60, help="行为时间跨度（天）")
    parser.add_argument("--min-events", type=int, default=20, help="每个用户最少行为数")
    parser.add_argument("--max-events", type=int, default=60, help="每个用户最多行为数")
    parser.add_argument("--seed", type=int, default=20261117, help="随机种子，保证实验可复现")
    parser.add_argument("--out", default="/mnt/c/Users/Public/sim.sql", help="输出 SQL 文件路径")
    parser.add_argument("--api", default=API_BASE, help="后端地址")
    return parser


def sql_escape(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def weighted_pick(rng, items, weights):
    total = sum(weights)
    if total <= 0:
        return rng.choice(items)
    point = rng.random() * total
    acc = 0.0
    for item, weight in zip(items, weights):
        acc += weight
        if point <= acc:
            return item
    return items[-1]


def main():
    args = build_arg_parser().parse_args()
    rng = random.Random(args.seed)

    products = fetch_products(args.api)
    by_category = {}
    for product in products:
        by_category.setdefault(product["categoryId"], []).append(product)
    categories = sorted(by_category.keys())

    out = []
    out.append("-- 零号藏馆系统 模拟行为数据集")
    out.append("-- 生成参数: users=%d days=%d events=%d~%d seed=%d"
               % (args.users, args.days, args.min_events, args.max_events, args.seed))
    out.append("-- 幂等: 先清理旧模拟数据")
    out.append("DELETE FROM user_behavior WHERE user_id > %d AND user_id <= %d;"
               % (USER_ID_BASE, USER_ID_BASE + args.users))
    out.append("DELETE FROM user_profile WHERE user_id > %d AND user_id <= %d;"
               % (USER_ID_BASE, USER_ID_BASE + args.users))
    out.append("")

    out.append("-- 1. 模拟用户（密码与 admin 相同，均为 123456，便于演示时登录）")
    for i in range(1, args.users + 1):
        uid = USER_ID_BASE + i
        out.append(
            "INSERT IGNORE INTO sys_user(id, username, password, email, role, status) "
            "VALUES(%d, 'sim%03d', %s, 'sim%03d@demo.com', 'USER', 1);"
            % (uid, i, sql_escape(DEFAULT_PASSWORD_HASH), i)
        )
    out.append("")

    now = datetime.datetime.now()
    rows = []

    for i in range(1, args.users + 1):
        uid = USER_ID_BASE + i
        # 潜在偏好：每个用户只有 1~2 个强兴趣品类，其余品类仅偶发浏览。
        # 现实电商中用户的购买高度集中在少数类目；若偏好过于弥散，
        # "个性化推荐"就会退化成"热门推荐"，无法体现算法差异。
        top_cats = rng.sample(categories, rng.randint(1, 2))
        prefs = {c: (0.95 if c in top_cats else 0.05) for c in categories}
        cat_list = list(prefs.keys())
        cat_weights = [prefs[c] for c in cat_list]

        # 价格敏感度：影响同类内部的选择
        price_averse = rng.random()

        events = rng.randint(args.min_events, args.max_events)
        session_no = 0
        while events > 0:
            events -= 1
            category = weighted_pick(rng, cat_list, cat_weights)
            candidates = by_category[category]

            # 同品类内按价格偏好加权：price_averse 越高越偏向低价
            prices = [float(p["price"]) for p in candidates]
            max_price = max(prices) if prices else 1.0
            local_weights = [
                (1.0 - price_averse) + price_averse * (1.0 - (price / max_price))
                for price in prices
            ]
            product = weighted_pick(rng, candidates, local_weights)

            # 时间向近期倾斜：u^1.6 让近 7 天事件更密集
            days_ago = (rng.random() ** 1.6) * args.days
            moment = now - datetime.timedelta(
                days=days_ago, minutes=rng.randint(0, 1439), seconds=rng.randint(0, 59))
            ts = moment.strftime("%Y-%m-%d %H:%M:%S")

            dice = rng.random()
            if dice < 0.70:
                rows.append((uid, product["id"], "VIEW", "1.00",
                             rng.randint(3000, 150000), None, "LIST", ts))
            elif dice < 0.90:
                rows.append((uid, product["id"], "CART", "3.00", 0, None, "DETAIL", ts))
            else:
                # 一次下单会话：1~3 件同类藏品，共享 scene，用于构建共现矩阵
                session_no += 1
                scene = "SIMORD-%d-%d" % (uid, session_no)
                items = rng.randint(1, 3)
                for _ in range(items):
                    extra = weighted_pick(rng, candidates, local_weights)
                    rows.append((uid, extra["id"], "ORDER", "5.00", 0, None, scene, ts))

    out.append("-- 2. 行为日志（%d 条）" % len(rows))
    chunk = 400
    for start in range(0, len(rows), chunk):
        part = rows[start:start + chunk]
        values = ",\n".join(
            "(%d, %d, '%s', %s, %d, %s, %s, '%s')"
            % (r[0], r[1], r[2], r[3], r[4], sql_escape(r[5]), sql_escape(r[6]), r[7])
            for r in part
        )
        out.append(
            "INSERT INTO user_behavior(user_id, product_id, behavior_type, weight, "
            "stay_ms, keyword, scene, create_time) VALUES\n" + values + ";"
        )

    with open(args.out, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out))

    print("用户: %d" % args.users)
    print("行为: %d" % len(rows))
    print("输出: %s" % args.out)


if __name__ == "__main__":
    main()
