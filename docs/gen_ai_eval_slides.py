#!/usr/bin/env python3
"""
Sinh deck .pptx cho cụm "Đánh giá định lượng module AI" (slide B0–B5) + 2 biểu đồ,
khớp tông màu deck Canva hiện tại (trích từ PDF):
  navy #0030B0 · accent #0070C0 · coral #D05050 · nền card xanh nhạt.
Chạy bằng venv: /tmp/slidegen-venv/bin/python docs/gen_ai_eval_slides.py
Output: docs/AI_Evaluation_Slides.pptx  (+ docs/chart_b2.png, docs/chart_b4.png)
"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.ticker import PercentFormatter

from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn

HERE = os.path.dirname(os.path.abspath(__file__))

# ---- Palette (trích từ deck) ----
NAVY   = RGBColor(0x00, 0x30, 0xB0)
ACCENT = RGBColor(0x00, 0x70, 0xC0)
BLUE2  = RGBColor(0x00, 0x60, 0xB0)
CORAL  = RGBColor(0xD0, 0x50, 0x50)
INK    = RGBColor(0x22, 0x2A, 0x3A)
GREY   = RGBColor(0x6B, 0x72, 0x80)
CARD_BLUE = RGBColor(0xDC, 0xE6, 0xF6)
CARD_TEAL = RGBColor(0xDD, 0xEF, 0xEF)
CARD_PEACH= RGBColor(0xF7, 0xE6, 0xDA)
TINT      = RGBColor(0xE6, 0xEF, 0xFA)
WHITE  = RGBColor(0xFF, 0xFF, 0xFF)
LIGHT  = RGBColor(0xE0, 0xF0, 0xF0)

HFONT = "Montserrat"   # tiêu đề / số lớn (Canva có; fallback hệ thống)
BFONT = "Arial"        # body / bảng (an toàn dấu tiếng Việt)

# ---------- charts ----------
plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 12})

def chart_b2(path):
    labels = ["5-fact", "15-fact", "25-fact"]
    hallu = [100, 50, 50]
    faith = [1.70, 3.10, 3.20]
    fig, ax1 = plt.subplots(figsize=(6.2, 3.5), dpi=200)
    fig.patch.set_alpha(0)
    ax1.patch.set_alpha(0)
    bars = ax1.bar(labels, hallu, width=0.5, color="#0070C0", alpha=0.85,
                   label="Tỉ lệ hallucination (%)", zorder=2)
    ax1.set_ylabel("Hallucination (%)", color="#0070C0")
    ax1.set_ylim(0, 110)
    ax1.tick_params(axis="y", labelcolor="#0070C0")
    for b, v in zip(bars, hallu):
        ax1.text(b.get_x()+b.get_width()/2, v+3, f"{v}%", ha="center",
                 color="#0070C0", fontweight="bold")
    ax1.axhline(50, xmin=0.34, xmax=0.97, color="#0070C0", linestyle="--", linewidth=1.3, zorder=1)
    ax1.annotate("trần ~50%", xy=(1.0, 50), xytext=(0.45, 62), color="#0070C0",
                 fontsize=11, fontweight="bold",
                 arrowprops=dict(arrowstyle="->", color="#0070C0"))
    ax2 = ax1.twinx()
    ax2.plot(labels, faith, "-o", color="#D05050", linewidth=2.5, markersize=8,
             label="Faithfulness (1-5)", zorder=3)
    ax2.set_ylabel("Faithfulness (1-5)", color="#D05050")
    ax2.set_ylim(0, 5.2)
    ax2.tick_params(axis="y", labelcolor="#D05050")
    for x, v in enumerate(faith):
        ax2.text(x, v+0.18, f"{v:.2f}", ha="center", color="#D05050", fontweight="bold")
    for s in ["top"]:
        ax1.spines[s].set_visible(False); ax2.spines[s].set_visible(False)
    fig.tight_layout()
    fig.savefig(path, transparent=True, bbox_inches="tight")
    plt.close(fig)

def chart_b4(path):
    groups = ["≤40 trang\n(n=4)", "41–100 trang\n(n=3)", ">100 trang\n(n=3)"]
    stuff = [5254, 6782, 15774]
    rag   = [3082, 2559, 2609]
    pct   = ["−41%", "−62%", "−83%"]
    x = range(len(groups)); w = 0.38
    fig, ax = plt.subplots(figsize=(6.4, 3.5), dpi=200)
    fig.patch.set_alpha(0); ax.patch.set_alpha(0)
    b1 = ax.bar([i-w/2 for i in x], stuff, w, color="#A9B8DC", label="STUFF (nhồi cả tài liệu)")
    b2 = ax.bar([i+w/2 for i in x], rag, w, color="#0070C0", label="RAG (top-k)")
    ax.set_ylabel("Token đầu vào (TB)")
    ax.set_xticks(list(x)); ax.set_xticklabels(groups)
    ax.set_ylim(0, 18000)
    for i in x:
        ax.text(i, max(stuff[i], rag[i])+550, pct[i], ha="center",
                color="#D05050", fontweight="bold", fontsize=13)
    for s in ["top", "right"]:
        ax.spines[s].set_visible(False)
    ax.legend(frameon=False, loc="upper left", fontsize=10)
    fig.tight_layout()
    fig.savefig(path, transparent=True, bbox_inches="tight")
    plt.close(fig)

def chart_p2(path):
    from matplotlib.patches import Patch
    metrics = ["Prompt tokens", "Chi phí sinh", "Completion tokens", "Latency", "Faithfulness"]
    pc   = [-68.6, -55.7, -37.5, -20.2, -17.1]; pc_p = ["0.004","0.002","0.006","0.084","0.027"]
    pc_s = [True, True, True, False, True]
    qz   = [-55.6, -44.9, -3.2, -8.4, None];     qz_p = ["0.004","0.004","0.232","0.065", None]
    qz_s = [True, True, False, False, None]
    PC_C, QZ_C, FA_C = "#0070C0", "#A9CCE8", "#D05050"
    PCOL = 17  # cột p-value cố định bên phải trục 0
    ypos = list(range(len(metrics)))[::-1]; h = 0.36
    fig, ax = plt.subplots(figsize=(8.2, 4.0), dpi=200)
    fig.patch.set_alpha(0); ax.patch.set_alpha(0)

    def vlabel(v, y, inside_color, outside_color):
        if abs(v) >= 15:                      # đủ dài → nhãn nằm trong thanh
            ax.text(v+1.5, y, f"{v:.1f}%", va="center", ha="left",
                    color=inside_color, fontweight="bold", fontsize=9.5)
        else:                                 # thanh ngắn → nhãn ra ngoài bên trái
            ax.text(v-1.5, y, f"{v:.1f}%", va="center", ha="right",
                    color=outside_color, fontweight="bold", fontsize=9.5)

    for i, y in enumerate(ypos):
        c = FA_C if metrics[i] == "Faithfulness" else PC_C
        ax.barh(y+0.2, pc[i], height=h, color=c, hatch="" if pc_s[i] else "////",
                edgecolor="white", zorder=2)
        vlabel(pc[i], y+0.2, "white", "#222")
        ax.text(PCOL, y+0.2, ("✓ " if pc_s[i] else "ns ") + f"p={pc_p[i]}",
                va="center", ha="left", fontsize=8.5, color="#555")
        if qz[i] is not None:
            ax.barh(y-0.2, qz[i], height=h, color=QZ_C, hatch="" if qz_s[i] else "////",
                    edgecolor="white", zorder=2)
            vlabel(qz[i], y-0.2, "#1A3A6B", "#1A3A6B")
            ax.text(PCOL, y-0.2, ("✓ " if qz_s[i] else "ns ") + f"p={qz_p[i]}",
                    va="center", ha="left", fontsize=8.5, color="#999")
        else:
            ax.text(PCOL, y-0.2, "quiz: đo bằng IWF gate (D10)", va="center", ha="left",
                    fontsize=8, color="#999", style="italic")
    ax.axvline(0, color="#888", linewidth=1)
    ax.set_yticks(ypos); ax.set_yticklabels(metrics)
    ax.set_xlim(-82, 46); ax.set_xticks([-80, -60, -40, -20, 0])
    ax.set_xlabel("% thay đổi (RAG so với STUFF)")
    for sname in ["top", "right", "left"]:
        ax.spines[sname].set_visible(False)
    ax.tick_params(length=0)
    leg = [Patch(facecolor=PC_C, label="Page-content (N=30)"),
           Patch(facecolor=QZ_C, label="Focused-quiz (N=10)"),
           Patch(facecolor="white", edgecolor="#888", hatch="////", label="ns (chưa có ý nghĩa)")]
    ax.legend(handles=leg, frameon=False, fontsize=8.5, loc="lower center",
              bbox_to_anchor=(0.5, -0.32), ncol=3)
    fig.tight_layout(); fig.savefig(path, transparent=True, bbox_inches="tight"); plt.close(fig)

def chart_p3(path):
    docs  = ["D01","D02","D03","D04","D05","D06","D07","D08","D09","D10"]
    pages = [36,114,124,65,360,36,72,26,25,57]
    red   = [58,79,88,70,83,59,56,-7,34,59]          # % token reduction (âm = tăng)
    fchg  = ["down","down","same","up","down","up","down","down","down","down"]
    cmap  = {"up":"#2E9E5B", "same":"#9AA3AF", "down":"#D05050"}
    fig, ax = plt.subplots(figsize=(6.5, 3.7), dpi=200)
    fig.patch.set_alpha(0); ax.patch.set_alpha(0)
    offs = {"D01": (-22, -13), "D02": (7, -13), "D03": (8, 5), "D04": (8, 6), "D05": (-30, 7),
            "D06": (-4, 11), "D07": (9, -13), "D08": (10, 6), "D09": (8, 6), "D10": (9, -13)}
    for d, x, y, f in zip(docs, pages, red, fchg):
        ax.scatter(x, y, s=120, color=cmap[f], edgecolor="white", linewidth=1.2, zorder=3)
        ox, oy = offs.get(d, (7, 5))
        ax.annotate(d, (x, y), textcoords="offset points", xytext=(ox, oy), fontsize=9, color="#333")
    ax.axhline(0, color="#bbb", linewidth=1, linestyle="--")
    ax.set_xlabel("Số trang tài liệu"); ax.set_ylabel("% token giảm (RAG)")
    ax.set_ylim(-20, 102); ax.set_xlim(0, 395)
    ax.annotate("tài liệu càng dài → tiết kiệm càng nhiều", xy=(360, 83), xytext=(118, 96),
                fontsize=10, color="#0070C0",
                arrowprops=dict(arrowstyle="->", color="#0070C0"))
    ax.annotate("D08 nhỏ: +7% (top-k > tài liệu)", xy=(26, -7), xytext=(70, -16),
                fontsize=9.5, color="#555", arrowprops=dict(arrowstyle="->", color="#888"))
    # legend màu faithfulness
    from matplotlib.lines import Line2D
    leg = [Line2D([0],[0], marker='o', color='w', markerfacecolor=cmap[k], markersize=10, label=l)
           for k, l in [("up","faith tăng"),("same","faith ~"),("down","faith giảm")]]
    ax.legend(handles=leg, frameon=False, fontsize=9, loc="lower right", title="màu điểm")
    for s in ["top", "right"]:
        ax.spines[s].set_visible(False)
    fig.tight_layout(); fig.savefig(path, transparent=True, bbox_inches="tight"); plt.close(fig)

def chart_p4(path):
    import random
    random.seed(7)
    gem = [3,3,2,5,2,4,4,5,3,3]
    cla = [4,4,3,4,3,5,4,5,3,3]
    hallu = [0,0,1,0,1,0,0,0,0,0]   # 1 = cờ hallucination (cả 2 cùng True)
    fig, ax = plt.subplots(figsize=(5.6, 4.4), dpi=200)
    fig.patch.set_alpha(0); ax.patch.set_alpha(0)
    # dải ±1 quanh đường chéo
    xs = [0.5, 5.5]
    ax.fill_between(xs, [x-1 for x in xs], [x+1 for x in xs], color="#0070C0", alpha=0.10, zorder=0)
    ax.plot(xs, xs, color="#888", linestyle="--", linewidth=1.2, zorder=1, label="đồng thuận tuyệt đối")
    for g, c, h in zip(gem, cla, hallu):
        jx = g + random.uniform(-0.06, 0.06); jy = c + random.uniform(-0.06, 0.06)
        if h:
            ax.scatter(jx, jy, s=150, marker="^", color="#D05050", edgecolor="white",
                       linewidth=1.2, zorder=3)
        else:
            ax.scatter(jx, jy, s=130, color="#0070C0", edgecolor="white", linewidth=1.2, zorder=3)
    ax.set_xlim(0.5, 5.5); ax.set_ylim(0.5, 5.5)
    ax.set_xticks(range(1,6)); ax.set_yticks(range(1,6))
    ax.set_xlabel("Điểm Gemini-Flash (judge)"); ax.set_ylabel("Điểm Claude (reviewer 2)")
    ax.text(0.7, 5.15, "dải ±1 điểm", color="#0070C0", fontsize=10)
    ax.text(3.4, 1.0, "▲ = cờ hallucination (cả 2 cùng True)", color="#D05050", fontsize=9)
    ax.set_aspect("equal")
    for s in ["top", "right"]:
        ax.spines[s].set_visible(False)
    fig.tight_layout(); fig.savefig(path, transparent=True, bbox_inches="tight"); plt.close(fig)

# ---------- pptx helpers ----------
EMU_W, EMU_H = Inches(13.333), Inches(7.5)

def _set_font(run, name=BFONT, size=18, color=INK, bold=False, italic=False):
    run.font.name = name
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = color

def textbox(slide, x, y, w, h, lines, align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP,
            wrap=True):
    """lines: list of (text, dict-style) ; dict keys: name,size,color,bold,italic,space_after"""
    tb = slide.shapes.add_textbox(x, y, w, h)
    tf = tb.text_frame
    tf.word_wrap = wrap
    tf.vertical_anchor = anchor
    tf.margin_left = tf.margin_right = Pt(2)
    tf.margin_top = tf.margin_bottom = Pt(2)
    for i, (text, st) in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = st.get("align", align)
        if "space_after" in st: p.space_after = Pt(st["space_after"])
        if "space_before" in st: p.space_before = Pt(st["space_before"])
        if "line" in st: p.line_spacing = st["line"]
        run = p.add_run(); run.text = text
        _set_font(run, st.get("name", BFONT), st.get("size", 18),
                  st.get("color", INK), st.get("bold", False), st.get("italic", False))
    return tb

def fill_rect(slide, x, y, w, h, color, line=None, shape=MSO_SHAPE.RECTANGLE, round_=False):
    sp = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE if round_ else shape, x, y, w, h)
    sp.fill.solid(); sp.fill.fore_color.rgb = color
    if line is None:
        sp.line.fill.background()
    else:
        sp.line.color.rgb = line; sp.line.width = Pt(1)
    sp.shadow.inherit = False
    return sp

def corner_decor(slide):
    """Hoạ tiết tam giác xanh nhạt ở góc (đơn giản hoá)."""
    t1 = slide.shapes.add_shape(MSO_SHAPE.RIGHT_TRIANGLE, Inches(11.6), Inches(0), Inches(1.733), Inches(1.1))
    t1.rotation = 90; t1.fill.solid(); t1.fill.fore_color.rgb = LIGHT; t1.line.fill.background(); t1.shadow.inherit=False
    t2 = slide.shapes.add_shape(MSO_SHAPE.RIGHT_TRIANGLE, Inches(0), Inches(6.5), Inches(1.6), Inches(1.0))
    t2.rotation = 180; t2.fill.solid(); t2.fill.fore_color.rgb = CARD_BLUE; t2.line.fill.background(); t2.shadow.inherit=False

def header(slide, section, subtitle):
    corner_decor(slide)
    textbox(slide, Inches(0.55), Inches(0.30), Inches(10.5), Inches(0.9), [
        (section, {"name": HFONT, "size": 24, "color": NAVY, "bold": True, "space_after": 2}),
        (subtitle, {"name": BFONT, "size": 15, "color": GREY}),
    ])
    # logo placeholder
    textbox(slide, Inches(11.9), Inches(0.32), Inches(1.2), Inches(0.4),
            [("CSE", {"name": HFONT, "size": 16, "color": RGBColor(0xB0,0xB8,0xC4), "bold": True, "align": PP_ALIGN.RIGHT})])
    # underline accent
    fill_rect(slide, Inches(0.57), Inches(1.22), Inches(0.9), Inches(0.05), ACCENT)

def footer(slide, n):
    textbox(slide, Inches(12.5), Inches(7.05), Inches(0.7), Inches(0.35),
            [(str(n), {"name": BFONT, "size": 11, "color": GREY, "align": PP_ALIGN.RIGHT})])

def card(slide, x, y, w, h, head, head_fill, body, head_text_color=NAVY, body_size=11.5):
    fill_rect(slide, x, y, w, h, WHITE, line=RGBColor(0xDF,0xE5,0xEE), round_=True)
    fill_rect(slide, x, y, w, Inches(0.62), head_fill, round_=True)
    textbox(slide, x+Inches(0.12), y+Inches(0.06), w-Inches(0.24), Inches(0.5),
            [(head, {"name": HFONT, "size": 14, "color": head_text_color, "bold": True,
                     "align": PP_ALIGN.CENTER})], anchor=MSO_ANCHOR.MIDDLE)
    lines = [(t, {"name": BFONT, "size": body_size, "color": INK, "space_after": 4, "line": 1.05}) for t in body]
    textbox(slide, x+Inches(0.18), y+Inches(0.75), w-Inches(0.36), h-Inches(0.85), lines)

def bignum(slide, x, y, w, number, label, num_color=NAVY):
    textbox(slide, x, y, w, Inches(0.9),
            [(number, {"name": HFONT, "size": 40, "color": num_color, "bold": True, "align": PP_ALIGN.CENTER})])
    textbox(slide, x, y+Inches(0.92), w, Inches(0.8),
            [(label, {"name": BFONT, "size": 12.5, "color": INK, "align": PP_ALIGN.CENTER, "line": 1.05})])

def styled_table(slide, x, y, w, h, data, col_widths=None, header_fill=ACCENT,
                 first_col_tint=True, font_size=12, row_h=None):
    rows, cols = len(data), len(data[0])
    gtab = slide.shapes.add_table(rows, cols, x, y, w, h)
    tbl = gtab.table
    tbl.first_row = False; tbl.horz_banding = False
    if col_widths:
        for i, cw in enumerate(col_widths):
            tbl.columns[i].width = cw
    if row_h is not None:
        for r in range(rows):
            tbl.rows[r].height = row_h
    for r in range(rows):
        for c in range(cols):
            cell = tbl.cell(r, c)
            cell.margin_left = Pt(6); cell.margin_right = Pt(6)
            cell.margin_top = Pt(3); cell.margin_bottom = Pt(3)
            cell.vertical_anchor = MSO_ANCHOR.MIDDLE
            is_head = (r == 0)
            if is_head:
                cell.fill.solid(); cell.fill.fore_color.rgb = header_fill
            elif first_col_tint and c == 0:
                cell.fill.solid(); cell.fill.fore_color.rgb = TINT
            else:
                cell.fill.solid(); cell.fill.fore_color.rgb = WHITE
            tf = cell.text_frame; tf.word_wrap = True
            p = tf.paragraphs[0]; p.alignment = PP_ALIGN.LEFT if c == 0 else PP_ALIGN.CENTER
            run = p.add_run(); run.text = str(data[r][c])
            _set_font(run, BFONT, font_size, WHITE if is_head else INK,
                      bold=is_head or (c == 0))
    return gtab

def divider_band(slide, x, y, w, text, color=CORAL):
    fill_rect(slide, x, y, w, Inches(0.5), TINT, round_=True)
    textbox(slide, x+Inches(0.2), y+Inches(0.02), w-Inches(0.4), Inches(0.46),
            [(text, {"name": BFONT, "size": 12.5, "color": color, "italic": True})],
            anchor=MSO_ANCHOR.MIDDLE)

# ---------- build ----------
def build():
    chart_b2(os.path.join(HERE, "chart_b2.png"))
    chart_b4(os.path.join(HERE, "chart_b4.png"))
    chart_p2(os.path.join(HERE, "chart_p2.png"))
    chart_p3(os.path.join(HERE, "chart_p3.png"))
    chart_p4(os.path.join(HERE, "chart_p4.png"))

    prs = Presentation()
    prs.slide_width = EMU_W; prs.slide_height = EMU_H
    blank = prs.slide_layouts[6]

    def new():
        s = prs.slides.add_slide(blank)
        # white bg
        bg = fill_rect(s, 0, 0, EMU_W, EMU_H, WHITE);
        s.shapes._spTree.remove(bg._element); s.shapes._spTree.insert(2, bg._element)
        return s

    SEC = "4. ĐÁNH GIÁ ĐỊNH LƯỢNG MODULE AI"

    # ---- A0: Kiến trúc module AI — sơ đồ 2 pha ----
    s = new(); header(s, "3. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG", "Kiến trúc module AI — pipeline 2 pha")
    footer(s, "A1")
    textbox(s, Inches(0.7), Inches(1.18), Inches(12), Inches(0.3),
            [("Điều phối bởi AiFeatureController · tích hợp qua Spring AI ChatClient (trừu tượng nhà cung cấp — có thể thay model)",
              {"name": BFONT, "size": 11, "color": GREY, "italic": True})])

    def fbox(x, w, y, h, lines, fill, tcolor, fs=10.0):
        fill_rect(s, Inches(x), Inches(y), Inches(w), Inches(h), fill, round_=True)
        paras = [(t, {"name": HFONT if i == 0 else BFONT, "size": fs+1.0 if i == 0 else fs,
                      "color": tcolor, "bold": i == 0, "align": PP_ALIGN.CENTER,
                      "line": 1.0, "space_after": 1}) for i, t in enumerate(lines)]
        textbox(s, Inches(x+0.05), Inches(y+0.03), Inches(w-0.1), Inches(h-0.06),
                paras, anchor=MSO_ANCHOR.MIDDLE)

    def rarrow(x, y, w):
        a = s.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, Inches(x), Inches(y), Inches(w), Inches(0.2))
        a.fill.solid(); a.fill.fore_color.rgb = ACCENT; a.line.fill.background(); a.shadow.inherit = False

    def darrow(xc, y, h, color=ACCENT):
        a = s.shapes.add_shape(MSO_SHAPE.DOWN_ARROW, Inches(xc-0.13), Inches(y), Inches(0.26), Inches(h))
        a.fill.solid(); a.fill.fore_color.rgb = color; a.line.fill.background(); a.shadow.inherit = False

    # ----- PHA A (Ingest, offline) -----
    textbox(s, Inches(0.45), Inches(1.5), Inches(6.5), Inches(0.3),
            [("● PHA A — INGEST / INDEX  (offline, @Async)", {"name": HFONT, "size": 11.5, "color": ACCENT, "bold": True})])
    ya, ha = 1.85, 0.82
    fbox(0.45, 2.0, ya, ha, ["Tài liệu nguồn", "PDF / DOCX / PPT"], CARD_BLUE, NAVY)
    rarrow(2.48, ya+0.31, 0.24)
    fbox(2.75, 2.2, ya, ha, ["DocumentChunker", "1200 / overlap 200"], CARD_BLUE, NAVY)
    rarrow(4.97, ya+0.31, 0.21)
    fbox(5.2, 2.9, ya, ha, ["GeminiEmbeddingClient", "768 chiều"], CARD_BLUE, NAVY)
    textbox(s, Inches(8.35), Inches(2.0), Inches(4.5), Inches(0.75),
            [("Chạy MỘT LẦN lúc lưu khóa học", {"name": BFONT, "size": 10.5, "color": GREY, "italic": True, "space_after": 1}),
             ("(tài liệu → vector), không lặp khi sinh.", {"name": BFONT, "size": 10.5, "color": GREY, "italic": True})])

    # ----- pgvector (kho chia sẻ) -----
    fbox(5.2, 2.9, 3.05, 1.0, ["pgvector — kho vector khóa học", "DocumentChunkDao"], ACCENT, WHITE, fs=10.5)
    darrow(6.65, 2.69, 0.34)                      # ghi
    textbox(s, Inches(6.82), Inches(2.66), Inches(1.2), Inches(0.3),
            [("ghi", {"name": BFONT, "size": 9, "color": GREY})])
    darrow(6.65, 4.12, 0.78)                      # đọc
    textbox(s, Inches(6.82), Inches(4.35), Inches(1.6), Inches(0.3),
            [("đọc top-k", {"name": BFONT, "size": 9, "color": GREY})])

    # ----- PHA B (Serve, online) -----
    textbox(s, Inches(0.45), Inches(4.58), Inches(5.0), Inches(0.3),
            [("● PHA B — SINH NỘI DUNG  (online · mỗi request)", {"name": HFONT, "size": 11.5, "color": ACCENT, "bold": True})])
    yb, hb = 4.95, 0.82
    fbox(0.45, 2.0, yb, hb, ["Yêu cầu người dùng", "chủ đề · độ khó"], CARD_TEAL, NAVY)
    rarrow(2.5, yb+0.31, 2.6)
    fbox(5.2, 2.9, yb, hb, ["Embed query → truy xuất top-k", "(fallback: nhồi tài liệu)"], CARD_BLUE, NAVY)
    rarrow(8.12, yb+0.31, 0.2)
    fbox(8.35, 2.3, yb, hb, ["AiGeneratorService", "ChatClient → Gemini 2.5 Pro"], BLUE2, WHITE)
    rarrow(10.66, yb+0.31, 0.13)
    fbox(10.8, 2.05, yb, hb, ["QuizSchemaValidator", "gate + retry (Quiz)"], CORAL, WHITE)
    textbox(s, Inches(8.35), Inches(5.85), Inches(4.5), Inches(0.35),
            [("↓  Đầu ra: JSON có schema  →  trình soạn thảo SCORM",
              {"name": HFONT, "size": 10.5, "color": NAVY, "bold": True})])

    textbox(s, Inches(0.45), Inches(6.5), Inches(12.4), Inches(0.45),
            [("Toàn bộ fail-soft: RAG lỗi/chưa index → tự fallback nhồi toàn tài liệu, không làm hỏng luồng. Gate chỉ áp cho Quiz; outline/page-content đi thẳng từ LLM.",
              {"name": BFONT, "size": 10.5, "color": GREY, "italic": True})])

    # ---- A1: Vì sao Gemini 2.5 Pro (thuộc Mục 3) ----
    s = new(); header(s, "3. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG", "Vì sao chọn Google Gemini 2.5 Pro?")
    footer(s, "A2")
    cw, ch = Inches(5.92), Inches(1.95); gx, gy = Inches(0.7), Inches(1.55)
    a_cards = [
        ("01 · Context window cực lớn (~1M token)", ACCENT, WHITE,
         ["Nhồi TOÀN BỘ tài liệu nguồn để bám sát — tài liệu test có cuốn 360 trang.",
          "⭐ Lý do quyết định: model context nhỏ không grounding nổi."]),
        ("02 · Đa phương thức (multimodal) gốc", CARD_TEAL, NAVY,
         ["Đọc & hiểu trực tiếp PDF / DOCX / PPT —", "đúng định dạng học liệu giảng viên tải lên."]),
        ("03 · Structured output (JSON) ổn định", CARD_BLUE, NAVY,
         ["Mọi tính năng parse JSON theo schema.", "→ Đã đo 100% JSON/schema hợp lệ (60 lượt gọi)."]),
        ("04 · Tiếng Việt tốt + hệ sinh thái thống nhất", CARD_PEACH, CORAL,
         ["Corpus nhiều tài liệu Việt; cùng nhà cung cấp có embedding",
          "gemini-embedding-001 cho RAG; quota/chi phí hợp lý."]),
    ]
    for i, (head_t, hf, htc, body) in enumerate(a_cards):
        x = gx + (i % 2) * (cw + Inches(0.2))
        y = gy + (i // 2) * (ch + Inches(0.2))
        card(s, x, y, cw, ch, head_t, hf, body, head_text_color=htc)
    fill_rect(s, Inches(0.7), Inches(5.85), Inches(11.93), Inches(0.95), TINT, round_=True)
    textbox(s, Inches(0.95), Inches(5.98), Inches(11.4), Inches(0.75), [
        ("Tích hợp qua Spring AI ChatClient → trừu tượng hoá nhà cung cấp: có thể THAY MODEL mà không sửa nghiệp vụ (không khoá cứng vendor).",
         {"name": BFONT, "size": 13, "color": NAVY, "bold": True, "space_after": 2}),
        ("Lưu ý trung thực: chưa chạy bake-off head-to-head GPT/Claude — chọn theo tiêu chí khớp yêu cầu, và chứng minh đạt ngưỡng bằng đánh giá định lượng (phần sau).",
         {"name": BFONT, "size": 11.5, "color": GREY, "italic": True}),
    ])

    # ---- B0: khung đánh giá ----
    s = new(); header(s, SEC, "Phương pháp & khung đo lường"); footer(s, 1)
    textbox(s, Inches(0.6), Inches(1.5), Inches(12.1), Inches(0.8),
            [("“Module AI có đáng tin, hiệu quả và sư phạm không? → ĐO LƯỜNG, không KHẲNG ĐỊNH.”",
              {"name": HFONT, "size": 18, "color": NAVY, "bold": True, "align": PP_ALIGN.CENTER})])
    badges = [("A", "Độ tin cậy", CARD_BLUE), ("B", "Độ trễ & Chi phí", CARD_TEAL),
              ("C", "Trung thực\n(chống bịa)", CARD_PEACH), ("D/E", "Sư phạm:\nIWF + Bloom", CARD_BLUE),
              ("F", "RAG vs Nhồi", CARD_TEAL)]
    bw = Inches(2.3); gap = Inches(0.16); x0 = Inches(0.7); y0 = Inches(2.7)
    for i, (tag, name, col) in enumerate(badges):
        x = x0 + i*(bw+gap)
        fill_rect(s, x, y0, bw, Inches(1.25), col, round_=True)
        textbox(s, x, y0+Inches(0.12), bw, Inches(0.5),
                [(tag, {"name": HFONT, "size": 22, "color": NAVY, "bold": True, "align": PP_ALIGN.CENTER})])
        textbox(s, x+Inches(0.1), y0+Inches(0.66), bw-Inches(0.2), Inches(0.55),
                [(name, {"name": BFONT, "size": 12, "color": INK, "align": PP_ALIGN.CENTER, "line": 1.0})])
    # Hai hộp song song: dữ liệu (trái) + chuẩn sư phạm (phải)
    bx, bw2, by2, bh2 = Inches(0.7), Inches(5.86), Inches(4.15), Inches(2.45)
    fill_rect(s, bx, by2, bw2, bh2, TINT, round_=True)
    textbox(s, bx+Inches(0.25), by2+Inches(0.12), bw2-Inches(0.5), bh2-Inches(0.2), [
        ("TẬP DỮ LIỆU & THIẾT LẬP", {"name": HFONT, "size": 13, "color": ACCENT, "bold": True, "space_after": 5}),
        ("• 10 tài liệu đa lĩnh vực (CNTT · KHTN · KHXH · tiếng Anh · tiếng Việt)",
         {"name": BFONT, "size": 12, "color": INK, "space_after": 4, "line": 1.05}),
        ("• + 3 tài liệu hold-out (chưa từng dùng tune → chống overfitting)",
         {"name": BFONT, "size": 12, "color": INK, "space_after": 4, "line": 1.05}),
        ("• Model pin gemini-2.5-pro-preview-05-06, temp 0.2 → tái lập được",
         {"name": BFONT, "size": 12, "color": INK, "line": 1.05}),
    ])
    px2 = bx + bw2 + Inches(0.21)
    fill_rect(s, px2, by2, bw2, bh2, CARD_PEACH, round_=True)
    textbox(s, px2+Inches(0.25), by2+Inches(0.12), bw2-Inches(0.5), bh2-Inches(0.2), [
        ("CHUẨN SƯ PHẠM ĐÁNH GIÁ CHẤT LƯỢNG CÂU HỎI", {"name": HFONT, "size": 12.5, "color": CORAL, "bold": True, "space_after": 5}),
        ("• Item Writing Flaws — Haladyna (2002): 12 lỗi soạn câu hỏi trắc nghiệm chuẩn → chất lượng kỹ thuật",
         {"name": BFONT, "size": 11.5, "color": INK, "space_after": 4, "line": 1.03}),
        ("• Thang Bloom (6 mức: Nhớ→Hiểu→Áp dụng→Phân tích→Đánh giá→Sáng tạo) → cấp độ tư duy & độ khó",
         {"name": BFONT, "size": 11.5, "color": INK, "space_after": 4, "line": 1.03}),
        ("• 4 tiêu chí sư phạm: rõ ràng · 1 đáp án đúng · nhiễu hợp lý · bám nguồn",
         {"name": BFONT, "size": 11.5, "color": INK, "line": 1.03}),
    ])

    # ---- B1: reliability + latency/cost ----
    s = new(); header(s, SEC, "A + B — Độ tin cậy, độ trễ & chi phí"); footer(s, 2)
    bignum(s, Inches(0.7), Inches(1.7), Inches(3.7), "100%", "JSON parse & schema hợp lệ\n(60 lượt gọi)")
    bignum(s, Inches(4.8), Inches(1.7), Inches(3.7), "~20–25s", "Độ trễ p50 / lượt\n(chi phí ~$0.02–0.06)", num_color=ACCENT)
    bignum(s, Inches(8.9), Inches(1.7), Inches(3.7), "0%", "Trường rỗng (sau khi sửa)\n— ban đầu 35%", num_color=CORAL)
    styled_table(s, Inches(0.7), Inches(4.05), Inches(6.2), Inches(1.6),
                 [["Tính năng", "Latency p50", "Cost/lượt"],
                  ["Sinh dàn ý", "~21s", "~$0.06"],
                  ["Sinh nội dung", "~25s", "~$0.024"],
                  ["Sinh quiz", "~20s", "~$0.017"]],
                 col_widths=[Inches(2.8), Inches(1.7), Inches(1.7)])
    fill_rect(s, Inches(7.2), Inches(4.05), Inches(5.43), Inches(1.6), CARD_PEACH, round_=True)
    textbox(s, Inches(7.45), Inches(4.2), Inches(5.0), Inches(1.35), [
        ("💡 Quan sát định lượng", {"name": HFONT, "size": 13, "color": CORAL, "bold": True, "space_after": 4}),
        ("100% đầu ra bị bọc markdown fence dù prompt cấm → cần lớp hậu xử lý stripJsonFence. Phản chứng giả định “ép JSON là hết lỗi cú pháp”.",
         {"name": BFONT, "size": 12.5, "color": INK, "line": 1.08}),
    ])

    # ---- B2: faithfulness ----
    s = new(); header(s, SEC, "C — Độ trung thực & “trần” hallucination"); footer(s, 3)
    s.shapes.add_picture(os.path.join(HERE, "chart_b2.png"), Inches(0.5), Inches(1.6), height=Inches(3.4))
    textbox(s, Inches(7.35), Inches(1.6), Inches(5.45), Inches(3.6), [
        ("Mở rộng Ground-Truth 5→15→25 facts:", {"name": HFONT, "size": 14, "color": NAVY, "bold": True, "space_after": 5}),
        ("• Hallucination chạm TRẦN ~50% — chứng minh là giới hạn phương pháp, không hand-wave.",
         {"name": BFONT, "size": 13, "color": INK, "space_after": 5, "line": 1.05}),
        ("• Phân rã 50% còn lại (đọc tay 5 ca): 100% là inference-extension, 0 ca mâu thuẫn nguồn.",
         {"name": BFONT, "size": 13, "color": INK, "space_after": 5, "line": 1.05}),
        ("→ Tỉ lệ bịa-sai thực tế ước tính 0–10%.",
         {"name": BFONT, "size": 13, "color": CORAL, "bold": True, "space_after": 5}),
    ])
    divider_band(s, Inches(7.35), Inches(5.35), Inches(5.45),
                 "So literature: ChatGPT MCQ 30–40% · RAG-SOTA 5–10% · ta ~50%", color=BLUE2)
    divider_band(s, Inches(0.55), Inches(5.35), Inches(6.0),
                 "AI là “trợ lý cần giảng viên duyệt”, không tự động tuyệt đối.", color=NAVY)

    # ---- B3: IWF + runtime gate ----
    s = new(); header(s, SEC, "D/E — Chất lượng câu hỏi & kiểm soát runtime"); footer(s, 4)
    bignum(s, Inches(0.7), Inches(1.8), Inches(4.0), "68% → 94.4%", "Câu KHÔNG lỗi soạn đề (IWF)\ntrên tài liệu lạ (hold-out)")
    bignum(s, Inches(0.7), Inches(4.0), Inches(4.0), "17/18", "Đạt cả 4 tiêu chí sư phạm\n(94.4%)", num_color=ACCENT)
    # gate pipeline
    fill_rect(s, Inches(5.1), Inches(1.9), Inches(7.5), Inches(4.4), TINT, round_=True)
    textbox(s, Inches(5.3), Inches(2.05), Inches(7.1), Inches(0.5),
            [("RUNTIME QUALITY GATE (kiểm soát từng câu)", {"name": HFONT, "size": 14, "color": ACCENT, "bold": True})])
    steps = ["1 · Sinh quiz", "2 · Kiểm tra: Schema · Citation trích nguyên văn · Bloom 1–6 · IWF (TW-1/TW-5/ID-2)",
             "3 · Lỗi? → Tự sửa (retry 1 lần)", "4 · Trả kết quả  (đếm metric qua Micrometer)"]
    yy = Inches(2.7)
    for i, st in enumerate(steps):
        fill_rect(s, Inches(5.4), yy, Inches(6.9), Inches(0.7), WHITE, line=RGBColor(0xCF,0xDA,0xEC), round_=True)
        textbox(s, Inches(5.6), yy+Inches(0.04), Inches(6.6), Inches(0.62),
                [(st, {"name": BFONT, "size": 12.5, "color": INK if i!=2 else CORAL, "bold": i==2})],
                anchor=MSO_ANCHOR.MIDDLE)
        yy = yy + Inches(0.88)

    # ---- B4: RAG ----
    s = new(); header(s, SEC, "F — RAG vs Nhồi tài liệu (đóng góp nổi bật)"); footer(s, 5)
    bignum(s, Inches(0.7), Inches(1.7), Inches(3.5), "−68.6%", "Token đầu vào\n(Wilcoxon p = 0.004)", num_color=NAVY)
    bignum(s, Inches(0.7), Inches(3.9), Inches(3.5), "−55.7%", "Chi phí sinh\n(p = 0.002)", num_color=ACCENT)
    s.shapes.add_picture(os.path.join(HERE, "chart_b4.png"), Inches(4.4), Inches(1.55), height=Inches(3.6))
    divider_band(s, Inches(4.4), Inches(5.45), Inches(8.2),
                 "Đánh đổi trung thực: faithfulness −17% (p=0.027) do top-k bỏ sót — giảm được bằng tăng top-k / reranking.",
                 color=CORAL)

    # ---- B5: tổng kết + integrity ----
    s = new(); header(s, SEC, "Tổng kết & tính liêm chính phương pháp"); footer(s, 6)
    card(s, Inches(0.7), Inches(1.65), Inches(3.85), Inches(2.5), "TIN CẬY", CARD_BLUE,
         ["100% schema hợp lệ", "94% câu không lỗi (hold-out)", "Gate tự sửa runtime"])
    card(s, Inches(4.74), Inches(1.65), Inches(3.85), Inches(2.5), "TRUNG THỰC", CARD_PEACH,
         ["Bịa-sai thực ~0–10%", "0 ca mâu thuẫn nguồn", "AI = trợ lý có giám sát"], head_text_color=CORAL)
    card(s, Inches(8.78), Inches(1.65), Inches(3.85), Inches(2.5), "HIỆU QUẢ", CARD_TEAL,
         ["RAG −69% token", "−56% chi phí (significant)", "Mở rộng theo độ dài tài liệu"])
    fill_rect(s, Inches(0.7), Inches(4.4), Inches(11.93), Inches(1.5), TINT, round_=True)
    textbox(s, Inches(0.95), Inches(4.55), Inches(11.4), Inches(1.25), [
        ("✓ Liêm chính: faithfulness do AI-judge → DISCLOSE rõ không phải human, và KIỂM CHỨNG CHÉO bằng model thứ hai:",
         {"name": HFONT, "size": 13, "color": ACCENT, "bold": True, "space_after": 3}),
        ("κ (hallucination) = 1.00 · điểm 100% trong ±1 (QWK 0.66) → judge không phải outlier.",
         {"name": BFONT, "size": 13, "color": INK, "space_after": 3}),
        ("Future work: human rescoring · tăng top-k/reranking · bake-off đa model.",
         {"name": BFONT, "size": 12.5, "color": GREY, "italic": True}),
    ])

    APX = "PHỤ LỤC — DỮ LIỆU CHI TIẾT (Q&A)"

    # ---- P1: Danh sách tài liệu đo lường ----
    s = new(); header(s, APX, "P1 — Danh sách tài liệu dùng để đo lường"); footer(s, "P1")
    textbox(s, Inches(0.7), Inches(1.45), Inches(12), Inches(0.32),
            [("Tập chính (dùng để phát triển & đo) — 10 tài liệu", {"name": HFONT, "size": 13, "color": ACCENT, "bold": True})])
    main_cols = [Inches(0.85), Inches(5.0), Inches(3.0), Inches(1.25), Inches(1.83)]
    styled_table(s, Inches(0.7), Inches(1.82), Inches(11.93), Inches(3.2),
                 [["Mã", "Tài liệu", "Lĩnh vực", "Ngôn ngữ", "Quy mô"],
                  ["D01", "Programming Languages & Compilers (Intro)", "CNTT — Ngôn ngữ LT", "EN", "36 trang"],
                  ["D02", "Structured Query Language — DDL/DML/DCL", "CNTT — CSDL", "EN", "114 trang"],
                  ["D03", "Searching Algorithms & Hash Tables", "CNTT — CTDL & Giải thuật", "EN", "124 trang"],
                  ["D04", "Transaction Processing Concepts", "CNTT — CSDL / Giao dịch", "EN", "65 trang"],
                  ["D05", "Graph Connectivity (Discrete Structures)", "Toán rời rạc", "EN/VI", "360 trang"],
                  ["D06", "Bayesian Learning", "AI — Học máy", "EN", "36 trang"],
                  ["D07", "Design Thinking (E-Commerce)", "TMĐT — Thiết kế", "EN", "72 trang"],
                  ["D08", "Đại cương về Marketing", "Kinh tế — Marketing", "VI", "26 trang"],
                  ["D09", "Trường tĩnh điện trong chân không", "Vật lý đại cương", "VI", "25 trang"],
                  ["D10", "Supply, Demand & Market Equilibrium", "Kinh tế vi mô", "EN", "57 trang"]],
                 col_widths=main_cols, font_size=10.5, row_h=Inches(0.285))
    textbox(s, Inches(0.7), Inches(5.18), Inches(12), Inches(0.32),
            [("Tập hold-out (CHƯA từng dùng tune → chống overfitting) — 3 tài liệu", {"name": HFONT, "size": 13, "color": CORAL, "bold": True})])
    styled_table(s, Inches(0.7), Inches(5.55), Inches(11.93), Inches(1.15),
                 [["Mã", "Tài liệu", "Lĩnh vực", "Ngôn ngữ", "Quy mô"],
                  ["D11", "Data Streaming (Chapter 2)", "CNTT — Kỹ thuật dữ liệu", "EN", "~17k ký tự"],
                  ["D12", "Những khái niệm chung về Nhà nước", "Luật / Chính trị", "VI", "~14k ký tự"],
                  ["D13", "Delta Lake", "CNTT — Kỹ thuật dữ liệu", "EN", "~95k ký tự"]],
                 col_widths=main_cols, font_size=10.5, row_h=Inches(0.285), header_fill=CORAL)
    textbox(s, Inches(0.7), Inches(6.78), Inches(12), Inches(0.3),
            [("13 tài liệu · EN + VI · 25–360 trang · phủ CNTT, Toán, AI, Kinh tế, Marketing, Vật lý, Luật.",
              {"name": BFONT, "size": 10.5, "color": GREY, "italic": True})])

    # ---- P2: Wilcoxon đầy đủ ----
    s = new(); header(s, APX, "P2 — Kiểm định Wilcoxon signed-rank (paired)"); footer(s, "P2")
    textbox(s, Inches(0.7), Inches(1.45), Inches(12), Inches(0.32),
            [("RAG vs STUFF — % thay đổi theo chỉ số  (page-content N=30 · focused-quiz N=10)",
              {"name": HFONT, "size": 12.5, "color": ACCENT, "bold": True})])
    s.shapes.add_picture(os.path.join(HERE, "chart_p2.png"), Inches(2.15), Inches(1.85), height=Inches(4.15))
    textbox(s, Inches(0.7), Inches(6.15), Inches(12.1), Inches(0.9), [
        ("Token & chi phí giảm mạnh, có ý nghĩa thống kê (p ≤ 0,006) ở CẢ HAI chức năng; latency chỉ là xu hướng (ns).",
         {"name": BFONT, "size": 12, "color": INK, "space_after": 3}),
        ("Faithfulness chỉ vẽ cho page-content (đánh đổi −17%, p=0,027); chất lượng quiz đo bằng IWF gate (D10), không dùng key-fact judge.",
         {"name": BFONT, "size": 11.5, "color": GREY, "italic": True}),
    ])

    # ---- P2: per-doc RAG ----
    s = new(); header(s, APX, "P3 — RAG vs STUFF theo từng tài liệu (page-content)"); footer(s, "P3")
    s.shapes.add_picture(os.path.join(HERE, "chart_p3.png"), Inches(0.4), Inches(1.65), height=Inches(4.1))
    styled_table(s, Inches(7.75), Inches(1.62), Inches(4.9), Inches(4.2),
                 [["Doc", "Δ token", "Faith S→R"],
                  ["D01", "−58%", "3,67 → 3,00"], ["D02", "−79%", "4,67 → 3,33"],
                  ["D03", "−88%", "2,67 → 2,67"], ["D04", "−70%", "4,00 → 4,50"],
                  ["D05", "−83%", "3,67 → 2,00"], ["D06", "−59%", "4,00 → 4,33"],
                  ["D07", "−56%", "5,00 → 3,33"], ["D08", "+7%", "5,00 → 4,33"],
                  ["D09", "−34%", "3,00 → 2,67"], ["D10", "−59%", "5,00 → 3,67"]],
                 col_widths=[Inches(1.15), Inches(1.5), Inches(2.25)], font_size=10.5, row_h=Inches(0.37))
    textbox(s, Inches(0.5), Inches(6.0), Inches(12.2), Inches(0.7),
            [("D08 (26 trang) +7% token vì top-6 chunk > cả tài liệu · D05 (360 trang) faithfulness giảm mạnh nhất do top-k bỏ sót · Faith = TB 3 lần (loại 1 dòng lỗi D04).",
              {"name": BFONT, "size": 11, "color": GREY, "italic": True})])

    # ---- P3: cross-check judge ----
    s = new(); header(s, APX, "P4 — Cross-check LLM-judge bằng model thứ hai"); footer(s, "P4")
    s.shapes.add_picture(os.path.join(HERE, "chart_p4.png"), Inches(0.55), Inches(1.65), height=Inches(4.45))
    # aggregate panel
    px, pw = Inches(7.6), Inches(5.05)
    fill_rect(s, px, Inches(1.6), pw, Inches(4.5), TINT, round_=True)
    textbox(s, px+Inches(0.25), Inches(1.75), pw-Inches(0.5), Inches(0.4),
            [("ĐỘ ĐỒNG THUẬN (n=10)", {"name": HFONT, "size": 14, "color": ACCENT, "bold": True})])
    stats = [("κ hallucination = 1,00", "trùng khớp hoàn toàn", CORAL),
             ("QWK (điểm 1–5) = 0,66", "khá (substantial)", NAVY),
             ("Đồng thuận ±1 điểm = 100%", "không cặp nào lệch > 1", NAVY),
             ("MAD = 0,60 · TB G/C = 3,40 / 3,80", "Claude lenient hơn ~0,4", GREY)]
    yy = Inches(2.35)
    for big, sub, col in stats:
        textbox(s, px+Inches(0.25), yy, pw-Inches(0.5), Inches(0.5),
                [(big, {"name": HFONT, "size": 16, "color": col, "bold": True})])
        textbox(s, px+Inches(0.25), yy+Inches(0.42), pw-Inches(0.5), Inches(0.35),
                [(sub, {"name": BFONT, "size": 11.5, "color": GREY, "italic": True})])
        yy = yy + Inches(0.88)
    textbox(s, Inches(0.7), Inches(6.35), Inches(12), Inches(0.5),
            [("Gemini-Flash judge vs model thứ 2 (Claude) trên CÙNG key_facts → cross-MODEL, không phải human inter-rater (đã disclose). Judge không phải outlier.",
              {"name": BFONT, "size": 11, "color": GREY, "italic": True})])

    out = os.path.join(HERE, "AI_Evaluation_Slides.pptx")
    prs.save(out)
    print("Saved:", out, "| slides:", len(prs.slides._sldIdLst))

if __name__ == "__main__":
    build()
