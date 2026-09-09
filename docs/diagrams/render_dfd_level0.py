# Thaika.co DFD — Yourdon / Coad, sized for Word page width.
# Same visual language as Lawway: large bold type, wide gutters, short
# orthogonal arrows, labels beside lines. Process fill #F3E4C0.
import math
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "thaika-dfd-level0.png")

SCALE = 2
W, H = 900, 820
DPI = 220

ENTITY = "#B7D4EA"
PROCESS = "#F3E4C0"
INK = "#111111"
LINE = "#111111"

FONT_DIR = r"C:\Windows\Fonts"
F_BOLD = os.path.join(FONT_DIR, "arialbd.ttf")


def s(v):
    return int(round(v * SCALE))


def font(path, size):
    return ImageFont.truetype(path, s(size))


f_head = font(F_BOLD, 17)
f_num = font(F_BOLD, 18)
f_name = font(F_BOLD, 14)
f_ent = font(F_BOLD, 14)
f_store = font(F_BOLD, 14)
f_label = font(F_BOLD, 13)
f_leg = font(F_BOLD, 13)
f_key = font(F_BOLD, 14)

img = Image.new("RGB", (s(W), s(H)), "white")
d = ImageDraw.Draw(img)
N = {}


def text_c(x, y, text, fnt, fill=INK):
    d.text((s(x), s(y)), text, font=fnt, fill=fill, anchor="ma")


def add_entity(nid, x, y, title, w=128, h=42):
    d.rectangle(
        [s(x), s(y), s(x + w), s(y + h)],
        fill=ENTITY, outline=LINE, width=max(2, SCALE),
    )
    text_c(x + w / 2, y + h / 2, title, f_ent)
    N[nid] = {"x": x, "y": y, "w": w, "h": h}


def add_process(nid, x, y, num, name, w=150, h=122):
    d.ellipse(
        [s(x), s(y), s(x + w), s(y + h)],
        fill=PROCESS, outline=LINE, width=max(2, SCALE),
    )
    text_c(x + w / 2, y + 36, str(num), f_num)
    ty = y + 62
    for line in name.split("\n"):
        text_c(x + w / 2, ty, line, f_name)
        ty += 18
    N[nid] = {"x": x, "y": y, "w": w, "h": h}


def add_store(nid, x, y, title, w=150, h=40):
    d.line([s(x), s(y), s(x + w), s(y)], fill=LINE, width=max(3, SCALE + 1))
    d.line(
        [s(x), s(y + h), s(x + w), s(y + h)],
        fill=LINE, width=max(3, SCALE + 1),
    )
    text_c(x + w / 2, y + h / 2, title, f_store)
    N[nid] = {"x": x, "y": y, "w": w, "h": h}


def arrow_head(tip, angle, size=9):
    spread = math.radians(22)
    a1, a2 = angle + math.pi - spread, angle + math.pi + spread
    d.polygon(
        [
            (s(tip[0]), s(tip[1])),
            (s(tip[0] + size * math.cos(a1)), s(tip[1] + size * math.sin(a1))),
            (s(tip[0] + size * math.cos(a2)), s(tip[1] + size * math.sin(a2))),
        ],
        fill=LINE,
    )


def hline(x0, y, x1, headed=True):
    d.line([s(x0), s(y), s(x1), s(y)], fill=LINE, width=max(3, SCALE + 1))
    if headed:
        arrow_head((x1, y), 0 if x1 >= x0 else math.pi)


def vline(x, y0, y1, headed=True):
    d.line([s(x), s(y0), s(x), s(y1)], fill=LINE, width=max(3, SCALE + 1))
    if headed:
        arrow_head((x, y1), math.pi / 2 if y1 >= y0 else -math.pi / 2)


def label(x, y, text, anchor="mm"):
    d.text((s(x), s(y)), text, font=f_label, fill=INK, anchor=anchor)


def port(n, side, t=0.5):
    b = N[n]
    if side == "right":
        return b["x"] + b["w"], b["y"] + b["h"] * t
    if side == "left":
        return b["x"], b["y"] + b["h"] * t
    if side == "top":
        return b["x"] + b["w"] * t, b["y"]
    return b["x"] + b["w"] * t, b["y"] + b["h"]


def pair_store(process, store, down, up):
    x, y0 = port(process, "bottom", 0.5)
    _, y1 = port(store, "top", 0.5)
    vline(x - 11, y0, y1)
    vline(x + 11, y1, y0)
    label(x + 20, y0 + 9, down, "lm")
    label(x + 20, y1 - 9, up, "lm")


# ---------------------------------------------------------------------------
text_c(W / 2, 22, "Thaika.co — Data Flow Diagram (Level 0)", f_head)

add_entity("contractor", 16, 50, "Contractor", w=140)
add_entity("guest", 728, 50, "Guest Visitor", w=148)
add_entity("client", 16, 240, "Client")
add_entity("ai", 758, 478, "AI Services")
add_entity("email", 348, 696, "Email Service", w=168)

add_process("p1", 175, 200, "1", "authenticate\nuser")
add_process("p2", 365, 200, "2", "manage\nprofiles")
add_process("p3", 555, 200, "3", "discover\ncontractors")
add_process("p4", 175, 470, "4", "handle\njobs & bids")
add_process("p5", 365, 470, "5", "manage\nproject")
add_process("p6", 555, 470, "6", "assist\nwith AI")

add_store("d1", 175, 368, "User Accounts")
add_store("d2", 365, 368, "Profiles")
add_store("d3", 175, 638, "Jobs")
add_store("d4", 365, 638, "Project Data")
add_store("d5", 555, 638, "Messages")

LEFT = 168
MID = 348

# Contractor → 1
x, y0 = port("contractor", "bottom", 0.55)
_, y1 = port("p1", "top", 0.28)
vline(x, y0, y1)
label(x - 8, (y0 + y1) / 2, "credentials", "rm")

# Guest Visitor ↔ 3 (same routing as Lawway)
gx, gy = port("guest", "bottom", 0.45)
p3x, p3y = port("p3", "right", 0.45)
vline(gx, gy, p3y, headed=False)
hline(gx, p3y, p3x)
label(gx + 10, (gy + p3y) / 2, "search filters", "lm")

p3x, p3y = port("p3", "right", 0.62)
gx, gy = port("guest", "bottom", 0.18)
hline(p3x, p3y, gx, headed=False)
vline(gx, p3y, gy)
label(gx - 10, (p3y + gy) / 2, "contractor listings", "rm")

# Client ↔ 1
x0, y = port("client", "right", 0.18)
x1, _ = port("p1", "left", 0.55)
hline(x0, y, x1)
label((x0 + x1) / 2, y - 16, "credentials", "ms")

x1, y = port("p1", "left", 0.78)
x0, _ = port("client", "right", 0.38)
hline(x1, y, x0)
label((x0 + x1) / 2, y + 16, "session", "ms")

# Client → 4 job / bid accept
cx, cy = port("client", "bottom", 0.62)
p4x, p4y = port("p4", "left", 0.28)
vline(cx, cy, p4y, headed=False)
hline(cx, p4y, p4x)
label(cx - 8, (cy + p4y) / 2, "job / bid accept", "rm")

# Client ↔ 6 cost question / estimate (bottom band)
BOT1, BOT2 = 768, 798
JX_AI = 538
cx, cy = port("client", "bottom", 0.38)
p6x, p6y = port("p6", "left", 0.20)
vline(cx, cy, BOT1, headed=False)
hline(cx, BOT1, JX_AI, headed=False)
vline(JX_AI, BOT1, p6y, headed=False)
hline(JX_AI, p6y, p6x)
label(250, BOT1 - 16, "cost question", "ms")

p6x, p6y = port("p6", "left", 0.08)
hline(p6x, p6y, JX_AI + 12, headed=False)
vline(JX_AI + 12, p6y, BOT2, headed=False)
hline(JX_AI + 12, BOT2, 90, headed=False)
vline(90, BOT2, N["client"]["y"] + N["client"]["h"], headed=True)
label(250, BOT2 + 16, "estimate / draft", "ms")

# Contractor left gutter → 2 and 4
p2x, p2y = port("p2", "left", 0.30)
vline(LEFT, 92, p2y, headed=False)
hline(N["contractor"]["x"] + N["contractor"]["w"], 92, LEFT, headed=False)
hline(LEFT, p2y, p2x)
label(88, 248, "profile / portfolio", "mm")

p4x, p4y = port("p4", "left", 0.62)
hline(LEFT, p4y, p4x)
label(LEFT - 12, p4y - 18, "bid / proposal", "rm")

# Contractor → 5 tasks / materials (lower waist, not through process 2)
p5x, p5y = port("p5", "top", 0.22)
vline(MID, 430, p5y, headed=False)
hline(LEFT, 430, MID, headed=False)
hline(MID, p5y, p5x)
label(MID - 12, 448, "tasks / materials", "rm")

# Stores
pair_store("p1", "d1", "store user", "read user")
pair_store("p2", "d2", "store profile", "read profile")
pair_store("p4", "d3", "store job", "read job")
pair_store("p5", "d4", "store work", "read work")
pair_store("p6", "d5", "write message", "read thread")

# Profiles → 3 verified listings
x0, y0 = port("d2", "right", 0.5)
x3, y3 = port("p3", "left", 0.78)
jx = x3 - 12
hline(x0, y0, jx, headed=False)
vline(jx, y0, y3, headed=False)
hline(jx, y3, x3)
label((x0 + jx) / 2, N["d2"]["y"] + N["d2"]["h"] + 12, "verified listings", "mt")

# 4 → 5 awarded job
x0, y = port("p4", "right", 0.50)
x1, _ = port("p5", "left", 0.50)
hline(x0, y, x1)
label((x0 + x1) / 2, y - 16, "awarded job", "ms")

# 5 ↔ Email Service (around Project Data, not through it)
x5, y5 = port("p5", "right", 0.80)
vx, vy = port("email", "top", 0.72)
jx = 518
hline(x5, y5, jx, headed=False)
vline(jx, y5, vy, headed=False)
hline(jx, vy, vx)
label(N["email"]["x"] + N["email"]["w"] / 2, N["email"]["y"] - 14, "contract PDF", "ms")

x5, y5 = port("p5", "bottom", 0.22)
vx, vy = port("email", "top", 0.22)
jx = 348
hline(vx, vy, jx, headed=False)
vline(jx, vy, y5)
label(N["email"]["x"] - 10, N["email"]["y"] + 22, "send status", "rm")

# 6 ↔ AI
x0, y = port("p6", "right", 0.32)
x1, _ = port("ai", "left", 0.32)
hline(x0, y, x1)
ax = N["ai"]["x"] + N["ai"]["w"] / 2
label(ax, N["ai"]["y"] - 14, "Gemini prompt", "ms")
x1, y = port("ai", "left", 0.68)
x0, _ = port("p6", "right", 0.55)
hline(x1, y, x0)
label(ax, N["ai"]["y"] + N["ai"]["h"] + 14, "answer / draft", "ms")

# Key
kx, ky, kw, kh = 728, 690, 154, 116
d.rectangle(
    [s(kx), s(ky), s(kx + kw), s(ky + kh)],
    outline=LINE, width=max(2, SCALE),
)
text_c(kx + kw / 2, ky + 14, "Key", f_key)
d.line([s(kx + 12), s(ky + 36), s(kx + 42), s(ky + 36)], fill=LINE, width=max(3, SCALE))
arrow_head((kx + 42, ky + 36), 0, size=7)
d.text((s(kx + 50), s(ky + 36)), "data flow", font=f_leg, fill=INK, anchor="lm")
d.ellipse(
    [s(kx + 14), s(ky + 48), s(kx + 34), s(ky + 66)],
    fill=PROCESS, outline=LINE, width=SCALE,
)
d.text((s(kx + 42), s(ky + 57)), "process", font=f_leg, fill=INK, anchor="lm")
d.line([s(kx + 14), s(ky + 76), s(kx + 38), s(ky + 76)], fill=LINE, width=max(3, SCALE))
d.line([s(kx + 14), s(ky + 86), s(kx + 38), s(ky + 86)], fill=LINE, width=max(3, SCALE))
d.text((s(kx + 46), s(ky + 81)), "Data Store", font=f_leg, fill=INK, anchor="lm")
d.rectangle(
    [s(kx + 14), s(ky + 94), s(kx + 38), s(ky + 104)],
    fill=ENTITY, outline=LINE, width=SCALE,
)
d.text((s(kx + 46), s(ky + 99)), "External Entity", font=f_leg, fill=INK, anchor="lm")

img.save(OUT, dpi=(DPI, DPI))
print(
    "Saved", OUT, img.size,
    "%.1f x %.1f in at %d dpi" % (img.size[0] / DPI, img.size[1] / DPI, DPI),
)
