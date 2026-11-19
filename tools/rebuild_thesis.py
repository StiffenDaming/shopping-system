#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
论文改造脚本：零号藏馆商城系统 → 收藏品商城系统
并新增推荐模块相关章节、表格与配图。

原文件保持不动，输出到 基于Spring Boot的收藏品商城系统.docx
"""

import os
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn

BASE = r"\\wsl.localhost\Ubuntu\home\emperor\Project\shopping-system"
# SRC 是"模板底稿"，务必保留：脚本每次都从它重新生成论文，
# 并顺带把正文中的旧品牌名「零号藏馆」批量替换为「收藏品商城」。
# 注意：不要把 SRC 指向 DST（生成结果），否则每次重新生成都会重复套用改动。
SRC = os.path.join(BASE, "原始论文底稿.docx")
DST = os.path.join(BASE, "基于Spring Boot的收藏品商城系统.docx")
FIG = os.path.join(BASE, "docs", "figures")

CENTER = WD_ALIGN_PARAGRAPH.CENTER


# ---------------------------------------------------------------------------
# 基础工具
# ---------------------------------------------------------------------------
def set_para_text(p, text):
    """整段替换文字，保留第一个 run 的格式。"""
    runs = p.runs
    if not runs:
        p.add_run(text)
        return
    for r in runs[1:]:
        r._element.getparent().remove(r._element)
    runs[0].text = text


def replace_in_paragraph(p, mapping):
    """按最长优先替换段落文字；含图片的段落跳过。"""
    if p._p.xpath(".//w:drawing"):
        return False
    text = p.text
    if not text:
        return False
    new = text
    for key in sorted(mapping, key=len, reverse=True):
        new = new.replace(key, mapping[key])
    if new == text:
        return False
    set_para_text(p, new)
    return True


def replace_everywhere(doc, mapping):
    count = 0
    for p in doc.paragraphs:
        if replace_in_paragraph(p, mapping):
            count += 1
    for table in doc.tables:
        for row in table.rows:
            for cell in row.cells:
                for p in cell.paragraphs:
                    if replace_in_paragraph(p, mapping):
                        count += 1
    for section in doc.sections:
        for part in (section.header, section.footer):
            for p in part.paragraphs:
                if replace_in_paragraph(p, mapping):
                    count += 1
    return count


def find_para(doc, prefix):
    """按前缀查找段落（标题形如 '第三章  系统需求分析'）。"""
    for p in doc.paragraphs:
        if (p.text or "").strip().startswith(prefix):
            return p
    raise KeyError("找不到段落: " + prefix)


def style_exists(doc, name):
    try:
        doc.styles[name]
        return True
    except KeyError:
        return False


def insert_table_before(anchor, headers, rows, doc):
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    try:
        table.style = doc.styles["Table Grid"]
    except KeyError:
        pass
    table.alignment = 1  # center
    for j, h in enumerate(headers):
        cell = table.rows[0].cells[j]
        cell.text = ""
        run = cell.paragraphs[0].add_run(h)
        run.bold = True
        run.font.size = Pt(9)
        cell.paragraphs[0].alignment = CENTER
    for i, row in enumerate(rows):
        for j, val in enumerate(row):
            cell = table.rows[i + 1].cells[j]
            cell.text = ""
            run = cell.paragraphs[0].add_run(str(val))
            run.font.size = Pt(9)
    anchor._p.addprevious(table._tbl)
    return table


def insert_blocks(anchor, blocks, doc, caption_style="图"):
    for block in blocks:
        kind = block[0]
        if kind == "h1":
            anchor.insert_paragraph_before(block[1], style="Heading 1")
        elif kind == "h2":
            anchor.insert_paragraph_before(block[1], style="Heading 2")
        elif kind == "h3":
            anchor.insert_paragraph_before(block[1], style="Heading 3")
        elif kind == "h4":
            anchor.insert_paragraph_before(block[1], style="Heading 4")
        elif kind == "p":
            anchor.insert_paragraph_before(block[1], style="Normal")
        elif kind == "img":
            path, caption = block[1], block[2]
            pic_para = anchor.insert_paragraph_before()
            pic_para.alignment = CENTER
            pic_para.add_run().add_picture(path, width=Inches(6.0))
            cap = anchor.insert_paragraph_before(caption, style=caption_style)
            cap.alignment = CENTER
        elif kind == "table":
            headers, rows, caption = block[1], block[2], block[3]
            if caption:
                cap = anchor.insert_paragraph_before(caption, style="Normal")
                cap.alignment = CENTER
            insert_table_before(anchor, headers, rows, doc)
        elif kind == "caption":
            cap = anchor.insert_paragraph_before(block[1], style=caption_style)
            cap.alignment = CENTER
        elif kind == "placeholder":
            p = anchor.insert_paragraph_before(block[1], style="Normal")
            p.alignment = CENTER
            for r in p.runs:
                r.font.color.rgb = RGBColor(0x99, 0x99, 0x99)
                r.font.size = Pt(9)


def img(name):
    return os.path.join(FIG, name)


# ---------------------------------------------------------------------------
# 新增内容
# ---------------------------------------------------------------------------
BLOCKS_1_3 = [
    ("p", "（4）设计并实现了个性化推荐模块。针对收藏品品类丰富、用户选择困难的问题，"
          "系统引入用户行为采集、用户画像构建与混合推荐算法，并借助大语言模型完成商品特征的"
          "结构化抽取，使系统在基础交易功能之外具备了一定的智能化服务能力。"),
]

BLOCKS_CH2 = [
    ("h2", "2.5  推荐算法相关技术"),
    ("p", "推荐系统的核心任务是根据用户的历史行为与物品特征，从大量候选中筛选出用户最可能感兴趣的物品。"
          "按照信息来源的不同，主流方法可分为基于内容的过滤、协同过滤以及二者的混合形式。"),
    ("h3", "2.5.1  基于内容的过滤与余弦相似度"),
    ("p", "基于内容的过滤（Content-Based Filtering）以物品自身的属性特征为出发点。它把物品表示为一个特征向量，"
          "同时依据用户历史偏好构建同维度的兴趣向量，再通过向量相似度衡量用户与物品的匹配程度。"
          "余弦相似度是最常用的度量方式，其计算如式（2-1）所示。"),
    ("p", "cos(u, p) = (V_u · V_p) / (|V_u| × |V_p|)                                   （2-1）"),
    ("p", "其中 V_u 表示用户偏好向量，V_p 表示物品特征向量，取值为两个向量夹角的余弦值，范围在 0 到 1 之间，"
          "值越大表示匹配度越高。该方法的优点是不依赖其他用户的数据，不存在冷启动中的\u201c新物品\u201d问题，"
          "推荐结果具有良好的可解释性；不足之处在于特征工程依赖人工或外部工具，且容易产生推荐结果同质化。"),
    ("h3", "2.5.2  协同过滤与共现矩阵"),
    ("p", "协同过滤（Collaborative Filtering）不关心物品的具体属性，而是利用群体行为中的相似性进行推荐，"
          "分为基于用户和基于物品两种形式。基于物品的协同过滤（Item-CF）先统计物品在订单、购物车等场景中的"
          "共同出现次数，构建物品共现矩阵，再据此计算物品之间的相似度，其计算如式（2-2）所示。"),
    ("p", "sim(i, j) = co(i, j) / √(deg(i) × deg(j))                                  （2-2）"),
    ("p", "其中 co(i, j) 表示物品 i 与物品 j 的共同出现次数，deg 表示物品的共现总次数。"
          "该方法能够挖掘出人工难以设计出的隐性关联，但对数据规模较为敏感：当物品数量少、交互稀疏时，"
          "共现矩阵大量为空，推荐效果会明显下降，同时新用户与新物品面临冷启动问题。"),
    ("h3", "2.5.3  混合推荐与冷启动策略"),
    ("p", "单一算法往往难以兼顾准确性与覆盖面，因此工程上普遍采用混合推荐。常见的混合方式包括加权融合、"
          "场景切换与级联组合。加权融合将不同召回策略的得分归一化后按权重相加，实现简单且便于调参，"
          "本文采用的即是这一方式。针对冷启动问题，通常引入热门度作为兜底：当用户尚无行为数据时，"
          "直接返回平台当前热度最高的物品，保证推荐位始终有内容可展示。"),
    ("p", ""),
    ("h2", "2.6  大语言模型 API 与提示工程"),
    ("h3", "2.6.1  大模型 API 的调用方式"),
    ("p", "大语言模型通常以 HTTP 接口的形式对外提供服务，请求体采用 JSON 格式，包含模型名称、消息列表、"
          "温度等参数，其中消息列表由系统提示词与用户提示词两部分组成。服务端返回同样为 JSON 结构，"
          "模型的输出文本位于 choices 字段中。调用方只需在请求头中携带身份凭证即可完成一次对话补全，"
          "无需自行部署模型，因而适合在中小型系统中按需引入。"),
    ("h3", "2.6.2  结构化信息抽取的提示设计"),
    ("p", "让大模型输出稳定的结构化数据，是提示工程的关键。实践中需要做到三点：一是通过系统提示词限定模型"
          "角色与输出格式，明确\u201c只输出 JSON、不输出解释文字\u201d；二是提供字段枚举与示例，"
          "约束取值的合法范围；三是将温度参数调低，减少输出的随机性。此外，部分平台支持 "
          "response_format 参数直接指定 JSON 模式，可进一步降低解析失败的概率。"),
]

BLOCKS_3_2 = [
    ("p", "6）个性化推荐功能：系统应能够记录用户的浏览、加购与下单行为，据此构建用户画像，"
          "并在首页与详情页提供热门藏品榜、猜你喜欢、为你推荐等推荐内容；未登录用户也应能够看到热门榜。"),
]

BLOCKS_3_3 = [
    ("p", "6）推荐响应性能需求：推荐接口应采用预计算方式，避免在请求链路中执行复杂统计，"
          "单次响应时间应控制在 200 毫秒以内，保证页面加载流畅。"),
    ("p", "7）推荐结果可解释性需求：推荐内容应向用户展示推荐理由，如\u201c根据您对某类藏品的关注\u201d，"
          "使推荐结果易于理解，提升用户信任度。"),
]

BLOCKS_4_3 = [
    ("p", "在上述 7 张业务表的基础上，为支撑用户画像与推荐算法，系统新增 4 张推荐相关数据表，"
          "分别记录用户行为、商品标签、用户画像与商品热度。"),
    ("table",
     ["字段名", "类型", "长度", "必填", "字段说明"],
     [
         ["id", "BIGINT", "20", "是", "行为ID，主键自增"],
         ["user_id", "BIGINT", "20", "是", "用户ID，0 表示游客"],
         ["product_id", "BIGINT", "20", "否", "商品ID，搜索行为可为空"],
         ["behavior_type", "VARCHAR", "20", "是", "VIEW / CART / ORDER / SEARCH"],
         ["weight", "DECIMAL", "4,2", "是", "行为基础权重"],
         ["stay_ms", "INT", "11", "否", "页面停留时长（毫秒）"],
         ["keyword", "VARCHAR", "100", "否", "搜索关键词"],
         ["scene", "VARCHAR", "50", "否", "来源场景 LIST / DETAIL / CART"],
         ["create_time", "DATETIME", "-", "是", "行为发生时间"],
     ],
     "表4-8  用户行为日志表（user_behavior）"),
    ("table",
     ["字段名", "类型", "长度", "必填", "字段说明"],
     [
         ["id", "BIGINT", "20", "是", "标签ID，主键自增"],
         ["product_id", "BIGINT", "20", "是", "商品ID"],
         ["tag_name", "VARCHAR", "50", "是", "标签名称"],
         ["tag_type", "VARCHAR", "20", "是", "SUBJECT / MATERIAL / STYLE / ERA"],
         ["weight", "DECIMAL", "4,2", "是", "标签权重"],
         ["source", "VARCHAR", "20", "是", "来源 LLM / RULE / MANUAL"],
         ["create_time", "DATETIME", "-", "是", "创建时间"],
     ],
     "表4-9  商品标签表（product_tag）"),
    ("table",
     ["字段名", "类型", "长度", "必填", "字段说明"],
     [
         ["id", "BIGINT", "20", "是", "画像ID，主键自增"],
         ["user_id", "BIGINT", "20", "是", "用户ID"],
         ["tag_vector", "TEXT", "-", "否", "标签偏好向量（JSON）"],
         ["category_vector", "TEXT", "-", "否", "分类偏好向量（JSON）"],
         ["price_pref", "DECIMAL", "10,2", "否", "价格偏好（加权均价）"],
         ["activity_level", "VARCHAR", "20", "是", "活跃度 HIGH / MID / LOW"],
         ["view_cnt", "INT", "11", "否", "累计浏览次数"],
         ["cart_cnt", "INT", "11", "否", "累计加购次数"],
         ["order_cnt", "INT", "11", "否", "累计下单次数"],
         ["update_time", "DATETIME", "-", "是", "更新时间"],
     ],
     "表4-10  用户画像表（user_profile）"),
    ("table",
     ["字段名", "类型", "长度", "必填", "字段说明"],
     [
         ["id", "BIGINT", "20", "是", "热度ID，主键自增"],
         ["product_id", "BIGINT", "20", "是", "商品ID"],
         ["hot_score", "DECIMAL", "12,4", "是", "热度分"],
         ["view_cnt", "INT", "11", "否", "近7天浏览数"],
         ["cart_cnt", "INT", "11", "否", "近7天加购数"],
         ["order_cnt", "INT", "11", "否", "近7天下单数"],
         ["stat_date", "DATE", "-", "是", "统计日期"],
     ],
     "表4-11  商品热度表（product_hot）"),
]

BLOCKS_4_4 = [
    ("h3", "4.4.7  用户行为采集流程设计"),
    ("p", "用户行为采集是推荐模块的数据入口。用户在页面上的浏览、加购、搜索等操作由前端埋点捕获，"
          "统一封装为事件对象后通过接口上报至后端。为保证不影响主流程，后端接收请求后仅做参数校验，"
          "随后将写入任务提交至独立线程池异步执行，接口立即返回。行为数据最终落入 user_behavior 表，"
          "成为后续画像与热度计算的原始素材。该流程的关键设计是\u201c埋点与业务解耦\u201d："
          "即使埋点写入失败，也不会影响用户加购、下单等核心操作。"),
    ("p", "游客的行为同样会被记录，此时 user_id 记为 0。由于埋点接口位于认证拦截器的白名单中，"
          "后端无法直接从上下文获取登录用户，因此需要自行解析请求头中的 JWT 令牌：解析成功则记录真实用户，"
          "解析失败则按游客处理，不阻断请求。"),
    ("h3", "4.4.8  推荐生成流程设计"),
    ("p", "推荐生成采用\u201c离线计算、在线读取\u201d的双链路设计。离线链路由定时任务驱动："
          "整点重新统计商品热度并写入 product_hot 表，每小时第 20 分钟重新聚合用户画像并写入 user_profile 表，"
          "两次任务都会先刷新商品目录缓存。在线链路即推荐接口本身，只读取上述两张结果表以及商品标签，"
          "在内存中完成相似度计算与加权排序，因此响应时间稳定在毫秒级。"),
    ("p", "这一设计把耗时的统计聚合从请求链路中剥离出去，避免了每次推荐都执行全表扫描。"
          "其代价是推荐结果存在一定的延迟（最长约一小时），属于近实时而非严格实时，"
          "对于收藏品这类低频浏览、长决策周期的消费场景是可以接受的。"),
]

BLOCKS_4_5 = [
    ("h2", "4.5  推荐算法设计"),
    ("p", "推荐模块在原有交易系统之上独立演进，整体遵循\u201c数据采集—画像构建—多路召回—加权排序—结果展示\u201d"
          "的分层思路。系统总体架构如图 4-14 所示，推荐模块与业务模块共用数据访问层，并通过离线方式调用"
          "大语言模型完成商品标签抽取。"),
    ("img", img("fig-4-1-architecture.png"), "图4-14  系统总体架构（含推荐模块）"),

    ("h3", "4.5.1  推荐系统整体架构"),
    ("p", "推荐系统自上而下划分为五层，如图 4-15 所示。数据层负责汇聚用户行为、商品标签与订单明细；"
          "画像层对行为数据按时间衰减进行聚合，形成用户兴趣向量；召回层通过热度、内容与协同三条路径"
          "各自产生候选集；排序层对三路得分归一化后加权融合，并处理冷启动；展示层将结果填充到"
          "热藏榜、猜你喜欢与为你推荐三个位置。用户的点击与浏览又会产生新的行为数据回流至数据层，"
          "形成闭环。"),
    ("img", img("fig-4-2-rec-pipeline.png"), "图4-15  推荐系统分层架构"),

    ("h3", "4.5.2  用户画像构建"),
    ("p", "用户画像的本质是把离散的行为日志压缩为一组可计算的兴趣权重。系统按式（4-1）对每条行为赋予"
          "随时间衰减的权重。"),
    ("p", "w = w_base × e^(-λt)                                                   （4-1）"),
    ("p", "其中 w_base 为行为基础权重（浏览 1、加购 3、下单 5、搜索 1），t 为行为发生至今天数，"
          "λ 为衰减系数，取 0.05，对应约 14 天的半衰期。也就是说，两周前的浏览行为对当前画像的影响"
          "只剩一半，这样画像能够自动\u201c遗忘\u201d过时的兴趣。"),
    ("p", "随后把每条行为映射到商品的标签与所属分类上，按式（4-2）累加得到标签权重。"),
    ("p", "W(u, c) = Σ w_i    (对用户 u 在标签或分类 c 上的所有行为求和)          （4-2）"),
    ("p", "最后对同一用户的向量按最大值归一化到 [0,1] 区间，保留相对强弱关系，便于后续与其他用户画像对比。"
          "构建流程如图 4-16 所示。"),
    ("img", img("fig-4-3-profile-flow.png"), "图4-16  用户画像构建流程"),
    ("p", "除标签向量外，画像还记录了分类偏好向量、价格偏好（按行为加权的平均价格）与活跃度等级"
          "（行为数大于 30 为高活跃、10 至 30 为中活跃、其余为低活跃），用于运营端的数据展示。"),

    ("h3", "4.5.3  基于内容的过滤推荐"),
    ("p", "内容过滤直接使用 2.5.1 节所述的余弦相似度。商品向量由三部分构成：商品标签（权重 1.0）、"
          "所属分类名（权重 1.5）以及分类伪标签（形如 CAT:3，权重 1.5）。分类权重高于普通标签，"
          "是因为同一分类下的藏品在题材与受众上最为接近。用户向量则取画像中的标签向量与分类向量合并。"
          "两者按式（4-3）计算相似度，作为内容得分。"),
    ("p", "score_content(u, p) = cos(V_u, V_p)                                  （4-3）"),
    ("p", "该方法不依赖其他用户的数据，因此对任何有少量行为的用户都能立即生效，"
          "同时也天然解决了新增藏品无从推荐的难题。"),

    ("h3", "4.5.4  基于物品的协同过滤"),
    ("p", "协同过滤的输入是物品共现关系。系统从三个来源构建共现组：真实订单中的商品组合、"
          "同一用户购物车内的商品组合，以及模拟订单会话中的商品组合。任一组内任意两件商品之间"
          "计一次共现，由此得到稀疏的交互矩阵，如图 4-17 所示。"),
    ("img", img("fig-4-4-cooccur.png"), "图4-17  物品共现矩阵与相似度计算"),
    ("p", "物品相似度按式（2-2）计算后，对目标用户已交互过的每件物品，取其在候选集上的相似度并乘以该物品的"
          "衰减行为权重，累加得到协同得分，如式（4-4）所示。"),
    ("p", "score_cf(u, p) = Σ sim(i, p) × w(u, i)     (i 属于用户 u 已交互物品)    （4-4）"),
    ("p", "考虑到本系统商品数量有限，共现矩阵较为稀疏，协同过滤在本文中只作为混合排序的补充项，"
          "承担对内容得分的微调作用。"),

    ("h3", "4.5.5  混合推荐与冷启动策略"),
    ("p", "三路得分量纲不同，先各自归一化到 [0,1]，再按式（4-5）加权融合。"),
    ("p", "score = α × score_content + β × hot_norm + γ × score_cf              （4-5）"),
    ("p", "默认取 α = 0.6、β = 0.3、γ = 0.1。内容项权重最高，因为它最能反映用户的个体偏好；"
          "热度项次之，用于在内容得分接近时区分优先级，并在用户行为稀少时提供兜底；"
          "协同项权重最低，仅作微调。融合过程如图 4-18 所示。"),
    ("img", img("fig-4-5-hybrid.png"), "图4-18  混合推荐加权融合"),
    ("p", "针对冷启动，系统设置了三层兜底策略：其一，未登录用户或画像为空的用户，令 α = 0、γ = 0、"
          "β = 1，直接退化为热门榜；其二，当所有候选的内容得分都为 0 时，用热度结果补齐空缺位置，"
          "并将补齐项的得分减半，使其排在内容命中项之后；其三，若热度表尚无数据（系统刚部署），"
          "则按上架时间倒序返回。三层策略保证推荐位在任何情况下都不会出现空白。"),

    ("h3", "4.5.6  商品热度计算模型"),
    ("p", "热度并非简单按浏览量排序，而是对多类交互加权，如式（4-6）所示。"),
    ("p", "hot(p) = 3 × N_order(p) + 2 × N_cart(p) + 1 × N_view(p)                （4-6）"),
    ("p", "其中三项分别统计近 7 天内的下单件数、加购次数与浏览次数。之所以提高下单权重，是因为购买行为"
          "的意图强度远高于浏览；而只统计近 7 天窗口，则使热度天然具备时间衰减特性，能够及时反映"
          "\u201c正在升温\u201d的藏品，而不是长期霸榜的老商品。"),
]

BLOCKS_5_3 = [
    ("h2", "5.3  智能推荐模块实现"),
    ("h3", "5.3.1  行为埋点与数据采集实现"),
    ("p", "前端封装了统一的埋点工具 track.js，对外提供 trackView、trackCart、trackSearch 三个方法。"
          "埋点请求失败时静默处理，不弹出任何提示，避免干扰用户操作。浏览行为在用户离开详情页时上报，"
          "并携带停留时长；页面切换通过路由监听处理，关闭标签页则借助 pagehide 事件兜底，"
          "保证行为不丢失。完整链路如图 5-13 所示。"),
    ("img", img("fig-5-1-track-flow.png"), "图5-13  用户行为采集与推荐更新流程"),
    ("p", "后端 BehaviorController 暴露 POST /api/behavior/track 接口。该接口位于认证拦截器白名单内，"
          "因此由 TokenResolver 组件自行解析请求头中的 JWT：解析成功记录真实用户 ID，否则记 0。"
          "写入动作通过 @Async 注解提交到名为 behaviorExecutor 的独立线程池，核心线程 2、最大线程 4、"
          "队列容量 2000，队列满时丢弃最旧任务。这一取舍是合理的：埋点属于旁路数据，宁可丢弃，"
          "也不能反压主业务。"),

    ("h3", "5.3.2  用户画像构建实现"),
    ("p", "画像构建由 ProfileService 完成。它先按用户维度查询最近 90 天的行为记录，逐条计算衰减权重，"
          "映射到商品标签与分类上累加，再归一化并写入 user_profile 表。为使计算不必反复查库，"
          "系统设计了 CatalogCache 组件，把\u201c商品到分类\u201d\u201c商品到标签\u201d等映射整体载入内存，"
          "有效期为 5 分钟，过期后自动重新加载。"),
    ("p", "画像结果以 JSON 字符串形式存储，例如 {\"文玩雅器\": 1.0, \"紫水晶\": 0.62}。"
          "选择 JSON 而非独立的关系表，是因为画像本质上是一个不定长的稀疏向量，JSON 在读取时"
          "可直接反序列化为映射结构，比多表关联更简洁高效。"),

    ("h3", "5.3.3  热门藏品榜实现"),
    ("p", "热度计算由 HotService 承担，实现上全部交由数据库完成聚合：浏览与加购通过一次 GROUP BY 查询"
          "按商品与行为类型统计，下单数据则由订单明细与订单表关联后按商品汇总。得到三项计数后，"
          "在应用层按式（4-6）计算热度分，并通过 INSERT ... ON DUPLICATE KEY UPDATE 语句"
          "写入当日记录，保证同一商品同一天只有一行。"),
    ("p", "前端在商品列表页顶部渲染热藏榜条，展示前 5 名并给出推荐理由。推荐理由直接由数据生成，"
          "例如\u201c近7天 182 次浏览 · 58 次加购\u201d，比笼统的\u201c热门推荐\u201d更有说服力。"),

    ("h3", "5.3.4  个性化推荐实现"),
    ("p", "RecommendService 是对外提供推荐能力的核心类，其中 hot、similar、forYou 三个方法分别对应"
          "热藏榜、猜你喜欢与为你推荐。三者的公共流程是：先取候选集（过滤下架商品），"
          "再按对应策略打分，最后排序截断并组装为 RecommendItemVO 返回。"),
    ("p", "为避免向用户推荐已经购买过的藏品，在你推荐接口中会先查询该用户的 ORDER 行为，"
          "将其已购商品从候选集中剔除。推荐理由则根据命中来源动态生成：内容命中时给出"
          "\u201c根据您对「某分类」的关注\u201d，协同命中时给出\u201c与您浏览过的藏品常被一起关注\u201d，"
          "热度命中时给出具体的浏览与加购数据。这种可解释的文案显著提升了推荐结果的可信度。"),

    ("h3", "5.3.5  大模型 API 集成与标签抽取实现"),
    ("p", "大模型的引入采取\u201c离线使用\u201d策略，只用于商品标签抽取这一件脏活，不参与在线推荐决策。"
          "具体做法是：由管理员触发抽取任务，后端遍历全部商品，对每件商品拼装提示词并调用 DeepSeek 的"
          "对话补全接口，要求模型以 JSON 返回 3 至 6 个结构化标签，解析后写入 product_tag 表。"
          "由于商品数量有限，整个抽取过程只需执行一次，后续在线推荐全部读库，不再产生 API 调用。"),
    ("p", "实现层面，LlmClient 基于 JDK 自带的 HttpClient 构建，未引入任何第三方 HTTP 依赖。"
          "接口调用设置了 30 秒超时，并在提示词中限定温度参数为 0.2、开启 JSON 输出模式。"
          "当 API Key 未配置或调用失败时，标签抽取自动降级为基于词典的规则匹配，"
          "使系统在没有外网或没有密钥的环境下依然可以正常运行与演示。"),
    ("p", "出于安全考虑，API Key 不写入代码与配置文件，而是通过环境变量 LLM_API_KEY 注入，"
          "Docker 部署时由 docker compose 在运行时传入。这样既避免了密钥泄露，"
          "也便于在不同环境中切换不同的密钥。"),
]

BLOCKS_6_5 = [
    ("p", "Allure 报告的实际展示效果如图 6-1 至图 6-3 所示。"),
    ("caption", "图6-1  Allure 测试报告总览"),
    ("caption", "图6-2  Allure 测试套件视图"),
    ("caption", "图6-3  Allure 单个用例执行详情"),

    ("h3", "6.5.4  推荐模块测试"),
    ("p", "推荐模块是本次新增的智能化功能，其正确性直接影响推荐结果的合理性。针对该模块共补充 25 个单元测试，"
          "分布在五个测试类中。与业务模块测试不同，推荐模块涉及时间衰减、向量相似度、加权排序等计算逻辑，"
          "因此测试重点从\u201c接口是否可用\u201d转向\u201c算法结果是否准确\u201d。所有用例均使用 Mockito 隔离 Mapper "
          "与缓存组件，不依赖数据库，可在任意环境快速执行。"),
    ("table",
     ["测试类", "用例数", "覆盖内容"],
     [
         ["BehaviorServiceTest", "8", "行为类型权重映射、非法输入拦截、游客归零、关键词截断"],
         ["ProfileServiceTest", "6", "空行为处理、时间衰减、向量归一化、活跃度分级、更新而非新增"],
         ["RecommendServiceTest", "5", "相似召回排除自身、冷启动兜底、已购过滤、混合加权、热度补齐"],
         ["HotServiceTest", "3", "热度加权公式、取消订单不计入、无行为时热度为零"],
         ["TagExtractionServiceTest", "3", "未配置 Key 时规则降级、已有标签跳过、强制重新抽取"],
     ],
     "表6-6  推荐模块单元测试覆盖情况"),
    ("p", "上述用例与 6.4 节的黑盒用例互为补充：黑盒用例验证功能是否可用，单元测试验证算法逻辑是否准确。"
          "例如时间衰减用例构造了两条相隔 14 天的行为记录，断言旧标签的权重约为新标签的 0.497 倍，"
          "与 exp(-0.05×14) 的理论值一致，说明画像的时间折扣逻辑实现正确；"
          "热度用例则断言\u201c订单 2 件、加购 4 次、浏览 10 次\u201d时热度分等于 24，验证了加权公式的准确性。"),
    ("p", "执行 mvn test 后，全部 113 个自动化测试通过，其中单元测试 76 个、API 集成测试 37 个，"
          "无失败、无跳过，说明新增的推荐模块与原有业务模块可以协同工作，未引入回归问题。"),
]

BLOCKS_6_7 = [
    ("h2", "6.7  推荐算法效果评估"),
    ("p", "功能测试只能说明推荐接口可用，不能说明推荐结果是否准确。为验证算法的有效性，"
          "本节在真实行为数据的基础上进行离线评估，对比四种策略的推荐质量。"),
    ("h3", "6.7.1  数据集与实验设置"),
    ("p", "系统尚处于开发阶段，真实用户行为数据有限，无法支撑多策略的对比实验。因此本文采用"
          "模拟数据集验证算法有效性，这也是推荐系统研究中常见的做法。模拟数据由脚本按\u201c用户存在"
          "潜在品类偏好\u201d的假设生成：每个用户拥有 1 至 2 个强兴趣品类，行为按浏览 70%、"
          "加购 20%、下单 10% 的比例分布，时间集中在最近 60 天并向近期倾斜。数据生成过程固定随机种子，"
          "保证实验可复现。数据集统计如表 6-7 所示。"),
    ("table",
     ["统计项", "数值"],
     [
         ["商品数量", "19 件（7 个分类）"],
         ["商品标签", "93 条"],
         ["行为记录", "9241 条"],
         ["模拟用户", "200 个"],
         ["训练集 / 测试集", "5606 条 / 3634 条"],
         ["时间切分点", "最近 14 天"],
         ["可评估用户数", "202 个"],
     ],
     "表6-7  实验数据集统计"),
    ("p", "实验采用时间切分而非随机切分：以切分点之前的行为构建画像与相似度，之后的行为作为判断"
          "推荐是否命中的依据。这样可避免用未来信息预测过去，评价结果更接近真实上线效果。"),
    ("h3", "6.7.2  评价指标"),
    ("p", "本文采用以下指标衡量推荐质量：Precision@K 表示推荐的前 K 个物品中命中用户实际感兴趣物品的比例；"
          "Recall@K 表示用户感兴趣的物品被推荐出来的比例；HR@K（命中率）表示至少命中一个物品的用户占比；"
          "NDCG@K 为归一化折损累计增益，命中位置越靠前得分越高，用于衡量排序质量；"
          "覆盖率表示推荐结果覆盖的商品占全部商品的比例；多样性取推荐列表内物品两两相似度的平均值之补，"
          "用于衡量推荐结果的丰富程度。"),
    ("h3", "6.7.3  对比实验与结果"),
    ("p", "实验对比了四种策略：POP（热门基线，按加权热度排序）、CONTENT（基于内容的过滤）、"
          "CF（基于物品的协同过滤）与 HYBRID（本文的混合加权）。取 K = 5 与 K = 10 两组结果，"
          "如表 6-8 与图 6-4 所示。"),
    ("table",
     ["策略", "P@5", "R@5", "HR@5", "NDCG@5", "覆盖率"],
     [
         ["POP", "0.4287", "0.3475", "0.9455", "0.4664", "26.32%"],
         ["CF", "0.6683", "0.5373", "0.9653", "0.7389", "94.74%"],
         ["CONTENT", "0.7208", "0.5923", "0.9901", "0.8055", "100%"],
         ["HYBRID", "0.7307", "0.5960", "0.9901", "0.8129", "100%"],
     ],
     "表6-8  四种推荐策略的评估结果（K = 5）"),
    ("img", img("fig-6-1-eval.png"), "图6-4  推荐算法效果对比"),
    ("p", "从 K = 10 的结果看，混合策略的 Precision 为 0.5149，Recall 为 0.8197，NDCG 为 0.8264，"
          "均优于其余三种策略，且命中率达到 100%，即全部用户都能在推荐列表中找到至少一件感兴趣的藏品。"),
    ("h3", "6.7.4  结果分析与局限性"),
    ("p", "实验结果表明：第一，个性化推荐显著优于热门基线。混合策略在 K = 5 时的 Precision 为 0.7307，"
          "热门基线为 0.4287，相对提升 70.4%；NDCG 从 0.4664 提升至 0.8129，提升 74.3%，"
          "说明用户画像确实捕捉到了品类偏好，而不是简单地\u201c什么火推什么\u201d。"
          "第二，混合策略优于任一单一策略，验证了\u201c内容为主、热度与协同为辅\u201d的权重设计是合理的。"
          "第三，协同过滤单独使用时 Precision 为 0.6683，低于内容过滤的 0.7208，"
          "这与商品数量少、共现矩阵稀疏的客观条件相符。第四，热门基线虽然覆盖率只有 26.32%，"
          "但多样性最高，说明其推荐结果更为分散；个性化策略以牺牲少量多样性换取了明显的相关性提升。"),
    ("p", "需要指出的是，本实验存在两方面局限。其一，商品池仅有 19 件，规模过小，导致各策略之间的差距"
          "被压缩；在补充的严格实验中（从候选集中剔除用户已交互过的商品），由于用户会在训练期就将"
          "偏好品类的商品几乎全部浏览完毕，测试集退化为随机探索行为，各策略的差异不再显著，"
          "热门基线反而占优。其二，数据集为模拟生成，虽然固定了随机种子并模拟了真实电商的漏斗分布，"
          "但终究不能完全替代真实用户行为。待系统上线并积累真实数据后，"
          "可在更大规模的商品池上重新验证本文的结论。"),
]

BLOCKS_7_1 = [
    ("p", "第五，系统在传统电商功能之外引入了个性化推荐能力。通过统一埋点采集用户行为，"
          "按时间衰减构建用户画像，并将基于内容的过滤、基于物品的协同过滤与商品热度进行加权融合，"
          "形成了可解释、可调控的混合推荐模型。同时，系统借助大语言模型完成非结构化商品描述到"
          "结构化标签的转换，为内容过滤提供了特征基础，这一做法在同类毕业设计中较为少见。"),
    ("p", "第六，推荐模块的工程实现兼顾了性能与可用性。热度与画像均由定时任务预先计算，"
          "在线接口仅做读取与内存运算，响应时间稳定在毫秒级；针对冷启动设计了三层兜底策略，"
          "保证推荐位在任何情况下都有内容展示；当大模型不可用时，标签抽取自动降级为规则匹配，"
          "系统整体功能不受影响。"),
]

NEW_385 = ("第四，系统前端界面较为简洁，缺少轮播图、限时活动等营销展示模块，对用户的吸引力有限；"
           "推荐模块目前采用定时任务进行近线计算，最长存在约一小时的延迟，尚未做到实时更新。"
           "第五，系统目前只支持单店铺B2C模式，不支持多商家入驻，功能场景相对单一。"
           "因此，该系统还有继续开发和完善的空间。")

NEW_388 = ("同时可以完善前端页面设计，增加首页轮播图、限时活动等营销模块，提升平台活跃度和用户体验。"
           "在推荐能力方面，可进一步引入实时流式计算替代当前的定时任务，缩短推荐结果的更新延迟；"
           "并结合藏品图像的多模态特征与深度学习排序模型，提升推荐的准确性与丰富度。"
           "长远来看，可以扩展为支持多商家入驻的B2B2C模式，丰富平台生态，"
           "打造更加完善、高效、安全的收藏品商城系统。")

SUMMARY_CN = [
    ("p", "随着互联网技术的快速发展和电子商务的蓬勃兴起，线上收藏品交易已成为收藏爱好者日常交流的重要方式。"
          "传统线下收藏品交易模式受限于时间与空间，藏品展示品类有限、运营成本高昂，"
          "难以满足收藏爱好者日益增长的便捷化交易需求。在此背景下，"
          "构建一套功能完善、操作便捷、运行稳定的收藏品商城系统具有重要的现实意义。"),
    ("p", "本文设计并实现了一款基于前后端分离架构的收藏品商城系统。系统采用 Vue3 + Element Plus 开发前端页面，"
          "使用 Vite 作为构建工具以提升开发效率；后端基于 Spring Boot + MyBatis-Plus 框架搭建业务服务，"
          "以 MySQL 作为数据存储数据库；采用 JWT 实现用户身份认证与权限控制，区分普通用户与管理员双角色权限体系。"
          "系统部署采用 Docker 容器化方案，实现一键启动、环境隔离与便捷运维。"),
    ("p", "系统面向普通用户实现了注册登录、商品浏览、购物车管理、收货地址管理、订单提交与查询、取消订单、"
          "确认收货、未读订单红点提示等前台核心功能；面向管理员实现了商品管理、订单管理、用户管理等后台管控模块。"
          "在基础交易功能之上，系统进一步设计并实现了个性化推荐模块：通过统一埋点采集浏览、加购、下单行为，"
          "按时间衰减构建用户画像；将基于内容的过滤、基于物品的协同过滤与商品热度加权融合为混合推荐模型；"
          "并借助大语言模型 API 对商品描述进行结构化标签抽取，为内容过滤提供特征支持。"),
    ("p", "系统在开发过程中引入了完善的测试体系，包括基于 JUnit5 和 Mockito 的单元测试、基于 MockMvc 的 API "
          "集成测试，并通过 Allure 框架生成可视化测试报告。在推荐算法评估方面，采用时间切分的离线实验，"
          "对比了热门基线、内容过滤、协同过滤与混合推荐四种策略。实验结果显示，混合策略在 K = 5 时的 "
          "Precision 达到 0.7307、NDCG 达到 0.8129，较热门基线分别提升 70.4% 与 74.3%，验证了本文推荐方案的"
          "有效性与合理性。"),
    ("p", "关键词：收藏品商城；前后端分离；Spring Boot；Vue3；用户画像；混合推荐"),
]

SUMMARY_EN = [
    ("p", "With the rapid development of Internet technology and the rise of e-commerce, online trading of "
          "collectibles has become an important way for collectors to communicate. Traditional offline trading "
          "is limited by time and space, with a narrow range of displayed items and high operating costs, "
          "making it difficult to meet the growing demand for convenient transactions. In this context, building "
          "a collectibles mall system with complete functions, convenient operation and stable running is of "
          "practical significance."),
    ("p", "This thesis designs and implements a collectibles mall system based on a front-end and back-end "
          "separation architecture. The front end is developed with Vue3 and Element Plus, using Vite as the build "
          "tool. The back end is built on Spring Boot and MyBatis-Plus, with MySQL as the data storage. JWT is "
          "used for authentication and permission control, distinguishing ordinary users from administrators. "
          "The system is deployed with Docker, enabling one-click startup and environment isolation."),
    ("p", "For ordinary users, the system implements registration and login, product browsing, shopping cart "
          "management, delivery address management, order submission and query, order cancellation, receipt "
          "confirmation and unread order notification. For administrators, it provides product, order and user "
          "management modules. Beyond basic trading functions, a personalized recommendation module is designed "
          "and implemented: user behaviors are collected through unified tracking, and user profiles are built "
          "with time decay. Content-based filtering, item-based collaborative filtering and item popularity are "
          "fused into a hybrid recommendation model. A large language model API is used to extract structured "
          "tags from product descriptions, providing features for content-based filtering."),
    ("p", "A complete testing system is introduced, including unit tests based on JUnit5 and Mockito, API "
          "integration tests based on MockMvc, and visual reports generated by Allure. For recommendation "
          "evaluation, an offline experiment with time-based splitting compares four strategies: popularity "
          "baseline, content-based filtering, collaborative filtering and hybrid recommendation. The results show "
          "that the hybrid strategy achieves a Precision of 0.7307 and an NDCG of 0.8129 at K = 5, which are "
          "70.4% and 74.3% higher than the popularity baseline, verifying the effectiveness of the proposed scheme."),
    ("p", "Keywords: Collectibles Mall; Front-end and Back-end Separation; Spring Boot; Vue3; "
          "User Profile; Hybrid Recommendation"),
]

# 修正论文与实现不一致之处（原文 -> 新文）
TEXT_FIXES = [
    # —— 3.2.1 用户端功能需求 ——
    ("主要实现商品浏览、购物车管理、下单购买、个人信息维护等功能",
     "主要实现商品浏览、购物车管理、下单购买、个人信息查看与密码修改等功能"),
    ("登录后可修改个人信息和密码。系统基于JWT实现身份认证，保障用户账户安全。",
     "注册时需填写图形验证码，以防止脚本批量注册；登录后可在个人中心查看个人信息并修改密码。"
     "系统基于JWT实现身份认证，保障用户账户安全。"),
    # —— 3.2.2 管理员端功能需求：原文写"五大模块"却只列四项 ——
    ("核心需求分为五大模块：商品管理模块、订单管理模块、用户管理模块和数据统计模块。",
     "核心需求分为五个模块：商品管理、订单管理、用户管理、数据统计与轮播图管理。"),
    # —— 4.1 架构：上传功能已删除 ——
    ("数据层采用MySQL关系型数据库存储业务数据，同时支持文件上传存储。",
     "数据层采用MySQL关系型数据库存储业务数据；商品图片托管于对象存储（OSS），后台以 URL 形式录入。"),
    # —— 4.3.2 用户表用途 ——
    ("支撑用户注册、登录、个人信息管理、权限校验等功能。",
     "支撑用户注册、登录、个人信息查看、权限校验等功能。"),
    # —— 4.4.1 注册流程：注册不返回 JWT ——
    ("注册成功后，后端会生成一个JWT令牌并返回给前端。",
     "注册成功后接口仅返回成功提示，不会签发令牌；用户需使用新注册的账号登录，登录时才由后端签发 JWT。"),
    # —— 5.1.1 注册功能：补充验证码 ——
    ("用户进入系统注册页面，填写用户名、密码、邮箱等信息后提交。",
     "用户进入系统注册页面，填写用户名、密码、邮箱并输入图形验证码后提交。"),
    # —— 5.2.1 商品管理：无分类/状态筛选 ——
    ("支持按名称搜索、按分类筛选、按状态过滤。",
     "支持按名称搜索，并可对单个商品执行上架、下架操作。"),
    # —— 5.2.3 用户管理：手机号字段已删除 ——
    ("包含用户ID、用户名、邮箱、手机号、角色、状态等核心数据",
     "包含用户ID、用户名、邮箱、角色、状态等核心数据"),
    ("被禁用的用户无法登录系统，保障平台安全。",
     "被禁用的用户无法登录系统；若该用户正处于登录状态，其令牌会立即失效，需重新登录。"),
    # —— 5.3.3 热藏榜位置与条数 ——
    ("前端在商品列表页顶部渲染热藏榜条，展示前 5 名并给出推荐理由。",
     "前端在首页顶部左侧展示轮播图、右侧竖排展示热藏榜前 6 名并给出推荐理由；"
     "用户点击分类或搜索时该区域自动收起，也可手动展开。"),
    # —— 6.7 数据来源自相矛盾 ——
    ("本节在真实行为数据的基础上进行离线评估", "本节在模拟行为数据集的基础上进行离线评估"),
    # —— 7.2 / 7.3 轮播图已实现 ——
    ("缺少轮播图、限时活动等营销展示模块", "缺少限时活动、优惠券等营销模块"),
    ("增加首页轮播图、限时活动等营销模块", "增加限时活动、优惠券等营销模块"),
    # —— 测试数量（终值：单元 89 + 接口 40 = 129；含黑盒 25 共 154）——
    ("覆盖购物车服务、订单服务、收货地址服务三大核心业务模块",
     "覆盖购物车服务、订单服务、收货地址服务、推荐模块四大核心业务模块"),
    ("共编写单元测试用例51个，其中购物车服务15个、订单服务20个、收货地址服务16个",
     "共编写单元测试用例98个，其中购物车服务15个、订单服务21个、收货地址服务16个、"
     "推荐模块25个、验证码6个、商品服务6个、客服助手9个"),
    ("全部 113 个自动化测试通过，其中单元测试 76 个、API 集成测试 37 个",
     "全部 138 个自动化测试通过，其中单元测试 98 个、API 集成测试 40 个"),
    ("共设计并执行测试用例113个，其中单元测试51个、API集成测试37个、黑盒功能测试25个",
     "共设计并执行测试用例163个，其中单元测试98个、API集成测试40个、黑盒功能测试25个"),
    # —— 摘要补充客服助手 ——
    ("未读订单红点提示等前台核心功能；",
     "未读订单红点提示等前台核心功能，以及基于知识库优先、大模型兜底的客服助手；"),
]

# 3.2.1 追加第 7 条（个人中心）
BLOCKS_3_2_1 = [
    ("p", "7）个人中心功能：登录用户可在个人中心查看用户名、邮箱、角色与注册时间等账号信息，并可修改登录密码；"
          "修改密码需校验原密码，修改成功后当前令牌立即失效，需使用新密码重新登录。"),
]

# 3.2.1 追加第 8 条（客服助手）
BLOCKS_3_2_1_ASSISTANT = [
    ("p", "8）客服助手功能：登录用户可在客服助手中咨询下单、支付、物流、售后、账号等问题。"
          "系统优先从本地知识库匹配答案，命中即返回；未命中时再请求大模型生成回答；"
          "两者都不可用时给出兜底提示，并引导用户联系管理员。"),
]

# 3.2.2 追加第 5 条（轮播图管理）
BLOCKS_3_2_2 = [
    ("p", "5）轮播图管理：管理员可维护首页轮播图，支持新增、修改、删除、调整排序与上下架，用于首页营销位展示。"),
]

# 4.3.2 追加轮播图表
BLOCKS_4_3_BANNER = [
    ("p", "此外，首页轮播图独立建表（表4-12），用于配置首页营销位的图片、排序与上下架状态。"),
    ("table",
     ["字段名", "类型", "长度", "必填", "字段说明"],
     [
         ["id", "BIGINT", "20", "是", "轮播图ID，主键自增"],
         ["image_url", "VARCHAR", "500", "是", "图片地址（16:9）"],
         ["title", "VARCHAR", "50", "否", "标题，仅后台识别用"],
         ["sort", "INT", "11", "是", "排序值，越小越靠前"],
         ["status", "INT", "11", "是", "状态: 1-上架, 0-下架"],
         ["create_time", "DATETIME", "-", "是", "创建时间"],
         ["update_time", "DATETIME", "-", "是", "更新时间"],
     ],
     "表4-12  首页轮播图表（banner）"),
]

# 4.3.2 追加客服助手未命中记录表
BLOCKS_4_3_CHAT = [
    ("p", "客服助手中未命中的问题单独记录在表4-13，用于分析用户真实疑问并补充知识库。"),
    ("table",
     ["字段名", "类型", "长度", "必填", "字段说明"],
     [
         ["id", "BIGINT", "20", "是", "记录ID，主键自增"],
         ["user_id", "BIGINT", "20", "否", "提问用户ID"],
         ["question", "VARCHAR", "300", "是", "用户提问原文"],
         ["source", "VARCHAR", "20", "是", "回答来源: LLM / FALLBACK"],
         ["top_score", "DECIMAL", "4,3", "否", "知识库最高匹配得分"],
         ["create_time", "DATETIME", "-", "是", "创建时间"],
     ],
     "表4-13  客服助手未命中问题表（chat_unanswered）"),
]

# 5.1.8 个人中心与密码修改
BLOCKS_5_1_8 = [
    ("h3", "5.1.8  个人中心与密码修改"),
    ("p", "个人中心页面只读展示当前登录用户的用户名、邮箱、角色与注册时间，并在同一页面提供修改密码表单。"
          "页面数据来自 GET /api/auth/info 接口，后端返回用户实体，其中密码字段通过 @JsonIgnore 在序列化时忽略，"
          "不会随响应下发。"),
    ("p", "修改密码调用 PUT /api/auth/password，需同时提交原密码与新密码：后端先校验原密码是否正确，"
          "正确则用 BCrypt 重新加密写入。为配合令牌撤销机制，修改密码时还会把用户的令牌版本号加一，"
          "使此前签发的所有令牌立即失效；前端收到成功响应后清除本地 token 并跳转登录页，要求重新登录。"),
    ("p", "该设计同时覆盖了\u201c忘记密码\u201d场景：用户联系管理员后，由管理员在用户管理中将其密码重置为 123456，"
          "用户用该密码登录后，再在个人中心改为自己的密码，形成完整闭环。"),
    ("caption", "图5-14  个人中心与修改密码页面"),
]

# 5.1.9 客服助手
BLOCKS_5_1_9 = [
    ("h3", "5.1.9  客服助手"),
    ("p", "客服助手以抽屉形式从导航栏打开，用户既可以点击系统推荐的常见问题，也可以自行输入。"
          "为兼顾响应速度与调用成本，系统采用「知识库优先、大模型兜底」的三级策略："
          "先在本地的 24 条问答知识库中匹配，命中则直接返回，耗时在毫秒级且不产生 API 费用；"
          "未命中时再调用大模型生成回答；若大模型未配置或调用失败，则返回兜底话术并引导用户联系管理员。"),
    ("p", "匹配环节没有使用向量检索，而是用「关键词特征度 + 二元组 Jaccard 相似度」组合打分："
          "3 字以上的长词，或只出现在一条问答中的短词，视为该问题的特征词，命中即为高置信；"
          "「推荐」「状态」这类被多条问答共用的短词单独命中不作为依据，需要字面相似度支撑。"
          "两项得分取最大值与阈值（默认 0.35）比较，据此判断是否命中。"),
    ("p", "调用大模型时，系统会把最相近的一条知识库问答作为参考上下文一并提交，"
          "并在系统提示词中加以约束：不得编造订单状态与物流信息、不得透露内部实现、超出商城范围直接说明不清楚。"
          "此外按用户统计每日调用次数（默认 30 次，命中知识库不计次），避免接口被滥用。"),
    ("p", "对于知识库未命中的问题，系统只把问题文本与回答来源写入 chat_unanswered 表；"
          "管理端可按问题聚合查看高频疑问，据此持续补充知识库，形成「使用—沉淀—改进」的闭环。"),
    ("caption", "图5-17  客服助手对话界面"),
]

# 5.2.5 轮播图管理（简版）
BLOCKS_5_2_5 = [
    ("h3", "5.2.5  轮播图管理模块"),
    ("p", "轮播图用于首页营销位展示，与商品、分类等业务数据相互独立。管理员可在后台维护轮播图的图片地址、"
          "标题、排序值与上下架状态，支持新增、修改与删除。"),
    ("p", "前台通过 GET /api/banners/list 获取已上架的轮播图并按排序值升序展示，后台接口位于 /api/admin/banners，"
          "仅管理员可调用。首页将轮播图与热藏榜按 3:1 的宽度比例并排布局，轮播图保持 16:9 比例，"
          "热藏榜以竖排列表填充同等高度；用户点击分类筛选或搜索时该区域自动收起，也可手动展开。"),
    ("caption", "图5-15  轮播图管理后台"),
    ("caption", "图5-16  首页轮播图与热藏榜展示效果"),
]

# 需要补图的图题（占位提示里会带出图名，详细要求见 docs/论文配图清单.md）
FIG_TITLES = {
    "5-14": "个人中心与修改密码页面",
    "5-15": "轮播图管理后台",
    "5-16": "首页轮播图与热藏榜展示效果",
    "5-17": "客服助手对话界面",
    "3-2": "管理员端功能用例图",
    "4-7": "购物车操作流程",
    "4-12": "JWT令牌验证流程",
    "4-13": "管理员后台管理流程",
    "5-7": "订单提交页面",
    "5-10": "订单管理页面",
    "5-11": "用户管理页面",
    "6-1": "Allure 测试报告总览",
    "6-2": "Allure 测试套件视图",
    "6-3": "Allure 单个用例执行详情",
}

REPLACE_MAP = {
    "零号藏馆商城系统": "收藏品商城系统",
    "零号藏馆商城": "收藏品商城",
    "零号藏馆": "收藏品商城",
    "Zero Vault collectibles mall system": "collectibles mall system",
    "Zero Vault Collectibles Mall": "Collectibles Mall",
    "Zero Vault collectibles mall": "collectibles mall",
    "Zero Vault": "Collectibles Mall",
}


def main():
    doc = Document(SRC)

    # 1. 全文替换名称
    n = replace_everywhere(doc, REPLACE_MAP)
    print("替换段落数:", n)

    # 2. 封面与标题
    for p in doc.paragraphs:
        if p.style is not None and p.style.name == "论文题目副标题" and p.text.strip():
            set_para_text(p, "基于Spring Boot的收藏品商城系统")
    for table in doc.tables:
        for row in table.rows:
            if row.cells[0].text.strip().startswith("论文题目"):
                for para in row.cells[1].paragraphs:
                    set_para_text(para, "基于Spring Boot的收藏品商城系统")

    # 3. 摘要与英文摘要改写（定位到旧段落）
    paras = doc.paragraphs
    cn_body = [p for p in paras if p.style.name == "Normal"][:0]  # 占位，避免误用
    # 按文本特征精确定位
    def find_by_start(prefix):
        for p in doc.paragraphs:
            if (p.text or "").strip().startswith(prefix):
                return p
        return None

    kw_cn = find_by_start("关键词：")
    abs_en = find_by_start("ABSTRACT")
    kw_en = find_by_start("Keywords:")

    # 中文摘要：取“摘 要”之后、关键词之前的段落
    zh_head = find_by_start("摘  要") or find_by_start("摘 要")
    # 逐段替换
    zh_paras = []
    started = False
    for p in doc.paragraphs:
        if p is zh_head:
            started = True
            continue
        if started and p is kw_cn:
            break
        if started and p.text.strip():
            zh_paras.append(p)
    for i, blocks in enumerate(SUMMARY_CN[:4]):
        if i < len(zh_paras):
            set_para_text(zh_paras[i], blocks[1])
    if kw_cn is not None:
        set_para_text(kw_cn, SUMMARY_CN[4][1])

    en_paras = []
    started = False
    for p in doc.paragraphs:
        if p is abs_en:
            started = True
            continue
        if started and p is kw_en:
            break
        if started and p.text.strip():
            en_paras.append(p)
    for i, blocks in enumerate(SUMMARY_EN[:4]):
        if i < len(en_paras):
            set_para_text(en_paras[i], blocks[1])
    if kw_en is not None:
        set_para_text(kw_en, SUMMARY_EN[4][1])

    # 3b. 上面的对象身份比较不可靠：doc.paragraphs 每次访问都会返回新的代理对象，
    #     `p is zh_head` 永远为 False。这里改用索引精确定位并真正执行替换。
    paras = doc.paragraphs

    def norm(s):
        return (s or "").strip().replace(" ", "").replace("\u3000", "")

    zh_head_i = next(i for i, p in enumerate(paras) if norm(p.text) == "摘要")
    kw_cn_i = next(i for i, p in enumerate(paras) if norm(p.text).startswith("关键词"))
    abs_en_i = next(i for i, p in enumerate(paras) if norm(p.text).upper() == "ABSTRACT")
    kw_en_i = next(i for i, p in enumerate(paras) if norm(p.text).startswith("Keywords:"))

    # 英文摘要标题在原文件中是 Normal 样式，补为一级标题，保证 Word 自动目录能收录
    paras[abs_en_i].style = doc.styles["Heading 1"]

    zh_body = [paras[i] for i in range(zh_head_i + 1, kw_cn_i) if paras[i].text.strip()]
    en_body = [paras[i] for i in range(abs_en_i + 1, kw_en_i) if paras[i].text.strip()]

    for i in range(min(4, len(zh_body))):
        set_para_text(zh_body[i], SUMMARY_CN[i][1])
    set_para_text(paras[kw_cn_i], SUMMARY_CN[4][1])

    for i in range(min(4, len(en_body))):
        set_para_text(en_body[i], SUMMARY_EN[i][1])
    set_para_text(paras[kw_en_i], SUMMARY_EN[4][1])

    # 4. 插入新增章节（先解析锚点，避免索引漂移）
    a_ch1 = find_para(doc, "第一章")
    a_ch3 = find_para(doc, "第三章")
    a_3_2_2 = find_para(doc, "3.2.2")
    a_3_3 = find_para(doc, "3.3  非功能需求")
    a_3_4 = find_para(doc, "3.4")
    a_4_4 = find_para(doc, "4.4  系统功能流程设计")
    a_ch5 = find_para(doc, "第五章")
    a_5_2 = find_para(doc, "5.2  管理员后台功能实现")
    a_ch6 = find_para(doc, "第六章")
    a_6_6 = find_para(doc, "6.6  测试结果分析")
    a_ch7 = find_para(doc, "第七章")

    # 目录占位（放在第一章之前）
    toc_title = a_ch1.insert_paragraph_before("目  录", style="Heading 1")
    toc_title.alignment = CENTER
    toc_title.paragraph_format.page_break_before = True
    hint = a_ch1.insert_paragraph_before(
        "（此处在 Word 中执行「引用 → 目录 → 自动目录」自动生成，无需手写；生成后可删除本行提示）",
        style="Normal")
    hint.alignment = CENTER
    for r in hint.runs:
        r.font.size = Pt(9)
        r.font.color.rgb = RGBColor(0x99, 0x99, 0x99)

    insert_blocks(a_ch3, BLOCKS_1_3, doc)
    insert_blocks(a_ch3, BLOCKS_CH2, doc)
    insert_blocks(a_3_2_2, BLOCKS_3_2, doc)
    insert_blocks(a_3_2_2, BLOCKS_3_2_1, doc)
    insert_blocks(a_3_2_2, BLOCKS_3_2_1_ASSISTANT, doc)
    insert_blocks(a_3_3, BLOCKS_3_2_2, doc)
    insert_blocks(a_3_4, BLOCKS_3_3, doc)
    insert_blocks(a_4_4, BLOCKS_4_3, doc)
    insert_blocks(a_4_4, BLOCKS_4_3_BANNER, doc)
    insert_blocks(a_4_4, BLOCKS_4_3_CHAT, doc)
    insert_blocks(a_ch5, BLOCKS_4_4, doc)
    insert_blocks(a_ch5, BLOCKS_4_5, doc)
    insert_blocks(a_5_2, BLOCKS_5_1_8, doc)
    insert_blocks(a_5_2, BLOCKS_5_1_9, doc)
    insert_blocks(a_ch6, BLOCKS_5_2_5, doc)
    insert_blocks(a_ch6, BLOCKS_5_3, doc)
    insert_blocks(a_6_6, BLOCKS_6_5, doc)
    insert_blocks(a_ch7, BLOCKS_6_7, doc)

    # 5. 第七章改写
    a_7_2 = find_para(doc, "7.2")
    insert_blocks(a_7_2, BLOCKS_7_1, doc)

    for p in doc.paragraphs:
        t = (p.text or "").strip()
        if t.startswith("第四，系统的前端界面较为简洁"):
            set_para_text(p, NEW_385)
        elif t.startswith("同时可以完善前端页面设计"):
            set_para_text(p, NEW_388)

    # 5b. 修正论文与实现不一致之处（详见 docs/系统核查与论文对齐清单.md）
    def replace_text(old, new):
        for p in doc.paragraphs:
            if old in (p.text or ""):
                set_para_text(p, p.text.replace(old, new))
                return True
        return False

    for old, new in TEXT_FIXES:
        if not replace_text(old, new):
            print("警告：未匹配到待替换文本 ->", old[:40])

    # 6. 原文有图题但没有图片的地方，补一个浅色占位提示，便于后续按位置补图
    import re
    placeholders = 0
    for p in list(doc.paragraphs):
        t = (p.text or "").strip()
        m = re.match(r"^[（(]?图\s*(\d+\s*[-－]\s*\d+)", t)
        if not m:
            continue
        prev = p._p.getprevious()
        if prev is not None and prev.xpath(".//w:drawing"):
            continue
        num = m.group(1).replace(" ", "")
        title = FIG_TITLES.get(num, "")
        ph = p.insert_paragraph_before()
        ph.alignment = CENTER
        run = ph.add_run("【此处插入图片：图%s%s】"
                         % (num, ("  " + title) if title else ""))
        run.font.size = Pt(9)
        run.font.color.rgb = RGBColor(0xAA, 0xAA, 0xAA)
        placeholders += 1
    print("补占位图题:", placeholders)

    try:
        doc.save(DST)
        print("已生成:", DST)
    except PermissionError:
        alt = DST.replace(".docx", "-new.docx")
        doc.save(alt)
        print("目标文件被占用（Word 可能正打开该文档），已另存为:", alt)
        print("请在关闭 Word 后执行：mv '%s' '%s'" % (alt, DST))
    print("段落总数:", len(Document(DST).paragraphs))


if __name__ == "__main__":
    main()
