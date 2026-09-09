# C4 Level-3 component diagram — same arrow rules as Lawway:
# people on the left, right-hand bus to externals, no snakes
# through the grid, labels in the gutters.
import math
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "thaika-component-diagram-c4.png")

SCALE = 2
W, H = 1240, 760
DPI = 220

PERSON = "#1B671E"
COMP = "#5BAB5D"
EXT = "#999999"
DB = "#5BAB5D"
INK = "#111111"
ARROW = "#2B6CA8"
BOUNDARY = "#6A9BC9"
BOUNDARY_FILL = "#EEF3F7"
WHITE = "#FFFFFF"

FONT_DIR = r"C:\Windows\Fonts"
F_BOLD = os.path.join(FONT_DIR, "arialbd.ttf")
F_REG = os.path.join(FONT_DIR, "arial.ttf")
F_ITAL = os.path.join(FONT_DIR, "ariali.ttf")


def s(v):
    return int(round(v * SCALE))


def font(path, size):
    return ImageFont.truetype(path, s(size))


f_head = font(F_BOLD, 18)
f_sub = font(F_REG, 12)
f_title = font(F_BOLD, 14)
f_tech = font(F_ITAL, 10)
f_body = font(F_REG, 12)
f_label = font(F_BOLD, 12)
f_bound = font(F_BOLD, 13)
f_leg = font(F_BOLD, 12)
f_foot = font(F_BOLD, 12)

img = Image.new("RGB", (s(W), s(H)), "white")
d = ImageDraw.Draw(img)


def wrap(text, fnt, max_w):
    words, lines, cur = text.split(), [], ""
    for w in words:
        trial = (cur + " " + w).strip()
        if d.textlength(trial, font=fnt) <= s(max_w) or not cur:
            cur = trial
        else:
            lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines


def text_c(x, y, text, fnt, fill=INK):
    d.text((s(x), s(y)), text, font=fnt, fill=fill, anchor="ma")


def shade(hex_color, factor=0.78):
    r, g, b = (int(hex_color[i : i + 2], 16) for i in (1, 3, 5))
    return "#%02X%02X%02X" % (int(r * factor), int(g * factor), int(b * factor))


def person_icon(cx, y, fill=WHITE):
    d.ellipse([s(cx - 6), s(y), s(cx + 6), s(y + 12)], fill=fill)
    d.polygon(
        [(s(cx), s(y + 11)), (s(cx - 11), s(y + 28)), (s(cx + 11), s(y + 28))],
        fill=fill,
    )


def node(x, y, w, h, title, tech, desc, fill, shape="box"):
    stroke = shade(fill, 0.72)
    if shape == "db":
        ey = 14
        d.ellipse(
            [s(x), s(y + h - ey), s(x + w), s(y + h)],
            fill=fill, outline=stroke, width=max(1, SCALE),
        )
        d.rectangle([s(x), s(y + ey / 2), s(x + w), s(y + h - ey / 2)], fill=fill)
        d.ellipse(
            [s(x), s(y), s(x + w), s(y + ey)],
            fill=fill, outline=stroke, width=max(1, SCALE),
        )
        pad_top = 20
    elif shape == "person":
        d.rounded_rectangle(
            [s(x), s(y), s(x + w), s(y + h)],
            radius=s(8), fill=fill, outline=stroke, width=max(1, SCALE),
        )
        person_icon(x + w / 2, y + 6)
        pad_top = 34
    else:
        d.rounded_rectangle(
            [s(x), s(y), s(x + w), s(y + h)],
            radius=s(8), fill=fill, outline=stroke, width=max(1, SCALE),
        )
        pad_top = 10
    cx, ty = x + w / 2, y + pad_top
    if shape == "person":
        text_c(cx, ty + 7, title, f_title, WHITE)
        ty += 18
        text_c(cx, ty + 6, tech, f_tech, "#DCE6F1")
        ty += 15
        if desc:
            for line in wrap(desc, f_body, w - 16):
                text_c(cx, ty + 7, line, f_body, WHITE)
                ty += 14
    elif shape == "db":
        text_c(cx, y + 20, tech, f_tech, "#DCE6F1")
        text_c(cx, y + 36, title, f_title, WHITE)
        if desc:
            text_c(cx, y + 54, desc, f_body, WHITE)
    else:
        text_c(cx, ty + 6, tech, f_tech, "#DCE6F1")
        ty += 15
        text_c(cx, ty + 8, title, f_title, WHITE)
        ty += 20
        if desc:
            for line in wrap(desc, f_body, w - 16):
                text_c(cx, ty + 7, line, f_body, WHITE)
                ty += 15
    return {"x": x, "y": y, "w": w, "h": h}


def dashed_rect(x1, y1, x2, y2, dash=9, gap=5):
    d.rectangle([s(x1), s(y1), s(x2), s(y2)], fill=BOUNDARY_FILL)

    def seg(ax, ay, bx, by):
        length = math.hypot(bx - ax, by - ay) or 1
        t, on = 0.0, True
        while t < 1:
            step = (dash if on else gap) / length
            t1 = min(t + step, 1)
            if on:
                d.line(
                    [
                        s(ax + (bx - ax) * t), s(ay + (by - ay) * t),
                        s(ax + (bx - ax) * t1), s(ay + (by - ay) * t1),
                    ],
                    fill=BOUNDARY, width=max(2, SCALE),
                )
            t, on = t1, not on

    seg(x1, y1, x2, y1)
    seg(x2, y1, x2, y2)
    seg(x2, y2, x1, y2)
    seg(x1, y2, x1, y1)


def arrow_head(tip, angle, size=9):
    spread = math.radians(22)
    a1, a2 = angle + math.pi - spread, angle + math.pi + spread
    d.polygon(
        [
            (s(tip[0]), s(tip[1])),
            (s(tip[0] + size * math.cos(a1)), s(tip[1] + size * math.sin(a1))),
            (s(tip[0] + size * math.cos(a2)), s(tip[1] + size * math.sin(a2))),
        ],
        fill=ARROW,
    )


LW = max(2, SCALE)


def hline(x0, y, x1, headed=False):
    d.line([s(x0), s(y), s(x1), s(y)], fill=ARROW, width=LW)
    if headed:
        arrow_head((x1, y), 0 if x1 >= x0 else math.pi)


def vline(x, y0, y1, headed=False):
    d.line([s(x), s(y0), s(x), s(y1)], fill=ARROW, width=LW)
    if headed:
        arrow_head((x, y1), math.pi / 2 if y1 >= y0 else -math.pi / 2)


def label(x, y, text, anchor="mm"):
    d.text((s(x), s(y)), text, font=f_label, fill=INK, anchor=anchor)


def port(b, side, t=0.5):
    if side == "right":
        return b["x"] + b["w"], b["y"] + b["h"] * t
    if side == "left":
        return b["x"], b["y"] + b["h"] * t
    if side == "top":
        return b["x"] + b["w"] * t, b["y"]
    return b["x"] + b["w"] * t, b["y"] + b["h"]


# --------------------------------------------------------------------------- title
text_c(W / 2, 16, "Thaika.co — Component Diagram", f_head)
text_c(W / 2, 34, "[C4 Model — Level 3]", f_sub)

PW, PH = 150, 90
PX = 18
client = node(PX, 70, PW, PH, "Client", "[Person]", "Post jobs, pay, review.", PERSON, "person")
contractor = node(PX, 210, PW, PH, "Contractor", "[Person]", "Bid, tasks, materials.", PERSON, "person")
guest = node(PX, 350, PW, PH, "Guest Visitor", "[Person]", "Landing and browse.", PERSON, "person")

BX1, BY1, BX2, BY2 = 210, 48, 840, 560
dashed_rect(BX1, BY1, BX2, BY2)
d.text(
    (s((BX1 + BX2) / 2), s(BY1 + 10)),
    "Thaika.co Mobile App   [Container: Android / Java]",
    font=f_bound, fill="#334455", anchor="ma",
)

CW, CH = 124, 108
GAP_X, GAP_Y = 100, 60
X1, X2, X3 = 226, 226 + CW + GAP_X, 226 + 2 * (CW + GAP_X)
Y1, Y2, Y3 = 88, 88 + CH + GAP_Y, 88 + 2 * (CH + GAP_Y)

accounts = node(X1, Y1, CW, CH, "Accounts & Auth", "«component»",
                "Signup, Firebase Auth.", COMP)
profiles = node(X2, Y1, CW, CH, "Profiles", "«component»",
                "Client / contractor fields.", COMP)
payments = node(X3, Y1, CW, CH, "Payments & Contracts", "«component»",
                "Deposit, PDF, email.", COMP)

discovery = node(X1, Y2, CW, CH, "Discovery & Search", "«component»",
                 "Directory, open jobs.", COMP)
jobs = node(X2, Y2, CW, CH, "Jobs & Bidding", "«component»",
            "Post, bid, assign.", COMP)
chat = node(X3, Y2, CW, CH, "Chat & Notifications", "«component»",
            "Threads and FCM.", COMP)

project = node(X1, Y3, CW, CH, "Project Work", "«component»",
               "Tasks and materials.", COMP)
reviews = node(X2, Y3, CW, CH, "Reviews", "«component»",
               "Ratings and replies.", COMP)
ai = node(X3, Y3, CW, CH, "AI Assistant", "«component»",
          "Gemini estimates, drafts.", COMP)

EW, EH = 178, 80
EX = 930
ext_gap = 10


def ext_at(i, title, tech, desc, fill=EXT, shape="box"):
    return node(EX, 56 + i * (EH + ext_gap), EW, EH, title, tech, desc, fill, shape)


fb_auth = ext_at(0, "Firebase Auth", "[Software System]", "Email sign-in.")
email = ext_at(1, "Email / SMTP", "[Software System]", "Contract notices.")
firestore = ext_at(2, "Cloud Firestore", "[Cloud Firestore]", "Users, jobs, bids.", DB, "db")
storage = ext_at(3, "Firebase Storage", "[Cloud Storage]", "Photos and PDFs.", DB, "db")
fcm = ext_at(4, "Cloud Messaging", "[Software System]", "Push alerts.")
gemini = ext_at(5, "Gemini API", "[Software System]", "Estimates, contracts.")

LEFT_BUS = PX + PW + 18
RIGHT_BUS = BX2 + 20

acc_in = port(accounts, "left", 0.50)
user_ys = [p["y"] + p["h"] / 2 for p in (client, contractor, guest)]
for who, y in zip((client, contractor, guest), user_ys):
    hline(who["x"] + who["w"], y, LEFT_BUS)
vline(LEFT_BUS, min(user_ys), max(user_ys))
hline(LEFT_BUS, acc_in[1], acc_in[0], headed=True)
label((LEFT_BUS + acc_in[0]) / 2, acc_in[1] - 13, "Uses")


def row(a, b, lbl):
    p, q = port(a, "right"), port(b, "left")
    hline(p[0], p[1], q[0], headed=True)
    gx = (a["x"] + a["w"] + b["x"]) / 2
    label(gx, p[1] - 16, lbl)


def col(a, b, lbl, t=0.42):
    p, q = port(a, "bottom", t), port(b, "top", t)
    vline(p[0], p[1], q[1], headed=True)
    gy = (a["y"] + a["h"] + b["y"]) / 2
    label(p[0] + 10, gy, lbl, anchor="lm")


row(accounts, profiles, "Profile")
row(profiles, payments, "Hire")
row(discovery, jobs, "Post")
row(jobs, chat, "Contact")
row(project, reviews, "Complete")
row(reviews, ai, "Facts")
col(accounts, discovery, "Auth")
col(discovery, project, "Assign")
col(chat, ai, "Ask", 0.42)

for box, t in ((payments, 0.50), (chat, 0.50), (ai, 0.50)):
    sx, sy = port(box, "right", t)
    hline(sx, sy, RIGHT_BUS)

externals = [fb_auth, email, firestore, storage, fcm, gemini]
ext_lbl = ["Sign-in", "Contract mail", "Documents", "Upload files",
           "Push", "Prompt / draft"]
ys = [e["y"] + e["h"] / 2 for e in externals]
vline(RIGHT_BUS, min(ys), max(ys))
for ext, lbl, y in zip(externals, ext_lbl, ys):
    hline(RIGHT_BUS, y, ext["x"], headed=True)
    label((RIGHT_BUS + ext["x"]) / 2, y - 12, lbl)

lgx, lgy = 18, 680
d.rounded_rectangle(
    [s(lgx), s(lgy), s(lgx + 700), s(lgy + 38)],
    radius=s(6), outline="#888888", width=SCALE,
)
for i, (fill, name) in enumerate((
    (PERSON, "Person"),
    (COMP, "Component"),
    (EXT, "External system"),
    (DB, "Database"),
)):
    x = lgx + 16 + i * 170
    d.rounded_rectangle([s(x), s(lgy + 9), s(x + 22), s(lgy + 28)], radius=s(3), fill=fill)
    d.text((s(x + 30), s(lgy + 19)), name, font=f_leg, fill=INK, anchor="lm")

text_c(W / 2, 738, "Component View: Thaika.co Mobile App   ·   C4 model", f_foot)

img.save(OUT, dpi=(DPI, DPI))
print(
    "Saved", OUT, img.size,
    "%.1f x %.1f in at %d dpi" % (img.size[0] / DPI, img.size[1] / DPI, DPI),
)
