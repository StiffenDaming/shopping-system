#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
论文配图生成脚本
用 Pillow 直接绘制，不依赖 Graphviz / matplotlib。
输出到 docs/figures/ 目录，供 rebuild_thesis.py 插入文档。
"""

import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "docs", "figures")
FONT_PATH = r"C:\Windows\Fonts\msyh.ttc"

# 配色：克制的学术风，蓝灰为主
C_BG = "#FFFFFF"
C_BAND = "#EEF3FA"
C_BOX = "#FFFFFF"
C_BORDER = "#8FA8C8"
C_MAIN = "#2F5597"
C_ACCENT = "#C55A11"
C_TEXT = "#1F2937"
C_SUB = "#5B6B7F"
C_LINE = "#7F8FA6"


def font(size, bold=False):
    # msyh.ttc 内含多字重，index 0=常规；粗体用 simhei 替代
    if bold:
        return ImageFont.truetype(r"C:\Windows\Fonts\msyhbd.ttc", size)
    return ImageFont.truetype(FONT_PATH, size)


def text_center(draw, cx, cy, lines, size=22, fill=C_TEXT, bold=False, spacing=8):
    f = font(size, bold)
    heights = []
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=f)
        heights.append(bbox[3] - bbox[1])
    total = sum(heights) + spacing * (len(lines) - 1)
    y = cy - total / 2
    for line, h in zip(lines, heights):
        bbox = draw.textbbox((0, 0), line, font=f)
        w = bbox[2] - bbox[0]
        draw.text((cx - w / 2, y - bbox[1]), line, font=f, fill=fill)
        y += h + spacing


def box(draw, xy, lines, fill=C_BOX, outline=C_BORDER, size=22, radius=10,
        bold=False, width=2, text_fill=C_TEXT):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)
    cx = (xy[0] + xy[2]) / 2
    cy = (xy[1] + xy[3]) / 2
    text_center(draw, cx, cy, lines, size=size, fill=text_fill, bold=bold)


def arrow(draw, start, end, color=C_LINE, width=3, head=12, dashed=False):
    x1, y1 = start
    x2, y2 = end
    if dashed:
        # 手绘虚线
        total = ((x2 - x1) ** 2 + (y2 - y1) ** 2) ** 0.5
        if total == 0:
            return
        seg = 10
        n = int(total // (seg * 2))
        for i in range(n + 1):
            t0 = (i * seg * 2) / total
            t1 = min((i * seg * 2 + seg) / total, 1.0)
            draw.line([(x1 + (x2 - x1) * t0, y1 + (y2 - y1) * t0),
                       (x1 + (x2 - x1) * t1, y1 + (y2 - y1) * t1)],
                      fill=color, width=width)
    else:
        draw.line([start, end], fill=color, width=width)
    # 箭头
    import math
    ang = math.atan2(y2 - y1, x2 - x1)
    for sign in (1, -1):
        a = ang + sign * 0.42
        draw.line([(x2, y2), (x2 - head * math.cos(a), y2 - head * math.sin(a))],
                  fill=color, width=width)


def save(img, name):
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, name)
    img.save(path, "PNG")
    print("saved", path)
    return path


# ---------------------------------------------------------------------------
# 图 1：系统总体架构
# ---------------------------------------------------------------------------
def fig_architecture():
    W, H = 1500, 900
    img = Image.new("RGB", (W, H), C_BG)
    d = ImageDraw.Draw(img)

    # 容器边界：包住 Nginx、后端与 MySQL
    d.rounded_rectangle((60, 175, 1180, 845), radius=14, outline=C_ACCENT, width=2)
    d.text((74, 186), "Docker Compose 编排", font=font(16), fill=C_ACCENT)

    # 浏览器
    box(d, (420, 40, 900, 130), ["浏览器客户端", "Vue3 + Element Plus + Vite"],
        fill=C_BAND, size=24, bold=True)
    arrow(d, (660, 130), (660, 200))

    # Nginx
    box(d, (330, 200, 990, 290), ["Nginx（前端容器 :80）", "静态资源托管 + /api 反向代理"],
        fill=C_BOX, size=22)

    # 后端容器
    d.rounded_rectangle((100, 330, 780, 690), radius=12, outline=C_BORDER, width=2)
    d.text((116, 340), "Spring Boot 应用（后端容器 :8080）", font=font(17, True), fill=C_MAIN)

    box(d, (120, 380, 480, 520), ["业务模块", "认证 / 商品 / 购物车", "订单 / 地址 / 统计"],
        size=20)
    box(d, (500, 380, 760, 520), ["推荐模块", "行为埋点 / 用户画像", "热度榜 / 混合推荐"],
        size=20, fill="#FBEFE6", outline=C_ACCENT)

    box(d, (120, 560, 760, 650), ["MyBatis-Plus 数据访问层", "统一分页 / 逻辑删除 / 自动填充"],
        size=20, fill=C_BAND)

    arrow(d, (660, 290), (660, 330))
    arrow(d, (300, 520), (300, 558))
    arrow(d, (630, 520), (630, 558))

    # 数据库
    box(d, (330, 730, 990, 820), ["MySQL 8.0（3307）", "业务表 7 张 + 推荐表 4 张"],
        fill=C_BAND, size=22)
    arrow(d, (440, 650), (440, 728))

    # 大模型（容器外）
    box(d, (1210, 380, 1450, 520), ["DeepSeek 大模型 API", "离线商品标签抽取"],
        size=18, fill="#F3F7EE", outline="#7E9E5B")
    arrow(d, (760, 450), (1208, 450), color="#7E9E5B", dashed=True)
    d.text((830, 415), "离线调用", font=font(15), fill="#5C7A3E")

    d.text((60, 850), "图：系统总体架构", font=font(16), fill=C_SUB)
    return save(img, "fig-4-1-architecture.png")


# ---------------------------------------------------------------------------
# 图 2：推荐系统五层架构
# ---------------------------------------------------------------------------
def fig_pipeline():
    W, H = 1500, 700
    img = Image.new("RGB", (W, H), C_BG)
    d = ImageDraw.Draw(img)

    layers = [
        ("数据层", ["用户行为日志", "商品标签", "订单明细"], "#EEF3FA"),
        ("画像层", ["时间衰减聚合", "标签偏好向量", "活跃度建模"], "#E7EEF8"),
        ("召回层", ["热度召回", "内容召回", "协同召回"], "#DDE7F4"),
        ("排序层", ["归一化", "加权融合", "冷启动兜底"], "#D3E0F1"),
        ("展示层", ["热藏榜", "猜你喜欢", "为你推荐"], "#C9D9ED"),
    ]
    x0, y0, bw, bh, gap = 90, 130, 220, 300, 45
    centers = []
    for i, (title, items, color) in enumerate(layers):
        x = x0 + i * (bw + gap)
        box(d, (x, y0, x + bw, y0 + 70), [title], fill=C_MAIN, outline=C_MAIN,
            size=24, bold=True, text_fill="#FFFFFF")
        box(d, (x, y0 + 70, x + bw, y0 + bh), items, fill=color, size=19)
        centers.append(x + bw / 2)
        if i > 0:
            prev_right = x0 + (i - 1) * (bw + gap) + bw
            arrow(d, (prev_right + 4, y0 + bh / 2), (x - 6, y0 + bh / 2))

    # 行为回流
    y_back = y0 + bh + 90
    d.line([(centers[-1], y0 + bh), (centers[-1], y_back)], fill=C_ACCENT, width=3)
    d.line([(centers[-1], y_back), (centers[0], y_back)], fill=C_ACCENT, width=3)
    arrow(d, (centers[0], y_back), (centers[0], y0 + bh + 6), color=C_ACCENT)
    d.text((centers[0] + 250, y_back + 12), "展示产生的行为回流为新的数据（闭环）",
           font=font(17), fill=C_ACCENT)

    # 离线 / 在线标注
    d.text((90, 80), "离线（定时任务）", font=font(18, True), fill=C_SUB)
    d.text((1010, 80), "在线（毫秒级响应）", font=font(18, True), fill=C_SUB)
    d.line([(862, 100), (862, y0 + bh - 24)], fill=C_LINE, width=2)
    d.text((60, 640), "图：推荐系统分层架构", font=font(16), fill=C_SUB)
    return save(img, "fig-4-2-rec-pipeline.png")


# ---------------------------------------------------------------------------
# 图 3：用户画像构建流程
# ---------------------------------------------------------------------------
def fig_profile():
    W, H = 1500, 560
    img = Image.new("RGB", (W, H), C_BG)
    d = ImageDraw.Draw(img)

    steps = [
        ("① 行为采集", ["浏览 / 加购 / 下单", "记录商品、时间、停留"]),
        ("② 时间衰减", ["w = 基础权重 × e^(-λt)", "λ=0.05，约 14 天半衰"]),
        ("③ 标签聚合", ["行为映射到商品标签", "按标签累加权重"]),
        ("④ 归一化", ["同用户内除以最大值", "压缩到 [0,1]"]),
        ("⑤ 画像落库", ["标签向量 + 分类向量", "价格偏好 + 活跃度"]),
    ]
    bw, bh, gap = 250, 190, 50
    x0, y0 = 60, 170
    for i, (title, items) in enumerate(steps):
        x = x0 + i * (bw + gap)
        box(d, (x, y0, x + bw, y0 + 50), [title], fill=C_MAIN, outline=C_MAIN,
            size=21, bold=True, text_fill="#FFFFFF")
        box(d, (x, y0 + 50, x + bw, y0 + bh), items, fill=C_BAND, size=17)
        if i > 0:
            arrow(d, (x - gap + 6, y0 + bh / 2), (x - 8, y0 + bh / 2))

    d.text((60, 100), "输入：user_behavior 表", font=font(18), fill=C_SUB)
    d.text((1150, 100), "输出：user_profile 表", font=font(18), fill=C_SUB)
    d.text((60, 470), "图：用户画像构建流程", font=font(16), fill=C_SUB)
    return save(img, "fig-4-3-profile-flow.png")


# ---------------------------------------------------------------------------
# 图 4：物品共现矩阵
# ---------------------------------------------------------------------------
def fig_cooccur():
    W, H = 1200, 700
    img = Image.new("RGB", (W, H), C_BG)
    d = ImageDraw.Draw(img)

    rows = ["订单A", "订单B", "订单C", "购物车D"]
    cols = ["藏品1", "藏品2", "藏品3", "藏品4", "藏品5"]
    data = [
        [1, 1, 0, 1, 0],
        [0, 1, 1, 1, 0],
        [0, 0, 1, 0, 1],
        [1, 0, 0, 1, 1],
    ]
    x0, y0, cw, ch = 240, 235, 150, 70
    # 表头
    for j, c in enumerate(cols):
        box(d, (x0 + j * cw, y0 - ch, x0 + (j + 1) * cw, y0), [c], fill=C_BAND, size=18)
    for i, r in enumerate(rows):
        box(d, (x0 - 130, y0 + i * ch, x0, y0 + (i + 1) * ch), [r], fill=C_BAND, size=18)
        for j in range(len(cols)):
            val = data[i][j]
            fill = "#FFFFFF" if val == 0 else "#D6E4F5"
            draw_box = (x0 + j * cw, y0 + i * ch, x0 + (j + 1) * cw, y0 + (i + 1) * ch)
            d.rectangle(draw_box, fill=fill, outline=C_BORDER, width=1)
            f = font(22, True)
            txt = str(val)
            bb = d.textbbox((0, 0), txt, font=f)
            d.text((draw_box[0] + (cw - (bb[2] - bb[0])) / 2,
                    draw_box[1] + (ch - (bb[3] - bb[1])) / 2 - bb[1]), txt, font=f, fill=C_TEXT)

    d.text((60, 90), "用户-物品交互矩阵（1 表示同一订单/购物车内出现过）", font=font(19), fill=C_TEXT)
    d.text((60, 570), "相似度：  sim(i, j) = co(i, j) / √(deg(i) × deg(j))",
           font=font(21, True), fill=C_MAIN)
    d.text((60, 635), "图：物品共现矩阵与相似度计算", font=font(16), fill=C_SUB)
    return save(img, "fig-4-4-cooccur.png")


# ---------------------------------------------------------------------------
# 图 5：混合加权示意
# ---------------------------------------------------------------------------
def fig_hybrid():
    W, H = 1400, 640
    img = Image.new("RGB", (W, H), C_BG)
    d = ImageDraw.Draw(img)

    sources = [
        ("内容相似度 sim_content", "权重 α = 0.6", "#DDE7F4"),
        ("商品热度 hot_norm", "权重 β = 0.3", "#FBEFE6"),
        ("协同过滤 sim_cf", "权重 γ = 0.1", "#F3F7EE"),
    ]
    y = 130
    for title, w, color in sources:
        box(d, (80, y, 480, y + 80), [title, w], fill=color, size=18)
        arrow(d, (490, y + 40), (700, y + 40))
        y += 130

    box(d, (700, 210, 980, 350), ["加权融合", "score = α·内容 + β·热度 + γ·协同"],
        fill=C_MAIN, outline=C_MAIN, size=18, bold=True, text_fill="#FFFFFF")

    arrow(d, (990, 280), (1100, 280))
    box(d, (1100, 190, 1330, 370), ["Top-N 排序", "剔除已购", "生成推荐理由"],
        fill=C_BAND, size=18)

    d.text((80, 80), "三路召回各自归一化到 [0,1] 后再融合", font=font(19), fill=C_TEXT)
    d.text((80, 540), "冷启动：α=0, γ=0, β=1（退化为纯热度）", font=font(18), fill=C_ACCENT)
    d.text((80, 590), "图：混合推荐加权融合", font=font(16), fill=C_SUB)
    return save(img, "fig-4-5-hybrid.png")


# ---------------------------------------------------------------------------
# 图 6：行为采集流程
# ---------------------------------------------------------------------------
def fig_track():
    W, H = 1500, 600
    img = Image.new("RGB", (W, H), C_BG)
    d = ImageDraw.Draw(img)

    steps = [
        ("页面事件", ["浏览详情", "加入购物车", "搜索关键词"]),
        ("埋点上报", ["POST /api/behavior/track", "携带 JWT，游客可匿名"]),
        ("异步写入", ["独立线程池", "不阻塞主请求"]),
        ("定时聚合", ["整点算热度", "每小时算画像"]),
        ("推荐接口", ["读预计算结果", "毫秒级响应"]),
    ]
    bw, bh, gap = 250, 180, 55
    x0, y0 = 60, 180
    for i, (title, items) in enumerate(steps):
        x = x0 + i * (bw + gap)
        box(d, (x, y0, x + bw, y0 + 50), [title], fill=C_MAIN, outline=C_MAIN,
            size=21, bold=True, text_fill="#FFFFFF")
        box(d, (x, y0 + 50, x + bw, y0 + bh), items, fill=C_BAND, size=17)
        if i > 0:
            arrow(d, (x - gap + 6, y0 + bh / 2), (x - 8, y0 + bh / 2))

    box(d, (60, 420, 830, 500), ["关键设计：埋点与业务解耦，写失败不影响下单/加购"],
        fill="#FBEFE6", outline=C_ACCENT, size=18)
    d.text((60, 110), "一次用户点击到推荐结果更新的完整链路", font=font(19), fill=C_TEXT)
    d.text((60, 550), "图：用户行为采集与推荐更新流程", font=font(16), fill=C_SUB)
    return save(img, "fig-5-1-track-flow.png")


# ---------------------------------------------------------------------------
# 图 7：评估结果柱状图
# ---------------------------------------------------------------------------
def fig_eval():
    W, H = 1300, 700
    img = Image.new("RGB", (W, H), C_BG)
    d = ImageDraw.Draw(img)

    strategies = ["POP", "CF", "CONTENT", "HYBRID"]
    p5 = [0.4287, 0.6683, 0.7208, 0.7307]
    ndcg5 = [0.4664, 0.7389, 0.8055, 0.8129]

    ox, oy = 140, 580       # 原点
    plot_w, plot_h = 900, 420
    d.line([(ox, oy), (ox + plot_w + 40, oy)], fill=C_LINE, width=2)
    d.line([(ox, oy), (ox, oy - plot_h - 30)], fill=C_LINE, width=2)

    # y 轴刻度
    for i in range(6):
        val = i * 0.2
        y = oy - val * (plot_h / 1.0)
        d.line([(ox - 8, y), (ox, y)], fill=C_LINE, width=2)
        d.text((ox - 60, y - 12), f"{val:.1f}", font=font(16), fill=C_SUB)
        if 0 < i:
            d.line([(ox, y), (ox + plot_w, y)], fill="#E5E7EB", width=1)

    group_w = plot_w / len(strategies)
    bar_w = 55
    metric_fill = ["#4A6FA5", "#9AB6D6"]
    metric_line = ["#2F5597", "#6D93BD"]

    for i, name in enumerate(strategies):
        cx = ox + group_w * (i + 0.5)
        for k, (vals, offset) in enumerate([(p5, -bar_w / 1.6), (ndcg5, bar_w / 1.6)]):
            v = vals[i]
            h = v * plot_h
            x1 = cx + offset - bar_w / 2
            x2 = cx + offset + bar_w / 2
            d.rectangle((x1, oy - h, x2, oy), fill=metric_fill[k], outline=metric_line[k], width=2)
            f = font(16, True)
            txt = f"{v:.4f}"
            bb = d.textbbox((0, 0), txt, font=f)
            d.text((x1 + (bar_w - (bb[2] - bb[0])) / 2, oy - h - 26), txt, font=f, fill=C_TEXT)
        f = font(20, True)
        bb = d.textbbox((0, 0), name, font=f)
        d.text((cx - (bb[2] - bb[0]) / 2, oy + 14), name, font=f, fill=C_TEXT)

    # 图例
    d.rectangle((ox + plot_w - 230, 110, ox + plot_w - 200, 130),
                fill=metric_fill[0], outline=metric_line[0])
    d.text((ox + plot_w - 192, 108), "Precision@5", font=font(16), fill=C_TEXT)
    d.rectangle((ox + plot_w - 230, 140, ox + plot_w - 200, 160),
                fill=metric_fill[1], outline=metric_line[1])
    d.text((ox + plot_w - 192, 138), "NDCG@5", font=font(16), fill=C_TEXT)

    d.text((60, 60), "四种策略在测试集上的表现（K = 5）", font=font(20, True), fill=C_TEXT)
    d.text((60, 650), "图：推荐算法效果对比", font=font(16), fill=C_SUB)
    return save(img, "fig-6-1-eval.png")


if __name__ == "__main__":
    fig_architecture()
    fig_pipeline()
    fig_profile()
    fig_cooccur()
    fig_hybrid()
    fig_track()
    fig_eval()
    print("全部图片生成完成 ->", os.path.abspath(OUT_DIR))
