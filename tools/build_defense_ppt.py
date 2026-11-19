# -*- coding: utf-8 -*-
"""
生成答辩 PPT（16:9，16 页）。

数据来源：
  - 测试数量：backend/target/surefire-reports（129 = 单元 89 + 接口 40）
  - 推荐评估：docs/实验数据-推荐算法评估.md（K=5 主实验）
  - 数据规模：19 商品 / 7 分类 / 93 标签 / 9241 条行为 / 200 模拟用户
  - 表结构：schema-mysql.sql（13 张表）

运行：
  python tools/build_defense_ppt.py
"""

import os

from pptx import Presentation
from pptx.chart.data import CategoryChartData
from pptx.dml.color import RGBColor
from pptx.enum.chart import XL_CHART_TYPE, XL_LEGEND_POSITION
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.oxml.ns import qn
from pptx.util import Inches, Pt
from lxml import etree

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(BASE, "答辩PPT-基于Spring Boot的收藏品商城系统.pptx")

# ============ 配色 ============
INK = "0F2B46"
INK2 = "1B3A5C"
GOLD = "C8A15A"
BLUE = "2E6F9E"
TEXT = "23303F"
MUTED = "6B7A8F"
SOFT = "F5F7FA"
WHITE = "FFFFFF"
LINE = "DCE3EA"

FONT = "Microsoft YaHei"

W, H = 13.333, 7.5


def rgb(hex_str):
    return RGBColor.from_string(hex_str)


def style_run(run, size=18, bold=False, color=TEXT, font=FONT):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = rgb(color)
    run.font.name = font
    rpr = run._r.get_or_add_rPr()
    ea = rpr.find(qn("a:ea"))
    if ea is None:
        ea = etree.SubElement(rpr, qn("a:ea"))
    ea.set("typeface", font)


def textbox(slide, x, y, w, h, text, size=18, bold=False, color=TEXT,
            align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, spacing=1.15):
    box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = 0
    tf.margin_right = 0
    tf.margin_top = 0
    tf.margin_bottom = 0
    lines = text.split("\n")
    for i, line in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = spacing
        run = p.add_run()
        run.text = line
        style_run(run, size, bold, color)
    return box


def rect(slide, x, y, w, h, fill=WHITE, line_color=None, line_w=1.0,
         shape=MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.06):
    shp = slide.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    if fill is None:
        shp.fill.background()
    else:
        shp.fill.solid()
        shp.fill.fore_color.rgb = rgb(fill)
    if line_color:
        shp.line.color.rgb = rgb(line_color)
        shp.line.width = Pt(line_w)
    else:
        shp.line.fill.background()
    shp.shadow.inherit = False
    if shape == MSO_SHAPE.ROUNDED_RECTANGLE:
        try:
            shp.adjustments[0] = radius
        except Exception:
            pass
    return shp


def shape_text(shp, text, size=14, bold=False, color=TEXT,
               align=PP_ALIGN.CENTER, spacing=1.1):
    tf = shp.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf.margin_left = Inches(0.08)
    tf.margin_right = Inches(0.08)
    tf.margin_top = Inches(0.04)
    tf.margin_bottom = Inches(0.04)
    for i, line in enumerate(text.split("\n")):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = spacing
        run = p.add_run()
        run.text = line
        style_run(run, size, bold, color)


def new_slide(prs, bg=WHITE):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    bgshape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0,
                                     Inches(W), Inches(H))
    bgshape.fill.solid()
    bgshape.fill.fore_color.rgb = rgb(bg)
    bgshape.line.fill.background()
    bgshape.shadow.inherit = False
    return slide


def header(slide, part, title, subtitle=None):
    textbox(slide, 0.85, 0.42, 9, 0.3, part, 12, True, GOLD)
    textbox(slide, 0.85, 0.72, 11.6, 0.7, title, 27, True, INK)
    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.85), Inches(1.44),
                                 Inches(0.9), Inches(0.05))
    bar.fill.solid()
    bar.fill.fore_color.rgb = rgb(GOLD)
    bar.line.fill.background()
    bar.shadow.inherit = False
    if subtitle:
        textbox(slide, 0.85, 1.62, 11.6, 0.4, subtitle, 13, False, MUTED)


def footer(slide, page, total=16):
    textbox(slide, 0.85, 6.95, 8, 0.3,
            "基于 Spring Boot 的收藏品商城系统 · 毕业设计答辩", 9, False, MUTED)
    textbox(slide, 11.4, 6.95, 1.1, 0.3, f"{page:02d} / {total}", 9, False, MUTED,
            PP_ALIGN.RIGHT)


def bullet_block(slide, x, y, w, items, size=14, gap=0.42, color=TEXT,
                 marker_color=GOLD, line_spacing=1.25):
    """items: [(粗体前缀, 说明)] 或 [纯文本]"""
    cy = y
    for item in items:
        dot = slide.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(cy + 0.08),
                                     Inches(0.07), Inches(0.07))
        dot.fill.solid()
        dot.fill.fore_color.rgb = rgb(marker_color)
        dot.line.fill.background()
        dot.shadow.inherit = False
        if isinstance(item, tuple):
            head, desc = item
            box = slide.shapes.add_textbox(Inches(x + 0.2), Inches(cy - 0.02),
                                           Inches(w - 0.2), Inches(0.4))
            tf = box.text_frame
            tf.word_wrap = True
            tf.margin_left = 0
            tf.margin_top = 0
            tf.margin_bottom = 0
            p = tf.paragraphs[0]
            p.line_spacing = line_spacing
            r1 = p.add_run()
            r1.text = head
            style_run(r1, size, True, INK)
            r2 = p.add_run()
            r2.text = "  " + desc
            style_run(r2, size, False, color)
        else:
            textbox(slide, x + 0.2, cy - 0.02, w - 0.2, 0.4, item, size, False, color)
        cy += gap


def card(slide, x, y, w, h, title, lines, accent=BLUE, title_size=15,
         body_size=12, fill=SOFT):
    rect(slide, x, y, w, h, fill=fill, line_color=LINE)
    tag = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(y),
                                 Inches(0.06), Inches(h))
    tag.fill.solid()
    tag.fill.fore_color.rgb = rgb(accent)
    tag.line.fill.background()
    tag.shadow.inherit = False
    textbox(slide, x + 0.25, y + 0.18, w - 0.45, 0.32, title, title_size, True, INK)
    textbox(slide, x + 0.25, y + 0.62, w - 0.45, h - 0.75, "\n".join(lines),
            body_size, False, MUTED, spacing=1.3)


def chart_font(chart, size=11):
    chart.font.name = FONT
    chart.font.size = Pt(size)
    chart.font.color.rgb = rgb(TEXT)


# ==================================================================
prs = Presentation()
prs.slide_width = Inches(W)
prs.slide_height = Inches(H)

# ---------- 01 封面 ----------
s = new_slide(prs, INK)
# 装饰线
for yy, ww in ((2.0, 1.6), (5.22, 5.6)):
    ln = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.9), Inches(yy),
                            Inches(ww), Inches(0.02))
    ln.fill.solid()
    ln.fill.fore_color.rgb = rgb(GOLD)
    ln.line.fill.background()
    ln.shadow.inherit = False

textbox(s, 0.9, 1.35, 6, 0.35, "GRADUATION  DEFENSE", 13, True, GOLD)
textbox(s, 0.9, 2.15, 11.5, 1.1, "基于 Spring Boot 的收藏品商城系统", 42, True, WHITE)
textbox(s, 0.9, 3.32, 11.5, 0.5, "设 计 与 实 现", 22, True, GOLD)
textbox(s, 0.9, 4.05, 11.5, 0.9,
        "交易闭环 + 用户画像 + 混合推荐：一套可运行、可测试、可部署的垂直电商系统",
        14, False, "C7D3E0", spacing=1.5)

info = [("答辩人", "＿＿＿＿＿"), ("学号", "＿＿＿＿＿"),
        ("指导教师", "＿＿＿＿＿"), ("答辩时间", "2026 年 11 月")]
x = 0.9
for k, v in info:
    textbox(s, x, 5.45, 1.2, 0.3, k, 10, False, "8CA0B8")
    textbox(s, x, 5.72, 2.2, 0.3, v, 13, True, WHITE)
    x += 2.55

stack = "Spring Boot 2.7  ·  MyBatis-Plus  ·  MySQL 8  ·  Vue 3  ·  Element Plus  ·  Docker"
textbox(s, 0.9, 6.55, 11.5, 0.35, stack, 11, False, "7C90A8")

# ---------- 02 目录 ----------
s = new_slide(prs)
textbox(s, 0.85, 0.6, 6, 0.4, "CONTENTS", 13, True, GOLD)
textbox(s, 0.85, 0.95, 8, 0.7, "汇报内容", 30, True, INK)

toc = [
    ("01", "选题背景与需求", "行业痛点 · 功能与非功能需求"),
    ("02", "系统设计与实现", "架构 · 数据库 · 交易闭环 · 工程难点"),
    ("03", "个性化推荐模块", "用户画像 · 混合推荐 · 大模型标签"),
    ("04", "测试与效果评估", "138 个自动化测试 · 四策略对比实验"),
]
y = 2.15
for num, title, desc in toc:
    rect(s, 0.85, y, 11.6, 0.92, fill=SOFT, line_color=LINE)
    textbox(s, 1.1, y + 0.2, 0.9, 0.5, num, 22, True, GOLD)
    textbox(s, 2.05, y + 0.16, 4.6, 0.4, title, 17, True, INK)
    textbox(s, 2.05, y + 0.53, 8.5, 0.3, desc, 11, False, MUTED)
    y += 1.12

textbox(s, 0.85, 6.55, 11.6, 0.35,
        "汇报约 12 分钟：设计 6 分钟 · 推荐 4 分钟 · 演示 2 分钟", 11, False, MUTED)

# ---------- 03 背景 ----------
s = new_slide(prs)
header(s, "PART 01 · 选题背景", "为什么做这样一个系统",
       "从行业、平台、技术三个层面看，收藏品线上交易缺的不是「电商」，而是「垂直且轻量」的电商")

cards = [
    ("行业层面", ["线上收藏品交易持续增长，", "但传统线下交易受时空限制：", "品类有限、运营成本高"],
     BLUE),
    ("平台层面", ["头部电商入驻费用高、规则多、", "数据不自主；", "商家难以做个性化运营"], GOLD),
    ("技术层面", ["开源电商系统代码复杂、", "二次开发难度大，", "部分项目测试薄弱、质量难保证"], INK2),
]
x = 0.85
for title, lines, accent in cards:
    card(s, x, 2.15, 3.7, 2.05, title, lines, accent)
    x += 3.95

rect(s, 0.85, 4.55, 11.6, 0.72, fill=INK, line_color=None)
shape_text(s.shapes[-1],
           "由此确定目标：做一套轻量、可扩展、可测试，并具备推荐能力的收藏品垂直商城",
           14, True, WHITE)

bullet_block(s, 0.85, 5.55, 11.4, [
    ("可落地", "完整交易闭环（浏览 → 下单 → 发货 → 收货），而非功能堆砌"),
    ("有深度", "在交易之上增加用户画像与混合推荐，体现算法设计能力"),
    ("可验证", "全量自动化测试 + 离线对比实验，效果有数据支撑"),
], size=13, gap=0.44)

# ---------- 04 需求分析 ----------
s = new_slide(prs)
header(s, "PART 01 · 需求分析", "系统要做什么",
       "按角色拆分功能需求，再补充非功能约束")

rect(s, 0.85, 1.95, 5.6, 3.55, fill=SOFT, line_color=LINE)
textbox(s, 1.1, 2.12, 4, 0.35, "普通用户端", 16, True, BLUE)
bullet_block(s, 1.1, 2.6, 5.1, [
    "注册（图形验证码）/ 登录 / 退出",
    "商品浏览、搜索、分类筛选、排序、分页",
    "购物车增删改、结算下单",
    "收货地址管理、默认地址",
    "订单查询、取消待发货、确认收货",
    "个人中心：查看信息、修改密码",
    "个性化推荐：热藏榜 / 猜你喜欢 / 为你推荐",
], size=12, gap=0.36)

rect(s, 6.85, 1.95, 5.6, 3.55, fill=SOFT, line_color=LINE)
textbox(s, 7.1, 2.12, 4, 0.35, "管理员端", 16, True, GOLD)
bullet_block(s, 7.1, 2.6, 5.1, [
    "商品管理：增删改、上下架",
    "订单管理：发货、强制完成、状态筛选",
    "用户管理：禁用 / 启用、重置密码",
    "数据看板：用户、商品、订单、销售统计",
    "用户画像查看（标签与分类偏好）",
    "轮播图管理：增删改、排序、上下架",
], size=12, gap=0.36)

nf = [("安全性", "JWT 鉴权 + BCrypt 加密"),
      ("性能", "推荐预计算，响应 < 200ms"),
      ("可维护", "分层清晰、注释完善"),
      ("可部署", "Docker 一键启动")]
x = 0.85
for k, v in nf:
    rect(s, x, 5.7, 2.78, 0.95, fill=WHITE, line_color=LINE)
    textbox(s, x + 0.18, 5.83, 2.4, 0.3, k, 12, True, INK)
    textbox(s, x + 0.18, 6.15, 2.45, 0.42, v, 9.5, False, MUTED, spacing=1.2)
    x += 2.94

# ---------- 05 架构 ----------
s = new_slide(prs)
header(s, "PART 02 · 总体设计", "技术选型与分层架构",
       "前后端分离 + 容器化部署：职责清晰，便于独立演进")

layers = [
    ("表现层", "Vue 3 + Element Plus + Vite 5", "Pinia 状态管理 · Axios 拦截器 · 路由守卫", BLUE),
    ("接入层", "Nginx", "SPA 静态资源 · /api 反向代理 · 延迟解析避免启动即崩", INK2),
    ("业务层", "Spring Boot 2.7 + MyBatis-Plus", "Controller / Service / Mapper · JWT 拦截器 · 推荐模块 · 定时任务", GOLD),
    ("数据层", "MySQL 8", "12 张业务与推荐表 · 对象存储 OSS 存图片", "5A6B7C"),
]
y = 1.95
for name, main, sub, accent in layers:
    rect(s, 0.85, y, 8.0, 1.0, fill=SOFT, line_color=LINE)
    tag = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.85), Inches(y),
                             Inches(1.25), Inches(1.0))
    tag.fill.solid()
    tag.fill.fore_color.rgb = rgb(accent)
    tag.line.fill.background()
    tag.shadow.inherit = False
    shape_text(tag, name, 13, True, WHITE)
    textbox(s, 2.35, y + 0.2, 6.3, 0.3, main, 14, True, INK)
    textbox(s, 2.35, y + 0.57, 6.3, 0.3, sub, 10.5, False, MUTED)
    y += 1.14

rect(s, 9.1, 1.95, 3.35, 4.42, fill=WHITE, line_color=LINE)
textbox(s, 9.35, 2.12, 2.9, 0.3, "选型理由", 13, True, GOLD)
bullet_block(s, 9.35, 2.55, 2.85, [
    "生态成熟，文档与社区完备",
    "MyBatis-Plus 减少样板 SQL",
    "Vue 3 组合式 API 便于复用",
    "Docker 保证环境一致",
    "全部开源，零授权成本",
], size=10.5, gap=0.62)

# ---------- 06 数据库 ----------
s = new_slide(prs)
header(s, "PART 02 · 数据库设计", "13 张表，分三层组织",
       "交易数据保证一致性，推荐数据独立存放，互不干扰")

groups = [
    ("交易核心（7 张）", "sys_user · category · product · cart_item · orders · order_item · delivery_address", BLUE, 1.92),
    ("推荐模块（4 张）", "user_behavior · product_tag · user_profile · product_hot", GOLD, 3.02),
    ("运营与辅助（2 张）", "banner（首页轮播位）· chat_unanswered（客服未命中记录）", INK2, 4.12),
]
for title, body, accent, gy in groups:
    rect(s, 0.85, gy, 11.6, 0.95, fill=SOFT, line_color=LINE)
    t = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.85), Inches(gy),
                           Inches(0.06), Inches(0.95))
    t.fill.solid()
    t.fill.fore_color.rgb = rgb(accent)
    t.line.fill.background()
    t.shadow.inherit = False
    textbox(s, 1.1, gy + 0.14, 3.0, 0.3, title, 13, True, INK)
    textbox(s, 1.1, gy + 0.5, 11.0, 0.4, body, 10.5, False, MUTED)

textbox(s, 0.85, 5.42, 11.6, 0.3, "几个关键设计", 14, True, INK)
bullet_block(s, 0.85, 5.82, 5.5, [
    ("订单快照", "order_item 存商品名与成交价，改价不影响历史订单"),
    ("逻辑删除", "product 用 deleted 标记，避免外键悬空"),
], size=11.5, gap=0.55)
bullet_block(s, 6.75, 5.82, 5.5, [
    ("推荐解耦", "行为与画像独立建表，可按需清空重算"),
    ("索引", "行为表按 user_id + create_time 支撑时间窗口查询"),
], size=11.5, gap=0.55)

# ---------- 07 交易闭环 ----------
s = new_slide(prs)
header(s, "PART 02 · 核心业务", "交易闭环与状态机",
       "订单只允许合法流转，库存与订单始终保持一致")

steps = ["注册登录", "浏览搜索", "加入购物车", "结算下单", "待发货", "已发货", "已完成"]
x = 0.85
for i, st in enumerate(steps):
    accent = GOLD if st in ("待发货", "已发货", "已完成") else BLUE
    box = rect(s, x, 2.05, 1.45, 0.62, fill=WHITE, line_color=accent)
    shape_text(box, st, 10.5, True, INK)
    if i < len(steps) - 1:
        textbox(s, x + 1.50, 2.19, 0.2, 0.3, "›", 15, True, MUTED, PP_ALIGN.CENTER)
    x += 1.72

# 取消分支
box = rect(s, 2.55, 2.95, 3.2, 0.52, fill="FDECEC", line_color="E4A6A6")
shape_text(box, "取消订单（仅待发货）→ 原子恢复库存", 10, True, "A33A3A")

textbox(s, 0.85, 3.78, 11.6, 0.3, "三个容易出问题的地方，都做了针对性处理", 13, True, INK)

probs = [
    ("① 超卖", "下单不是「先查再减」，而是\nUPDATE ... WHERE stock >= ?\n影响行数为 0 即判定库存不足"),
    ("② 取消回补", "同样用单条原子 UPDATE 累加，\n避免并发取消时的丢失更新"),
    ("③ 越权", "用户只能操作自己的订单；\n管理员接口独立鉴权"),
]
x = 0.85
for title, body in probs:
    rect(s, x, 4.2, 3.75, 1.55, fill=SOFT, line_color=LINE)
    textbox(s, x + 0.22, 4.34, 3.3, 0.3, title, 13, True, GOLD)
    textbox(s, x + 0.22, 4.72, 3.35, 0.9, body, 10.5, False, MUTED, spacing=1.35)
    x += 3.95

textbox(s, 0.85, 6.05, 11.6, 0.55,
        "实测：库存 18 → 下单 2 件变 16 → 取消后回到 18；并发扣减不会出现负库存。",
        11, False, MUTED)

# ---------- 08 推荐总览 ----------
s = new_slide(prs)
header(s, "PART 03 · 推荐模块", "推荐系统整体链路",
       "离线算画像与标签，在线只做读取与加权，保证接口响应速度")

flow = [("行为埋点", "浏览 / 加购 / 下单\n游客可匿名上报", BLUE),
        ("用户画像", "时间衰减聚合\n标签 + 分类偏好", INK2),
        ("多路召回", "内容 / 热度 / 协同\n三路并行", GOLD),
        ("融合排序", "加权求和\n冷启动兜底", "5A6B7C")]
x = 0.85
for i, (title, sub, accent) in enumerate(flow):
    rect(s, x, 2.0, 2.6, 1.5, fill=WHITE, line_color=accent)
    textbox(s, x + 0.2, 2.15, 2.2, 0.32, title, 15, True, INK)
    textbox(s, x + 0.2, 2.62, 2.25, 0.75, sub, 10.5, False, MUTED, spacing=1.3)
    if i < 3:
        textbox(s, x + 2.66, 2.5, 0.35, 0.4, "→", 20, True, GOLD)
    x += 3.05

textbox(s, 0.85, 3.85, 11.6, 0.3, "两条离线链路（不占用请求时间）", 13, True, INK)
rect(s, 0.85, 4.25, 5.6, 1.35, fill=SOFT, line_color=LINE)
textbox(s, 1.1, 4.4, 5.1, 0.3, "大模型标签抽取", 12.5, True, INK)
textbox(s, 1.1, 4.75, 5.15, 0.75,
        "商品标题/描述 → DeepSeek → 结构化标签入库，\n供内容过滤计算相似度；失败自动降级为规则匹配",
        10.5, False, MUTED, spacing=1.3)
rect(s, 6.85, 4.25, 5.6, 1.35, fill=SOFT, line_color=LINE)
textbox(s, 7.1, 4.4, 5.1, 0.3, "定时任务", 12.5, True, INK)
textbox(s, 7.1, 4.75, 5.15, 0.75,
        "每小时重算商品热度与用户画像，\n推荐接口直接读表，单次响应毫秒级",
        10.5, False, MUTED, spacing=1.3)

textbox(s, 0.85, 5.95, 11.6, 0.6,
        "工程取舍：接受约 1 小时的近线延迟，换取推荐接口的稳定响应；\n"
        "若追求实时性，可替换为流式计算，这是后续优化方向。",
        11, False, MUTED, spacing=1.4)

# ---------- 09 用户画像 ----------
s = new_slide(prs)
header(s, "PART 03 · 用户画像", "用时间衰减刻画「兴趣强度」",
       "同样一次浏览，昨天和一个月前的价值不同")

rect(s, 0.85, 1.95, 11.6, 1.15, fill=INK, line_color=None)
shape_text(s.shapes[-1], "score(u, t) =  Σ  w(action) · exp( −λ · Δt )        λ = 0.05",
           18, True, WHITE)
textbox(s, 0.85, 3.22, 11.6, 0.35,
        "w：行为权重（浏览 1 / 加购 2 / 下单 3）    Δt：距今小时数    λ：衰减系数",
        10.5, False, MUTED)

dims = [
    ("行为统计", ["浏览、加购、下单的加权计数", "反映兴趣强度与转化倾向"], BLUE),
    ("标签画像", ["权重最高的若干标签", "用于内容相似度计算"], GOLD),
    ("分类偏好", ["各分类的归一化权重向量", "决定召回时的品类倾斜"], INK2),
]
x = 0.85
for title, lines, accent in dims:
    card(s, x, 3.75, 3.7, 1.35, title, lines, accent, title_size=13, body_size=10.5)
    x += 3.95

textbox(s, 0.85, 5.35, 11.6, 0.3, "为什么用时间衰减", 13, True, INK)
bullet_block(s, 0.85, 5.72, 11.4, [
    ("兴趣会漂移", "三个月前买过邮票，不代表现在还想看邮票"),
    ("避免老行为主导", "简单累加会让「历史最多」永久压过「最近偏好」"),
    ("可解释", "画像中的标签与分类权重可直接展示给管理员，便于核对"),
], size=11.5, gap=0.42)

# ---------- 10 混合推荐 ----------
s = new_slide(prs)
header(s, "PART 03 · 混合推荐", "三路召回 + 加权融合 + 三层冷启动",
       "单一策略各有短板，混合的关键是让它们互补")

recalls = [
    ("内容过滤  α = 0.6", "余弦相似度：用户偏好向量 vs 商品标签向量",
     "cos = (Vu · Vp) / (|Vu| · |Vp|)"),
    ("热度召回  β = 0.3", "近 7 天加权：订单×3 + 加购×2 + 浏览×1",
     "hot(p) = 3·N_order + 2·N_cart + 1·N_view"),
    ("协同过滤  γ = 0.1", "Item-CF：按共同出现次数构建共现矩阵",
     "sim(i,j) = co(i,j) / √(deg(i)·deg(j))"),
]
x = 0.85
for title, desc, formula in recalls:
    rect(s, x, 1.95, 3.75, 1.9, fill=SOFT, line_color=LINE)
    textbox(s, x + 0.22, 2.08, 3.3, 0.3, title, 13, True, INK)
    textbox(s, x + 0.22, 2.45, 3.35, 0.55, desc, 10, False, MUTED, spacing=1.25)
    fb = rect(s, x + 0.22, 3.05, 3.3, 0.55, fill=WHITE, line_color=LINE)
    shape_text(fb, formula, 9, False, INK2)
    x += 3.95

rect(s, 0.85, 4.05, 11.6, 0.62, fill=INK, line_color=None)
shape_text(s.shapes[-1],
           "final = 0.6 × 内容 + 0.3 × 热度 + 0.1 × 协同        三路得分先归一化再相加",
           14, True, WHITE)

textbox(s, 0.85, 4.85, 11.6, 0.3, "冷启动三层兜底", 13, True, INK)
cold = [("无行为", "直接返回热门榜", BLUE), ("行为较少", "热度 + 内容混合", GOLD),
        ("行为充足", "三路完整混合", INK2)]
x = 0.85
for title, desc, accent in cold:
    rect(s, x, 5.25, 3.75, 0.95, fill=WHITE, line_color=accent)
    textbox(s, x + 0.22, 5.38, 3.3, 0.28, title, 12, True, INK)
    textbox(s, x + 0.22, 5.7, 3.3, 0.3, desc, 10.5, False, MUTED)
    x += 3.95

textbox(s, 0.85, 6.42, 11.6, 0.35,
        "此外在商品详情页提供「相似藏品」，按标签相似度即时计算。", 10.5, False, MUTED)

# ---------- 11 大模型标签 ----------
s = new_slide(prs)
header(s, "PART 03 · 大模型应用", "用 LLM 把商品描述变成结构化标签",
       "标签质量直接决定内容过滤的效果，人工标注不现实")

flow2 = [("商品文本", "标题 + 描述\n（分类作为先验）"), ("DeepSeek", "deepseek-flash\nJSON 结构化输出"),
         ("标签入库", "product_tag 表\n统一归一化")]
x = 0.85
for i, (title, sub) in enumerate(flow2):
    rect(s, x, 1.95, 2.85, 1.25, fill=SOFT, line_color=LINE)
    textbox(s, x + 0.2, 2.08, 2.5, 0.3, title, 13, True, INK)
    textbox(s, x + 0.2, 2.45, 2.5, 0.6, sub, 10, False, MUTED, spacing=1.25)
    if i < 2:
        textbox(s, x + 2.9, 2.35, 0.4, 0.4, "→", 20, True, GOLD)
    x += 3.3

textbox(s, 0.85, 3.45, 11.6, 0.3, "三个工程考虑", 13, True, INK)
points = [
    ("离线执行", "批量运行，不占用户请求链路；管理端可手动强制重抽"),
    ("失败降级", "Key 未配置或调用失败时，自动回退到「分类 + 关键词」规则匹配"),
    ("零新依赖", "用 JDK 自带 HttpClient 调用，不引入额外 SDK"),
]
x = 0.85
for title, desc in points:
    rect(s, x, 3.8, 3.75, 1.45, fill=WHITE, line_color=LINE)
    textbox(s, x + 0.22, 3.93, 3.3, 0.3, title, 12.5, True, GOLD)
    textbox(s, x + 0.22, 4.3, 3.35, 0.85, desc, 9.5, False, MUTED, spacing=1.35)
    x += 3.95

rect(s, 0.85, 5.35, 11.6, 1.25, fill=SOFT, line_color=LINE)
textbox(s, 1.1, 5.5, 5.2, 0.3, "抽取结果示例", 12.5, True, INK)
textbox(s, 1.1, 5.88, 5.2, 0.6,
        "「1980 年猴票 · 生肖 · 全新品相」\n→ 标签：生肖 / 邮票 / 年代感 / 全新",
        10.5, False, MUTED, spacing=1.35)
textbox(s, 6.9, 5.5, 5.3, 0.3, "在系统中的作用", 12.5, True, INK)
textbox(s, 6.9, 5.88, 5.3, 0.6,
        "93 条标签覆盖 19 件商品，支撑内容相似度计算；\n"
        "标签越准，猜你喜欢的可解释性越强。",
        10.5, False, MUTED, spacing=1.35)

# ---------- 12 客服助手 ----------
s = new_slide(prs)
header(s, "PART 03 · 大模型应用", "客服助手：先查知识库，再问大模型",
       "常见问题由知识库直接回答（0 成本、毫秒级），没覆盖的才产生 API 费用")

assist_steps = [("用户提问", "推荐问题或自由输入", BLUE),
                ("本地匹配", "关键词特征度 +\n二元组相似度，阈值 0.35", INK2),
                ("大模型兜底", "带上最相近的条目\n作为参考上下文", GOLD),
                ("未命中记录", "写入 chat_unanswered\n用于补充知识库", "5A6B7C")]
x = 0.85
for i, (title, sub, accent) in enumerate(assist_steps):
    rect(s, x, 2.0, 2.6, 1.45, fill=WHITE, line_color=accent)
    textbox(s, x + 0.2, 2.14, 2.2, 0.3, title, 14, True, INK)
    textbox(s, x + 0.2, 2.56, 2.25, 0.8, sub, 10, False, MUTED, spacing=1.3)
    if i < 3:
        textbox(s, x + 2.66, 2.55, 0.35, 0.4, "→", 20, True, GOLD)
    x += 3.05

assist_stats = [("24", "条知识库问答"), ("0.35", "命中阈值"),
                ("30", "每人每日次数"), ("47ms", "命中响应")]
x = 0.85
for num, label in assist_stats:
    rect(s, x, 3.72, 2.78, 0.95, fill=INK, line_color=None)
    textbox(s, x, 3.82, 2.78, 0.42, num, 20, True, WHITE, PP_ALIGN.CENTER)
    textbox(s, x, 4.28, 2.78, 0.28, label, 9.5, False, "C7D3E0", PP_ALIGN.CENTER)
    x += 2.94

textbox(s, 0.85, 4.9, 11.6, 0.3, "为什么不用向量检索", 13, True, INK)
bullet_block(s, 0.85, 5.26, 11.4, [
    ("够用", "24 条规模下关键词 + 二元组相似度已足够，匹配回归 14/14"),
    ("可解释", "命中哪条、得分多少都能说清楚，便于排查答非所问"),
    ("零依赖", "不需要向量库与 embedding 服务，部署与演示都更轻"),
], size=10.5, gap=0.36)

textbox(s, 0.85, 6.5, 11.6, 0.35,
        "安全约束：Key 只在后端；提示词要求不编造订单状态、不透露内部实现，超范围直接说不知道。",
        10, False, MUTED)

# ---------- 13 工程难点 ----------
s = new_slide(prs)
header(s, "PART 02 · 工程实践", "四个真实踩过的坑",
       "这些不是教科书问题，是实际运行中暴露并修复的")

pains = [
    ("JWT 无法撤销", "用户被禁用后，已签发的 Token 仍可用 24 小时",
     "引入 token_version：改密/禁用时版本 +1，拦截器比对，旧 Token 立即失效"),
    ("并发超卖", "先查库存再扣减，两个请求同时通过检查",
     "改为条件更新 UPDATE ... WHERE stock >= ?，影响行数为 0 即拒绝"),
    ("AI 依赖不可用", "未配置 Key 时推荐直接不可用",
     "标签抽取降级为规则匹配，主流程零影响"),
    ("前端缓存", "改了代码，页面还是旧的",
     "index.html 强制 no-store，静态资源加哈希指纹长期缓存"),
]
positions = [(0.85, 1.92), (6.85, 1.92), (0.85, 3.88), (6.85, 3.88)]
for (title, problem, fix), (x, y) in zip(pains, positions):
    rect(s, x, y, 5.6, 1.78, fill=SOFT, line_color=LINE)
    textbox(s, x + 0.25, y + 0.14, 5.1, 0.32, title, 14, True, INK)
    textbox(s, x + 0.25, y + 0.54, 5.15, 0.45, "问题：" + problem, 10.5, False, "A33A3A",
            spacing=1.25)
    textbox(s, x + 0.25, y + 1.02, 5.15, 0.7, "解决：" + fix, 10.5, False, MUTED, spacing=1.3)

textbox(s, 0.85, 6.35, 11.6, 0.45,
        "其中 Token 撤销与库存并发均补充了针对性测试用例，避免回归。",
        11, False, MUTED)

# ---------- 13 测试 ----------
s = new_slide(prs)
header(s, "PART 04 · 系统测试", "138 个自动化测试",
       "JUnit 5 + Mockito + MockMvc，Allure 生成可视化报告")

stats = [("138", "自动化测试"), ("98", "单元测试"), ("40", "API 集成测试"), ("6", "测试模块")]
x = 0.85
for num, label in stats:
    rect(s, x, 1.95, 2.78, 1.1, fill=INK, line_color=None)
    textbox(s, x, 2.08, 2.78, 0.5, num, 26, True, WHITE, PP_ALIGN.CENTER)
    textbox(s, x, 2.62, 2.78, 0.3, label, 10.5, False, "C7D3E0", PP_ALIGN.CENTER)
    x += 2.94

textbox(s, 0.85, 3.3, 5.6, 0.3, "单元测试分布", 13, True, INK)
rows = [("订单服务", "21"), ("购物车服务", "15"), ("收货地址", "16"),
        ("推荐模块", "25"), ("图形验证码", "6"), ("商品服务", "6"), ("客服助手", "9")]
y = 3.7
for name, cnt in rows:
    textbox(s, 0.85, y, 2.6, 0.3, name, 11, False, TEXT)
    textbox(s, 2.9, y, 0.8, 0.3, cnt, 11, True, GOLD, PP_ALIGN.RIGHT)
    bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(3.85), Inches(y + 0.09),
                             Inches(float(cnt) / 25 * 2.2), Inches(0.12))
    bar.fill.solid()
    bar.fill.fore_color.rgb = rgb(BLUE)
    bar.line.fill.background()
    bar.shadow.inherit = False
    y += 0.42

textbox(s, 6.85, 3.3, 5.6, 0.3, "测什么", 13, True, INK)
bullet_block(s, 6.85, 3.72, 5.5, [
    ("业务规则", "状态机非法流转、越权、库存不足"),
    ("边界条件", "空参数、超长输入、负数金额"),
    ("异常路径", "服务异常时的提示与回滚"),
    ("接口契约", "状态码、返回结构、鉴权拦截"),
], size=11, gap=0.5)

rect(s, 6.85, 5.75, 5.6, 0.95, fill=SOFT, line_color=LINE)
textbox(s, 7.1, 5.88, 5.1, 0.7,
        "另有 25 个黑盒用例（论文 6.6），\n覆盖注册登录到下单收货的完整流程。",
        10.5, False, MUTED, spacing=1.3)

# ---------- 14 评估 ----------
s = new_slide(prs)
header(s, "PART 04 · 效果评估", "四种策略的离线对比",
       "时间切分：训练 5606 条 / 测试 3634 条，可评估用户 202 个")

chart_data = CategoryChartData()
chart_data.categories = ["POP 热门", "CONTENT 内容", "CF 协同", "HYBRID 混合"]
chart_data.add_series("Precision@5", (0.4287, 0.7208, 0.6683, 0.7307))
chart_data.add_series("NDCG@5", (0.4664, 0.8055, 0.7389, 0.8129))
gf = s.shapes.add_chart(XL_CHART_TYPE.COLUMN_CLUSTERED, Inches(0.85), Inches(1.9),
                        Inches(7.1), Inches(4.3), chart_data)
chart = gf.chart
chart_font(chart, 10)
chart.has_title = False
chart.has_legend = True
chart.legend.position = XL_LEGEND_POSITION.BOTTOM
chart.legend.include_in_layout = False
va = chart.value_axis
va.maximum_scale = 1.0
va.minimum_scale = 0.0
va.has_major_gridlines = True
va.tick_labels.font.size = Pt(9)
plot = chart.plots[0]
plot.gap_width = 60
for series, color in zip(chart.series, (BLUE, "C0553F")):
    series.format.fill.solid()
    series.format.fill.fore_color.rgb = rgb(color)

plot.has_data_labels = True
dl = plot.data_labels
dl.number_format = "0.00"
dl.number_format_is_linked = False
dl.font.size = Pt(9)
dl.font.bold = True
dl.font.color.rgb = rgb(INK)

textbox(s, 8.2, 1.95, 4.3, 0.3, "结论", 13, True, INK)
conclusions = [
    ("个性化 > 热门", "P@5 从 0.4287 提升到 0.7307，\n相对提升 70.4%"),
    ("混合 > 单一", "K=10 时 P 为 0.5149，\n高于内容过滤的 0.4802"),
    ("覆盖更广", "覆盖率 26.32% → 100%，\n不再「永远推同样几件」"),
]
y = 2.35
for title, desc in conclusions:
    textbox(s, 8.2, y, 4.2, 0.28, title, 11.5, True, GOLD)
    textbox(s, 8.2, y + 0.3, 4.25, 0.7, desc, 10, False, MUTED, spacing=1.3)
    y += 1.28

textbox(s, 0.85, 6.32, 11.6, 0.55,
        "实验设置：19 件商品 / 7 个分类 / 93 条标签 / 9241 条行为记录 / 200 个模拟用户；"
        "参数与线上一致（λ=0.05，α/β/γ=0.6/0.3/0.1）。",
        9.5, False, MUTED, spacing=1.3)

# ---------- 15 演示 ----------
s = new_slide(prs)
header(s, "PART 04 · 系统演示", "现场演示路径",
       "按用户旅程走一遍，重点看推荐与交易闭环")

demo_steps = [
    ("① 首页", "轮播图 + 热藏榜（3:1 布局）\n筛选商品时自动收起"),
    ("② 详情与推荐", "商品详情、「相似藏品」\n「为你推荐」推荐理由"),
    ("③ 交易", "加购 → 结算 → 下单\n取消订单后库存回补"),
    ("④ 后台", "用户画像、订单发货\n轮播图管理"),
]
positions = [(0.85, 1.95), (6.85, 1.95), (0.85, 3.75), (6.85, 3.75)]
for (title, desc), (x, y) in zip(demo_steps, positions):
    rect(s, x, y, 5.6, 1.5, fill=SOFT, line_color=LINE)
    textbox(s, x + 0.25, y + 0.15, 5.1, 0.3, title, 13, True, INK)
    textbox(s, x + 0.25, y + 0.55, 5.15, 0.75, desc, 10.5, False, MUTED, spacing=1.35)
    textbox(s, x + 5.0, y + 0.15, 0.5, 0.3, "▷", 12, True, GOLD)

rect(s, 0.85, 5.55, 11.6, 1.0, fill=WHITE, line_color=GOLD)
textbox(s, 1.1, 5.68, 11.1, 0.3, "演示账号", 11.5, True, GOLD)
textbox(s, 1.1, 6.02, 11.1, 0.4,
        "普通用户 user / 123456        管理员 admin / 123456        访问地址 http://localhost",
        11, False, TEXT)

# ---------- 16 总结 ----------
s = new_slide(prs, INK)
textbox(s, 0.9, 0.75, 6, 0.35, "SUMMARY", 12, True, GOLD)
textbox(s, 0.9, 1.1, 11, 0.6, "工作总结与展望", 28, True, WHITE)

textbox(s, 0.9, 2.05, 5.5, 0.3, "已完成", 13, True, GOLD)
bullet_block(s, 0.9, 2.45, 5.4, [
    "完整交易闭环与后台管理",
    "用户画像 + 三路混合推荐",
    "大模型标签抽取与降级策略",
    "138 个自动化测试 + 离线评估",
], size=11.5, gap=0.52, color="C7D3E0")

textbox(s, 6.9, 2.05, 5.6, 0.3, "不足与展望", 13, True, GOLD)
bullet_block(s, 6.9, 2.45, 5.5, [
    "实验基于模拟数据，需真实流量验证",
    "推荐为近线计算，约有 1 小时延迟",
    "单店铺模式，未支持多商家入驻",
], size=11.5, gap=0.52, color="C7D3E0")

rect(s, 0.9, 4.35, 11.5, 0.02, fill=GOLD, line_color=None)
textbox(s, 0.9, 4.6, 11.5, 0.35, "后续方向", 12, True, GOLD)
textbox(s, 0.9, 4.98, 11.5, 0.5,
        "实时流式计算替代定时任务     ·     多模态（图像特征）参与推荐     ·     在线支付与评价体系",
        11.5, False, "C7D3E0")

textbox(s, 0.9, 5.95, 11.5, 0.5, "感谢各位老师，请批评指正", 20, True, WHITE)
textbox(s, 0.9, 6.6, 11.5, 0.35, "Q & A", 12, True, GOLD)

# 页码（封面与结尾页不加）
for i, slide in enumerate(prs.slides, start=1):
    if i in (1, len(prs.slides._sldIdLst)):
        continue
    footer(slide, i, len(prs.slides._sldIdLst))

prs.save(OUT)
print("已生成:", OUT)
print("页数:", len(prs.slides.__iter__.__self__._sldIdLst))
