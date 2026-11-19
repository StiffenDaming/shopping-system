# -*- coding: utf-8 -*-
"""
生成课程设计报告初稿。

输入：docs/课程设计/00-课设报告模板.docx（只读，不修改）
输出：docs/课程设计/03-课程设计报告-初稿.docx
      docs/课程设计/03-配图清单.md

做法：保留模板的封面与任务书，从「第1章」开始重建正文，
     沿用模板的 Heading 1/2/3 与 Normal 样式（宋体小四、首行缩进 2 字符）。
     所有插图位置插入「【此处插入图片：图x.x 图题】」占位，并附图题。

运行：python tools/build_course_report.py
"""

import os
import re

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DIR = os.path.join(BASE, "docs", "课程设计")
SRC = os.path.join(DIR, "00-课设报告模板.docx")
DST = os.path.join(DIR, "03-课程设计报告-初稿.docx")
SCHEMA = os.path.join(BASE, "backend", "src", "main", "resources", "schema-mysql.sql")
FIG_MD = os.path.join(DIR, "03-配图清单.md")

CENTER = WD_ALIGN_PARAGRAPH.CENTER

MEMBERS = [
    ("李云天", "2023070330205", "组长", "架构与认证、推荐算法核心、论文统筹"),
    ("潘子皓", "2023070330217", "组员", "交易主链路（浏览、购物车、地址、下单、订单）"),
    ("张程", "2023070330220", "组员", "管理后台六个页面、前端公共组件"),
    ("李兴南", "2023070330241", "组员", "推荐工程（埋点、画像、热度、标签、评估）"),
    ("邱俊霖", "2023070330242", "组员", "注册登录、客服助手、测试体系、部署"),
]

TEACHERS = "张亚红、刘建华、劳传媛、王也"
TITLE = "收藏品商城系统的设计与实现"
PERIOD = "2026.11.09~2026.11.18"


# ==================== 基础排版工具 ====================
def set_east_asia(run, font):
    rpr = run._r.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        from lxml import etree
        rfonts = etree.SubElement(rpr, qn("w:rFonts"))
    rfonts.set(qn("w:eastAsia"), font)


def para(doc, text, indent=True, size=None, bold=False, align=None, color=None,
         font=None):
    p = doc.add_paragraph(style="Normal")
    run = p.add_run(text)
    if size:
        run.font.size = Pt(size)
    run.font.bold = bold
    if font:
        run.font.name = font
        set_east_asia(run, font)
    if color:
        run.font.color.rgb = RGBColor.from_string(color)
    if align is not None:
        p.alignment = align
    p.paragraph_format.first_line_indent = Pt(24) if indent else Pt(0)
    return p


def heading(doc, text, level, new_page=False):
    p = doc.add_paragraph(text, style=f"Heading {level}")
    if new_page:
        p.paragraph_format.page_break_before = True
    return p


def caption(doc, text):
    p = doc.add_paragraph(style="Normal")
    r = p.add_run(text)
    r.font.size = Pt(10.5)
    p.alignment = CENTER
    p.paragraph_format.first_line_indent = Pt(0)


def figure(doc, title):
    """插图占位 + 图题"""
    p = doc.add_paragraph(style="Normal")
    r = p.add_run(f"【此处插入图片：{title}】")
    r.font.size = Pt(10.5)
    r.font.color.rgb = RGBColor(0x80, 0x80, 0x80)
    p.alignment = CENTER
    p.paragraph_format.first_line_indent = Pt(0)
    caption(doc, title)


def code_block(doc, text):
    for line in text.strip("\n").split("\n"):
        p = doc.add_paragraph(style="Normal")
        r = p.add_run(line if line else " ")
        r.font.name = "Consolas"
        r.font.size = Pt(9)
        set_east_asia(r, "Consolas")
        p.paragraph_format.first_line_indent = Pt(0)
        p.paragraph_format.line_spacing = Pt(13)


def _table_style_name(doc):
    try:
        doc.styles["Table Grid"]
        return "Table Grid"
    except KeyError:
        return "Normal Table"


def _add_borders(table):
    """表格无内置样式时，手工加全框线。"""
    from docx.oxml import OxmlElement
    tblPr = table._tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), "single")
        el.set(qn("w:sz"), "4")
        el.set(qn("w:space"), "0")
        el.set(qn("w:color"), "000000")
        borders.append(el)
    tblPr.append(borders)


def make_table(doc, header, rows, widths=None, font_size=9):
    table = doc.add_table(rows=1, cols=len(header))
    style_name = _table_style_name(doc)
    table.style = style_name
    if style_name == "Normal Table":
        _add_borders(table)
    if widths:
        table.autofit = False
    else:
        table.autofit = True
    hdr = table.rows[0].cells
    for i, text in enumerate(header):
        hdr[i].text = ""
        p = hdr[i].paragraphs[0]
        r = p.add_run(str(text))
        r.font.size = Pt(font_size)
        r.font.bold = True
        p.alignment = CENTER
        p.paragraph_format.first_line_indent = Pt(0)
    for row in rows:
        cells = table.add_row().cells
        for i, text in enumerate(row):
            cells[i].text = ""
            p = cells[i].paragraphs[0]
            r = p.add_run(str(text))
            r.font.size = Pt(font_size)
            p.paragraph_format.first_line_indent = Pt(0)
            if i == 0:
                p.alignment = CENTER
    if widths:
        for idx, w in enumerate(widths):
            table.columns[idx].width = Cm(w)
        for row in table.rows:
            for idx, w in enumerate(widths):
                row.cells[idx].width = Cm(w)
    # 跨页时重复表头，且不允许单行被拆到两页
    from docx.oxml import OxmlElement
    header_trPr = table.rows[0]._tr.get_or_add_trPr()
    header_trPr.append(OxmlElement("w:tblHeader"))
    for row in table.rows:
        row._tr.get_or_add_trPr().append(OxmlElement("w:cantSplit"))
    return table


# ==================== 解析建表脚本，生成数据字典 ====================
TYPE_LEN = {"BIGINT": "20", "INT": "11", "DATETIME": "-", "TEXT": "-",
            "DECIMAL": "-", "DATE": "-", "TIMESTAMP": "-"}


def parse_schema():
    sql = open(SCHEMA, encoding="utf-8").read()
    tables = []
    pattern = re.compile(
        r"CREATE TABLE IF NOT EXISTS `(\w+)` \((.*?)\n\) ENGINE=.*?COMMENT='([^']*)';",
        re.S)
    for m in pattern.finditer(sql):
        name, body, comment = m.group(1), m.group(2), m.group(3)
        pk_match = re.search(r"PRIMARY KEY \(`(\w+)`\)", body)
        pk = pk_match.group(1) if pk_match else ""
        cols = []
        for line in body.split("\n"):
            line = line.strip()
            if not line.startswith("`"):
                continue
            cm = re.match(r"`(\w+)`\s+([A-Z]+)(?:\(([\d,]+)\))?(.*)", line)
            if not cm:
                continue
            col, ctype, length, rest = cm.group(1), cm.group(2), cm.group(3), cm.group(4)
            cc = re.search(r"COMMENT '([^']*)'", rest)
            cols.append({
                "name": col,
                "type": ctype,
                "len": length or TYPE_LEN.get(ctype, "-"),
                "comment": cc.group(1) if cc else "",
                "pk": "Y" if col == pk else "N",
                "null": "N" if "NOT NULL" in rest else "Y",
            })
        tables.append({"name": name, "comment": comment, "cols": cols})
    return tables


# ==================== 内容 ====================
DESIGN_CONTENT = (
    "本课程设计以收藏品交易为业务背景，设计并实现一套基于 Spring Boot + Vue3 前后端分离的"
    "收藏品商城系统，主要完成以下内容：\n"
    "（1）用户端：注册（含图形验证码）、登录与退出、商品浏览与检索、购物车、收货地址、"
    "下单与订单管理（取消、确认收货、未读提醒）、个人中心与修改密码；\n"
    "（2）管理端：数据看板、商品管理、订单管理（发货、强制完成）、用户管理（禁用、重置密码）、"
    "轮播图管理、用户画像查看；\n"
    "（3）智能推荐：用户行为埋点、时间衰减用户画像、热门榜、内容过滤与协同过滤的混合推荐、"
    "大模型商品标签抽取，并完成离线对比评估；\n"
    "（4）客服助手：本地知识库优先匹配，未命中转大模型，含每日限额与兜底，未命中问题沉淀入库；\n"
    "（5）工程实践：JWT 令牌撤销机制、库存并发控制、Docker 容器化部署，以及 138 个自动化测试。"
)

DESIGN_REQUIRE = (
    "1）采用 Java + Spring Boot 技术栈，前后端分离，前端使用 Vue3 + Element Plus；\n"
    "2）数据库采用 MySQL 8，完成概念结构、逻辑结构与物理表设计；\n"
    "3）实现下单—发货—收货的完整交易闭环，保证库存与订单状态一致；\n"
    "4）推荐模块需给出可解释的推荐理由，并提供离线评估数据；\n"
    "5）代码分层清晰、命名规范，关键逻辑有注释；\n"
    "6）编写单元测试与接口测试，使用 Docker Compose 完成一键部署；\n"
    "7）按时提交课程设计报告、系统源码与演示。"
)

DESIGN_PLAN = (
    "11.09—11.10：需求分析与功能模块划分，完成数据库设计；\n"
    "11.11—11.12：搭建工程脚手架，完成用户认证与权限、商品浏览与购物车；\n"
    "11.13—11.14：完成收货地址、下单与订单状态流转，处理库存并发；\n"
    "11.15：完成管理后台各功能模块；\n"
    "11.16—11.17：完成推荐模块与客服助手，并进行离线评估；\n"
    "11.18：系统测试、Docker 部署、文档整理与答辩准备。"
)

DESIGN_RESULT = (
    "1）收藏品商城系统（前后端分离，含个性化推荐与客服助手）；\n"
    "2）课程设计报告；\n"
    "3）测试用例文档与 Allure 测试报告；\n"
    "4）部署文档（README 与从零启动指南）；\n"
    "5）答辩演示文稿。"
)

REFS_TASK = (
    "[1] 王福强. Spring Boot 揭秘：快速构建微服务体系[M]. 北京：机械工业出版社，2016.\n"
    "[2] 尤雨溪. Vue.js 官方文档[EB/OL]. https://cn.vuejs.org/.\n"
    "[3] 李兴华. MyBatis 从入门到精通[M]. 北京：清华大学出版社，2019."
)


# ==================== 主流程 ====================
def main():
    doc = Document(SRC)

    # ---------- 1. 填任务书表格 ----------
    # 表头为 8 行：课程名称/开课学期/专业/年级/项目名称/项目成员/指导教师/完成日期
    header_values = {
        0: "计科专业综合应用课程设计",       # 课程名称
        1: "2026～2027学年第一学期",         # 开课学期
        2: "计算机科学与技术",               # 专业
        3: "2023级",                         # 年级
        4: TITLE,                             # 项目名称
        5: "、".join(f"{m[1]}{m[0]}" for m in MEMBERS),  # 项目成员
        6: TEACHERS,                          # 指导教师
        7: "2026年11月18日",                 # 完成日期
    }

    def fill_3col(table):
        for r, value in header_values.items():
            cells = table.rows[r].cells
            target = cells[2] if len(cells) >= 3 else cells[-1]
            target.text = ""
            p = target.paragraphs[0]
            run = p.add_run(value)
            run.font.size = Pt(10.5)

    fill_3col(doc.tables[0])
    fill_3col(doc.tables[2])

    # 表2：任务书正文
    t2 = doc.tables[1]
    task_rows = {
        0: (1, "、".join(m[0] for m in MEMBERS)),
        2: (1, "计科专业综合应用课程设计"),
        2.5: None,
        3: (1, "实验楼B244"),
        3.5: None,
        4: (1, DESIGN_CONTENT),
        5: (1, DESIGN_REQUIRE),
        6: (1, DESIGN_PLAN),
        7: (1, DESIGN_RESULT),
    }
    # 逐行按「去重后的单元格」写入，兼容合并单元格
    def distinct_cells(row):
        seen, out = set(), []
        for c in row.cells:
            if id(c._tc) in seen:
                continue
            seen.add(id(c._tc))
            out.append(c)
        return out

    def set_cell(cell, text, size=10.5):
        cell.text = ""
        lines = str(text).split("\n")
        for i, line in enumerate(lines):
            p = cell.paragraphs[0] if i == 0 else cell.add_paragraph()
            r = p.add_run(line)
            r.font.size = Pt(size)
            p.paragraph_format.first_line_indent = Pt(0)
            p.paragraph_format.line_spacing = Pt(13)

    # row0：学生姓名
    cells = distinct_cells(t2.rows[0])
    set_cell(cells[1], "、".join(m[0] for m in MEMBERS))
    # row1：课程名称 | 值 | 年级专业 | 值
    cells = distinct_cells(t2.rows[1])
    set_cell(cells[1], "计科专业综合应用课程设计")
    set_cell(cells[3], "2023级计算机科学与技术")
    # row2：地点 | 值 | 起止时间 | 值
    cells = distinct_cells(t2.rows[2])
    set_cell(cells[1], "实验楼B244")
    set_cell(cells[3], PERIOD)
    # row3~7：长文本，第2格可能横跨
    for ridx, text in ((3, DESIGN_CONTENT), (4, DESIGN_REQUIRE),
                       (5, DESIGN_PLAN), (6, DESIGN_RESULT), (7, REFS_TASK)):
        cells = distinct_cells(t2.rows[ridx])
        set_cell(cells[-1] if len(cells) > 2 else cells[1], text)

    # ---------- 2. 删除「第1章」及之后的正文（保留 sectPr） ----------
    body = doc.element.body
    started = False
    for child in list(body):
        if child.tag == qn("w:p") and not started:
            text = "".join(t.text or "" for t in child.iter(qn("w:t")))
            if text.strip().startswith("第1章"):
                started = True
        if started and child.tag != qn("w:sectPr"):
            body.remove(child)

    # 模板自带的旧目录是 Word 的 TOC，包在 w:sdt 里，段落遍历看不到，单独清掉
    for child in list(body):
        if child.tag == qn("w:sdt"):
            body.remove(child)

    # 任务书封面上的旧题目与日期
    for p in doc.paragraphs:
        t = (p.text or "").strip()
        if t.startswith("设计题目"):
            for r in p.runs[1:]:
                r._r.getparent().remove(r._r)
            if p.runs:
                p.runs[0].text = "设计题目：" + TITLE
                p.runs[0].font.highlight_color = None
        elif "2026 年 10 月 9 日" in t:
            for r in p.runs[1:]:
                r._r.getparent().remove(r._r)
            if p.runs:
                p.runs[0].text = "2026 年 11 月 09 日"

    # ---------- 3. 目录占位 ----------
    heading(doc, "目  录", 1, new_page=True)
    note = para(doc, "（此处插入目录：在 Word 中执行「引用 → 目录 → 自动目录」，"
                     "生成后删除本行提示；正文修改后右键目录选择「更新域」即可同步页码。）",
                indent=False)
    for r in note.runs:
        r.font.size = Pt(9)
        r.font.color.rgb = RGBColor(0x99, 0x99, 0x99)
    note.alignment = CENTER

    # ---------- 4. 第1章 ----------
    heading(doc, "第1章 系统描述", 1, new_page=True)
    heading(doc, "1.1 设计目的", 2)
    para(doc, "收藏品交易长期依赖线下古玩市场与拍卖会，交易受时间与空间限制，"
              "藏品展示品类有限、运营成本较高；而入驻综合电商平台又面临费用高、规则多、"
              "数据不自主等问题，商家难以针对收藏品品类做精细化运营。"
              "因此，设计一套轻量、可扩展、便于二次开发的垂直电商系统，具有实际应用价值。")
    para(doc, "本课程设计采用 Spring Boot + Vue3 前后端分离技术栈，实现一套收藏品商城系统，"
              "完成从商品浏览、购物车、下单到发货、收货的完整交易闭环，"
              "并在此基础上引入用户画像与混合推荐，使系统在基础交易功能之外具备一定的智能化服务能力。"
              "同时补充客服助手，用「知识库优先、大模型兜底」的方式降低人工答疑成本。")
    para(doc, "通过本课程设计，达到三个目的：一是完整实践前后端分离项目的工程组织与接口设计；"
              "二是掌握数据库设计、并发控制、鉴权与部署等关键工程能力；"
              "三是把课堂所学的数据结构、算法与软件工程方法落到可运行、可测试、可部署的系统上。")

    heading(doc, "1.2 需求分析", 2)
    requirements = [
        ("1.2.1 用户注册与登录", [
            "用户可通过注册创建账号，填写用户名、密码、邮箱，并输入图形验证码；"
            "注册成功后使用用户名和密码登录。系统基于 JWT 实现身份认证，密码采用 BCrypt 加密存储。",
            "功能特点：注册环节加入图形验证码，可有效抑制脚本批量注册；"
            "令牌带有版本号，用户修改密码或被禁用后，已签发的令牌立即失效，无需等待过期。"]),
        ("1.2.2 商品浏览与检索", [
            "用户可在首页查看全部上架商品，浏览名称、价格、图片与详情；支持关键词搜索、"
            "按分类筛选、按价格或随机排序，并支持分页浏览；点击商品进入详情页。",
            "功能特点：首页顶部左侧为轮播图、右侧为热藏榜，按 3:1 宽度并排展示；"
            "用户进行搜索或筛选时该区域自动收起，避免遮挡商品列表。"]),
        ("1.2.3 购物车", [
            "用户可将商品加入购物车，修改购买数量、删除购物车项，查看合计金额并一键结算；"
            "导航栏实时显示购物车商品数量。",
            "功能特点：加购与修改数量时均校验库存，超出库存会给出明确提示，避免下单时才发现无货。"]),
        ("1.2.4 收货地址", [
            "用户可管理多个收货地址，支持新增、修改、删除与设为默认地址；结算时自动带入默认地址。",
            "功能特点：收货手机号在前后端均按 11 位手机号格式校验，非法输入会被立即拦截。"]),
        ("1.2.5 订单交易", [
            "用户从购物车提交订单，可查看订单列表与详情；待发货状态下可取消订单，已发货状态下可确认收货。"
            "管理员发货后，导航栏出现未读红点提醒。",
            "功能特点：订单只允许合法流转；取消订单时库存自动回补；"
            "库存扣减使用条件更新，避免并发下单造成超卖。"]),
        ("1.2.6 个性化推荐", [
            "系统记录用户的浏览、加购与下单行为，据此构建用户画像，"
            "并在首页提供热藏榜与「为你推荐」，在详情页提供「相似藏品」；未登录用户也能看到热门榜。",
            "功能特点：每条推荐都附带推荐理由；无行为的新用户由热门榜兜底，行为越多推荐越贴合偏好。"]),
        ("1.2.7 客服助手", [
            "用户可在客服助手中咨询下单、支付、物流、售后、账号等问题，既能点击推荐问题，也能自由输入。",
            "功能特点：常见问题由本地知识库直接回答，响应快且不产生调用费用；"
            "知识库未覆盖时才请求大模型，仍无法回答则给出兜底话术并引导联系管理员。"]),
        ("1.2.8 后台管理", [
            "管理员可查看平台运营数据（用户、商品、订单、销售额），管理商品（增删改、上下架）、"
            "订单（发货、强制完成）、用户（禁用、启用、重置密码）与首页轮播图，并可查看用户画像。",
            "功能特点：后台接口统一做管理员鉴权，普通用户即使直接访问地址也会被拒绝。"]),
        ("1.2.9 非功能需求", [
            "安全性：接口统一鉴权，管理员接口仅管理员可访问，密码加密存储；"
            "性能：推荐结果由定时任务预计算，接口只做读取与加权，保证响应速度；"
            "可部署性：使用 Docker Compose 一键启动，前后端与数据库各自独立。",
            "可维护性：代码分层清晰、命名规范、关键逻辑有注释，并配套 138 个自动化测试。"]),
    ]
    for title, paras in requirements:
        heading(doc, title, 3)
        for t in paras:
            para(doc, t)

    # ---------- 5. 第2章 ----------
    heading(doc, "第2章 系统设计", 1)
    heading(doc, "2.1 功能模块划分", 2)
    para(doc, "系统按「前台用户、后台管理、智能服务」三条主线划分模块。"
              "前台面向普通用户，覆盖注册登录、商品浏览、购物车、收货地址、订单与个人中心；"
              "后台面向管理员，覆盖数据看板、商品、订单、用户、轮播图与用户画像；"
              "智能服务包括个性化推荐与客服助手。"
              "整体采用前后端分离架构：前端 Vue3 负责页面渲染与交互，"
              "通过 Nginx 反向代理访问后端 Spring Boot 提供的 REST 接口，数据存储于 MySQL。")
    figure(doc, "图2.1 功能模块划分图")

    heading(doc, "2.2 主要功能流程设计", 2)
    flows = [
        ("2.2.1 用户注册流程", "图2.2 用户注册流程图",
         "用户在注册页填写用户名、密码、邮箱并输入图形验证码后提交。"
         "前端先做格式校验，通过后请求注册接口；后端首先校验验证码是否有效（过期或已使用则拒绝），"
         "再检查用户名是否已存在，最后使用 BCrypt 加密密码并写入数据库，返回注册成功提示。"),
        ("2.2.2 下单与库存扣减流程", "图2.3 下单与库存扣减流程图",
         "用户结算时提交收货信息，系统遍历购物车项：校验商品是否上架、库存是否充足，"
         "计算订单总额并写入订单主表；随后逐条写入订单明细（保存商品名称与成交价快照），"
         "并执行条件更新扣减库存；若某件商品扣减影响行数为 0，说明库存已被其他请求占用，"
         "则抛出异常并由事务回滚，最后清除已结算的购物车项。"),
        ("2.2.3 取消订单与库存恢复流程", "图2.4 取消订单与库存恢复流程图",
         "用户在我的订单中发起取消，系统校验订单归属与状态，只有「待发货」的订单允许取消；"
         "随后按订单明细逐条执行原子更新，把数量加回商品库存，最后将订单状态置为「已取消」。"
         "使用单条 UPDATE 累加而非「先查再改」，避免并发取消时的库存丢失更新。"),
        ("2.2.4 个性化推荐生成流程", "图2.5 个性化推荐生成流程图",
         "推荐由定时任务近线计算：先按时间衰减聚合用户行为构建画像，"
         "再分别计算内容相似度、商品热度与协同过滤三路得分，"
         "按 0.6、0.3、0.1 的权重加权求和后排序写入推荐结果。"
         "用户无历史行为时走冷启动策略，直接按热度返回。"),
        ("2.2.5 客服助手问答流程", "图2.6 客服助手问答流程图",
         "用户提交问题后，系统先做文本归一化，再在本地知识库中按「关键词特征度 + 二元组相似度」打分，"
         "最高分不低于阈值即直接返回预置答案；否则判断大模型是否可用与当日次数是否用尽，"
         "可用则携带最相近条目作为上下文请求大模型，失败或不可用时返回兜底话术，"
         "并把未命中的问题写入数据库供后续补充知识库。"),
    ]
    for title, fig, text in flows:
        heading(doc, title, 3)
        para(doc, text)
        figure(doc, fig)

    heading(doc, "2.3 数据存储设计", 2)
    tables = parse_schema()
    para(doc, f"本系统使用 MySQL 8 作为数据库管理系统，共设计 {len(tables)} 张表，"
              "分为交易核心、智能推荐与运营辅助三类。"
              "交易核心表保证下单、支付与库存的一致性；推荐相关表独立存放行为、标签、画像与热度数据，"
              "可按需清空重算而不影响交易；运营辅助表保存轮播图与客服未命中问题。"
              "各表结构如下。")
    for idx, table in enumerate(tables, start=1):
        caption(doc, f"表2.{idx} {table['comment']}（{table['name']}）")
        rows = []
        for i, col in enumerate(table["cols"], start=1):
            rows.append([i, col["name"], col["type"], col["len"],
                         col["comment"], col["pk"], col["null"]])
        make_table(doc, ["序号", "字段名", "类型", "长度", "说明", "主键(Y/N)", "允许空(Y/N)"],
                   rows, widths=[0.8, 3.4, 2.0, 1.1, 5.3, 1.5, 1.7], font_size=8.5)
        doc.add_paragraph(style="Normal")

    # ---------- 6. 第3章 ----------
    heading(doc, "第3章 项目组成员分工计划", 1)
    heading(doc, "3.1 项目组成员信息", 2)
    para(doc, f"本项目由 5 名同学组成，项目名称：{TITLE}，"
              f"设计起止时间：{PERIOD}，指导教师：{TEACHERS}。成员信息如表3.1所示。")
    caption(doc, "表3.1 项目组成员信息表")
    rows = [[m[1], m[0], m[2], m[3]] for m in MEMBERS]
    make_table(doc, ["学号", "姓名", "角色", "主要负责内容"], rows,
               widths=[3.3, 2.0, 1.4, 9.1], font_size=9)
    doc.add_paragraph(style="Normal")

    heading(doc, "3.2 项目组计划及分工", 2)
    para(doc, "项目按需求分析、脚手架与数据库、交易链路、后台管理、推荐模块、客服助手、"
              "测试与部署七个阶段推进，各阶段分工与计划时间如表3.2所示。")
    caption(doc, "表3.2 项目组计划及分工表")
    plan = [
        ("1", "需求分析与功能模块划分、原型设计", "11.09—11.10", "全组"),
        ("2", "数据库设计（13 张表）与工程脚手架", "11.11", "李云天、邱俊霖"),
        ("3", "用户认证与权限（JWT、令牌撤销、验证码）", "11.11—11.12", "李云天、邱俊霖"),
        ("4", "商品浏览、购物车、收货地址", "11.12—11.13", "潘子皓"),
        ("5", "下单、订单状态流转与库存并发处理", "11.13—11.14", "潘子皓"),
        ("6", "管理后台（看板、商品、订单、用户、轮播图、画像）", "11.15", "张程"),
        ("7", "行为埋点、用户画像、热门榜、大模型标签", "11.15—11.16", "李兴南"),
        ("8", "内容过滤、协同过滤与混合推荐", "11.16", "李云天"),
        ("9", "推荐接口与前端推荐位、离线评估", "11.16—11.17", "李兴南"),
        ("10", "客服助手（知识库、匹配算法、大模型兜底）", "11.17", "邱俊霖"),
        ("11", "单元测试、接口测试与系统测试", "11.17—11.18", "邱俊霖、潘子皓"),
        ("12", "Docker 部署、文档整理与答辩准备", "11.18", "全组"),
    ]
    make_table(doc, ["序号", "工作内容", "计划起止", "负责人"], plan,
               widths=[1.1, 8.2, 3.2, 3.3], font_size=9)
    doc.add_paragraph(style="Normal")

    # ---------- 7. 第4章 ----------
    heading(doc, "第4章 系统实现", 1)
    heading(doc, "4.1 开发运行环境", 2)
    para(doc, "系统采用前后端分离架构，开发与运行环境如表4.1所示。"
              "后端与数据库通过 Docker Compose 编排，一条命令即可完成部署。")
    caption(doc, "表4.1 开发与运行环境")
    env = [
        ("操作系统", "Windows 11 + WSL2（Ubuntu）"),
        ("JDK", "OpenJDK 11"),
        ("后端框架", "Spring Boot 2.7.18 + MyBatis-Plus 3.5.3.1"),
        ("数据库", "MySQL 8.0"),
        ("构建工具", "Maven 3.8"),
        ("前端框架", "Vue 3 + Element Plus"),
        ("前端构建", "Vite 5 + Node.js 18"),
        ("容器化", "Docker + Docker Compose + Nginx"),
    ]
    make_table(doc, ["类别", "名称与版本"], env, widths=[3.6, 12.2], font_size=9)
    doc.add_paragraph(style="Normal")

    impl = [
        ("4.2 注册登录", [
            ("4.2.1 注册用户", "图4.1 注册页面",
             "注册页包含用户名、密码、确认密码、邮箱与图形验证码。验证码由后端用 5×7 点阵自绘生成，"
             "不依赖系统字体，5 分钟内有效且只能使用一次，点击图片可换一张。"),
            ("4.2.2 用户登录界面", "图4.2 登录页面",
             "用户输入用户名与密码登录，后端校验通过后签发 JWT 令牌，前端保存在本地并在后续请求中自动携带。"),
            ("4.2.3 登录结果", "图4.3 登录成功后的首页",
             "登录成功后跳转首页，右上角显示当前用户名，下拉菜单提供我的订单、收货地址、个人中心、"
             "客服助手与退出登录。"),
        ]),
        ("4.3 系统菜单与首页", [
            ("4.3.1 首页布局", "图4.4 首页（轮播图与热藏榜）",
             "首页顶部按 3:1 展示轮播图与热藏榜：左侧轮播图保持 16:9 比例自动切换，"
             "右侧竖排展示热藏榜前 6 名并给出推荐理由。"),
            ("4.3.2 用户菜单", "图4.5 用户下拉菜单",
             "用户菜单集中了订单、地址、个人中心、客服助手与退出登录入口，客服助手以抽屉形式打开。"),
        ]),
        ("4.4 商品浏览与检索", [
            ("4.4.1 商品列表", "图4.6 商品列表页面",
             "列表支持关键词搜索、分类筛选、排序与分页。提交筛选条件时顶部轮播区会自动收起，"
             "让商品直接进入视野。"),
            ("4.4.2 商品详情", "图4.7 商品详情与相似藏品",
             "详情页展示商品图片、价格、库存与描述，底部按标签相似度推荐相关藏品。"),
        ]),
        ("4.5 购物车与订单", [
            ("4.5.1 购物车", "图4.8 购物车页面",
             "购物车展示商品、单价、数量与小计，支持修改数量与删除，勾选后显示合计金额。"),
            ("4.5.2 订单结算", "图4.9 订单结算页面",
             "结算页可选择已保存的收货地址或重新填写，提交后生成订单并扣减库存。"),
            ("4.5.3 我的订单", "图4.10 我的订单页面",
             "订单列表按状态筛选，待发货可取消、已发货可确认收货；管理员发货后出现未读红点。"),
        ]),
        ("4.6 个性化推荐", [
            ("4.6.1 为你推荐", "图4.11 为你推荐区域",
             "首页底部展示为你推荐结果，每条都带有推荐理由，说明是基于浏览还是基于同类商品。"),
            ("4.6.2 热藏榜", "图4.12 热藏榜",
             "热藏榜按近 7 天加权热度排序，权重为下单 3、加购 2、浏览 1，游客也可查看。"),
        ]),
        ("4.7 客服助手", [
            ("4.7.1 知识库命中", "图4.13 客服助手对话界面",
             "抽屉中提供推荐问题与输入框，命中知识库时即时返回预置答案，并标注回答来源。"),
            ("4.7.2 大模型回答", "图4.14 未命中转大模型回答",
             "知识库未覆盖的问题转由大模型回答，回答内容受提示词约束，不编造订单与物流信息。"),
        ]),
        ("4.8 后台管理", [
            ("4.8.1 数据看板", "图4.15 数据看板",
             "看板展示用户数、商品数、订单数、销售额等统计卡片，以及订单状态分布与近 7 天订单趋势。"),
            ("4.8.2 商品管理", "图4.16 商品管理页面",
             "支持商品新增、编辑、删除与上下架，价格与库存均有正数校验。"),
            ("4.8.3 订单管理", "图4.17 订单管理页面",
             "管理员可对待发货订单执行发货，对已发货订单执行强制完成，并可查看订单明细。"),
            ("4.8.4 用户管理", "图4.18 用户管理页面",
             "支持按用户名或邮箱搜索，可禁用/启用账号、重置密码为 123456；禁用后该用户令牌立即失效。"),
            ("4.8.5 轮播图管理", "图4.19 轮播图管理页面",
             "可新增、编辑、删除轮播图，调整排序与上下架状态，前台按排序值展示上架图片。"),
            ("4.8.6 用户画像", "图4.20 用户画像页面",
             "展示用户的标签画像与分类偏好，便于运营了解用户兴趣分布。"),
        ]),
        ("4.9 退出系统", [
            ("4.9.1 退出登录", "图4.21 退出登录",
             "点击退出登录后清除本地令牌并跳转登录页，未重新登录无法访问需要鉴权的页面。"),
        ]),
    ]
    for chapter_title, items in impl:
        heading(doc, chapter_title, 2)
        for sub_title, fig, text in items:
            heading(doc, sub_title, 3)
            para(doc, text)
            figure(doc, fig)

    # ---------- 8. 第5章 ----------
    heading(doc, "第5章 项目总结", 1)
    summaries = [
        ("5.1 项目组长：李云天",
         "本人负责系统总体架构、认证与权限底座、推荐算法核心，并统筹报告与答辩材料。"
         "架构上确定了前后端分离与 Docker 部署方案，搭建 Controller/Service/Mapper 分层结构，"
         "统一了接口返回体与全局异常处理，避免各模块各写一套。认证方面完成 JWT 签发与拦截、"
         "BCrypt 密码存储；在测试中发现「用户被禁用后旧令牌仍可继续使用」的问题，"
         "为此引入令牌版本号，改密或禁用时版本自增，拦截器逐次比对，使旧令牌立即失效。"
         "推荐方面负责内容过滤、协同过滤与混合加权，实现 0.6/0.3/0.1 的三路融合与三层冷启动策略，"
         "并为每条推荐生成可解释的理由。不足之处在于推荐采用定时任务近线计算，"
         "结果最长约有一小时延迟；离线评估所用的行为数据为模拟数据，尚缺真实流量验证。"
         "后续可引入实时流式计算替代定时任务，并结合商品图像的多模态特征改善冷启动效果。"),
        ("5.2 项目组员：潘子皓",
         "本人负责交易主链路，包括商品浏览与检索、购物车、收货地址、下单结算以及订单状态流转。"
         "实现过程中最关键的是库存一致性：最初的写法是先查询库存再扣减，"
         "在并发测试时发现同一商品可能被重复卖出；改为条件更新，"
         "仅当库存充足时更新才会成功，影响行数为零即判定库存不足并回滚事务，"
         "取消订单的库存回补也改为单条原子更新，避免并发下的丢失更新。"
         "此外在订单明细中保存商品名称与成交价快照，保证商品后续改价不影响历史订单；"
         "收货手机号做了前后端一致的格式校验。不足之处在于取消订单没有时效限制，"
         "订单号采用时间戳加随机数，理论上存在极小概率的重复；"
         "后续计划改用雪花算法生成订单号，并增加发货超时自动提醒功能。"),
        ("5.3 项目组员：张程",
         "本人负责管理后台的六个功能页面与前端公共组件。后台部分完成了数据看板、商品管理、"
         "订单管理、用户管理、轮播图管理与用户画像查看，其中看板需要汇总用户、商品、订单与销售额等"
         "多项统计并绘制趋势图，订单管理需要按状态筛选并支持发货与强制完成。"
         "前端公共部分完成了路由与权限守卫、Axios 请求拦截、顶部导航与用户下拉菜单；"
         "权限上做了双重校验，普通用户即使直接输入后台地址也会被路由守卫拦截，后端接口同样返回无权限。"
         "过程中遇到的主要问题是页面缓存导致数据不刷新，"
         "通过统一刷新策略与静态资源版本化得以解决。不足在于后台只实现了必要的筛选，"
         "缺少批量操作、数据导出与操作日志；后续可补充批量上下架、订单导出与审计日志功能。"),
        ("5.4 项目组员：李兴南",
         "本人负责推荐模块的工程实现与效果评估，包括行为埋点、用户画像构建、热门榜计算、"
         "大模型商品标签抽取以及推荐结果在前端的接入。埋点覆盖浏览、加购与下单三类行为，"
         "游客也可匿名上报；为避免影响主流程响应，埋点采用异步线程池写入数据库。"
         "画像部分按时间衰减对行为加权，输出标签画像与分类偏好向量；"
         "标签抽取借助大模型把商品描述转成结构化标签，未配置密钥或调用失败时自动降级为规则匹配，"
         "保证推荐功能不因外部依赖而中断。评估部分编写了离线脚本，"
         "按时间切分数据集比较热门、内容过滤、协同过滤与混合四种策略，"
         "混合策略在 Precision@5 上明显优于热门基线。不足之处在于商品池较小导致共现矩阵稀疏，"
         "协同过滤单独使用时效果有限，且实验基于模拟数据；后续需扩大商品规模并接入真实行为数据。"),
        ("5.5 项目组员：邱俊霖",
         "本人负责注册登录、客服助手、测试体系与部署。注册环节实现了图形验证码，"
         "采用 5×7 点阵自绘，避免容器环境缺少字体导致生成失败，验证码 5 分钟过期且一次性使用。"
         "客服助手按「知识库优先、大模型兜底」实现：先对问题做归一化，"
         "用关键词特征度与二元组相似度打分，达到阈值直接返回预置答案；未命中时再请求大模型，"
         "并在此之前做每日次数限制，失败则返回兜底话术；未命中的问题写入数据库，便于后续补充知识库。"
         "测试方面搭建了单元测试与接口测试，合计 138 个自动化测试方法。"
         "部署方面使用 Docker Compose 编排前后端与数据库，过程中遇到 Nginx 启动时解析后端地址失败"
         "导致容器退出的问题，改为延迟解析后解决。不足在于知识库仅 24 条，覆盖面有限，"
         "且没有转人工入口；后续计划扩充知识库并增加工单转接功能。"),
    ]
    for title, text in summaries:
        heading(doc, title, 2)
        para(doc, text)

    # ---------- 9. 参考文献 ----------
    heading(doc, "参考文献", 1, new_page=True)
    refs = [
        "[1] 王福强. Spring Boot 揭秘：快速构建微服务体系[M]. 北京：机械工业出版社，2016.",
        "[2] 李兴华. MyBatis 从入门到精通[M]. 北京：清华大学出版社，2019.",
        "[3] 尤雨溪. Vue.js 官方文档[EB/OL]. https://cn.vuejs.org/，2024.",
        "[4] Spring 官方文档. Spring Boot Reference Documentation[EB/OL]. "
        "https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/，2023.",
        "[5] 施瓦茨. 高性能 MySQL（第 4 版）[M]. 北京：电子工业出版社，2022.",
        "[6] 项亮. 推荐系统实践[M]. 北京：人民邮电出版社，2012.",
        "[7] 李航. 统计学习方法（第 2 版）[M]. 北京：清华大学出版社，2019.",
        "[8] 张海藩. 软件工程导论（第 6 版）[M]. 北京：清华大学出版社，2013.",
        "[9] 周志明. 深入理解 Java 虚拟机（第 3 版）[M]. 北京：机械工业出版社，2020.",
        "[10] Docker 官方文档. Docker Compose 文档[EB/OL]. https://docs.docker.com/compose/，2024.",
    ]
    for r in refs:
        p = doc.add_paragraph(style="Normal")
        run = p.add_run(r)
        run.font.size = Pt(10.5)
        p.paragraph_format.first_line_indent = Pt(0)

    # ---------- 10. 附录 ----------
    heading(doc, "附录", 1, new_page=True)
    para(doc, "附系统主要功能模块代码（节选）。", indent=True)

    para(doc, "1、订单结算与库存扣减（OrderServiceImpl.java 节选）：", indent=False)
    code_block(doc, """
// 校验库存并计算总额
BigDecimal totalAmount = BigDecimal.ZERO;
for (CartItem item : cartItems) {
    Product product = productMapper.selectById(item.getProductId());
    if (product == null || product.getStatus() == 0) {
        throw new BusinessException("商品「" + item.getProductName() + "」不存在或已下架");
    }
    if (item.getQuantity() > product.getStock()) {
        throw new BusinessException("商品「" + product.getName() + "」库存不足");
    }
    totalAmount = totalAmount.add(product.getPrice()
            .multiply(BigDecimal.valueOf(item.getQuantity())));
}

// 扣减库存：使用条件更新，仅当库存充足时才会成功，避免并发超卖
int affected = productMapper.deductStock(product.getId(), item.getQuantity());
if (affected == 0) {
    throw new BusinessException("商品「" + product.getName() + "」库存不足");
}
""")

    para(doc, "2、混合推荐加权融合（RecommendService.java 节选）：", indent=False)
    code_block(doc, """
double content = contentScore.getOrDefault(product.getId(), 0.0);
double hot = hotScore.getOrDefault(product.getId(), 0.0);
double cf = cfScore.getOrDefault(product.getId(), 0.0);

double score;
String source;
if (coldStart) {
    score = hot;                       // 冷启动：直接按热度兜底
    source = "HOT";
} else {
    score = properties.getHybridAlpha() * content
            + properties.getHybridBeta() * hot
            + properties.getHybridGamma() * cf;   // 0.6 / 0.3 / 0.1
    source = "MIX";
}
ranked.add(new HashMap.SimpleEntry<>(product, score));
""")

    para(doc, "3、客服助手知识库匹配打分（AssistantService.java 节选）：", indent=False)
    code_block(doc, """
private double score(String question, FaqEntry entry) {
    double specific = 0.0;      // 特征词（长词，或只属于一条 FAQ 的短词）
    int genericHits = 0;        // 多条 FAQ 共用的通用词
    for (String keyword : entry.keywords) {
        if (!question.contains(keyword)) {
            continue;
        }
        boolean isSpecific = keyword.length() >= 3
                || keywordDocFreq.getOrDefault(keyword, 1) <= 1;
        if (isSpecific) {
            double weight = keyword.length() >= 4 ? 1.0
                    : keyword.length() == 3 ? 0.9 : 0.75;
            specific = Math.max(specific, weight);
        } else {
            genericHits++;
        }
    }
    double jaccard = bigramJaccard(question, entry.normalizedQuestion);
    double keywordScore;
    if (specific > 0) {
        keywordScore = specific;
    } else if (genericHits >= 2) {
        keywordScore = 0.8;
    } else if (genericHits == 1) {
        keywordScore = 0.45 * Math.min(1.0, jaccard / 0.15);
    } else {
        keywordScore = 0.0;
    }
    return Math.max(keywordScore, jaccard);
}
""")

    doc.save(DST)
    print("已生成:", DST)
    print("段落:", len(doc.paragraphs), " 表格:", len(doc.tables))


if __name__ == "__main__":
    main()
