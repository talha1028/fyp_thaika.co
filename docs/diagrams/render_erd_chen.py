# Builds the Thaika.co ERD in Chen notation (entity boxes, attribute ellipses,
# relationship diamonds) as six DOT files and renders them via Kroki.
# Style matches Lawway (greyscale boxes / diamonds, white ovals).
import os
import re
import time

import requests

HERE = os.path.dirname(os.path.abspath(__file__))
KROKI = "https://kroki.io/graphviz/png"

ENTITY = dict(shape="box", style="filled", fillcolor="#D9D9D9", color="#000000",
              fontcolor="#000000", margin="0.18,0.12")
ATTR = dict(shape="ellipse", style="filled", fillcolor="#FFFFFF", color="#000000",
            fontcolor="#000000", margin="0.04,0.02")
REL = dict(shape="diamond", style="filled", fillcolor="#D9D9D9", color="#000000",
           fontcolor="#000000", margin="0.05,0.02")
NOTE = dict(shape="box", style="filled", fillcolor="#BDBDBD", color="#000000",
            fontcolor="#000000")

OMIT_NOTE = ("epoch timestamps are omitted from\\nall entities on this diagram for space -\\n"
             "documents store created/updated millis.")

PARTS = [
    {
        "title": "Accounts & Notifications",
        "entities": [
            ("User", ["*userId", "email (unique)", "fullName", "phoneNumber",
                      "city", "userType", "phoneVerified", "fcmToken",
                      "category", "hourlyRate", "rating", "totalSpent"]),
            ("Notification", ["*notificationId", "userId (FK)", "title",
                              "type", "relatedId", "isRead"]),
        ],
        "rels": [
            ("User", "receives", "Notification", "1", "N"),
        ],
        "notes": [
            ("User", "-> continues in\\nPart 2 (Jobs)"),
        ],
        "global_note": True,
    },
    {
        "title": "Jobs & Bidding",
        "entities": [
            ("Job", ["*jobId", "clientId (FK)", "title", "category", "budget",
                     "timeline", "location", "status", "acceptedBidId (FK)",
                     "assignedContractorId (FK)"]),
            ("Bid", ["*bidId", "jobId (FK)", "contractorId (FK)", "bidAmount",
                     "completionDays", "proposal", "status"]),
            ("User~", ["*userId", "userType", "fullName"]),
        ],
        "rels": [
            ("User~", "posts", "Job", "1", "N"),
            ("User~", "assigned to", "Job", "1", "N"),
            ("Job", "attracts", "Bid", "1", "N"),
            ("User~", "submits", "Bid", "1", "N"),
            ("Job", "accepts", "Bid", "1", "1"),
        ],
        "notes": [
            ("User~", "-> shown in full in\\nPart 1 (Accounts)"),
            ("Job", "-> continues in\\nPart 3 (Contracts)"),
        ],
    },
    {
        "title": "Contracts & Payments",
        "entities": [
            ("Contract", ["*contractId", "jobId (FK)", "clientId (FK)",
                          "contractorId (FK)", "pdfUrl", "clientEmailSent"]),
            ("Payment", ["*paymentId", "jobId (FK)", "clientId (FK)",
                         "contractorId (FK)", "amount", "paymentType",
                         "paymentMethod", "transactionRef", "status"]),
            ("Mail", ["*id", "to (array)", "subject", "html"]),
            ("Job~", ["*jobId", "status"]),
            ("User~", ["*userId", "email"]),
        ],
        "rels": [
            ("Job~", "generates", "Contract", "1", "1"),
            ("Job~", "settled by", "Payment", "1", "N"),
            ("User~", "pays", "Payment", "1", "N"),
            ("Contract", "emailed as", "Mail", "1", "N"),
        ],
        "notes": [
            ("Job~", "-> shown in full in\\nPart 2 (Jobs)"),
            ("Payment", "deposit 30% / final 70%"),
        ],
    },
    {
        "title": "Tasks & Materials",
        "entities": [
            ("Task", ["*taskId", "jobId (FK)", "taskTitle", "assignedTo",
                      "status", "progressPercentage", "totalCost",
                      "createdBy (FK)"]),
            ("Material", ["*materialId", "jobId (FK)", "materialName",
                          "category", "quantity", "unit", "unitPrice",
                          "supplier", "status", "addedBy (FK)"]),
            ("Job~", ["*jobId", "title"]),
            ("User~", ["*userId", "userType"]),
        ],
        "rels": [
            ("Job~", "tracks", "Task", "1", "N"),
            ("Job~", "consumes", "Material", "1", "N"),
            ("User~", "creates", "Task", "1", "N"),
            ("User~", "adds", "Material", "1", "N"),
        ],
        "notes": [
            ("Job~", "-> shown in full in\\nPart 2 (Jobs)"),
        ],
    },
    {
        "title": "Chat Messaging",
        "entities": [
            ("Message", ["*messageId", "chatId", "senderId (FK)",
                         "receiverId (FK)", "messageText", "messageType",
                         "attachmentUrl", "isRead"]),
            ("User~", ["*userId", "fullName"]),
        ],
        "rels": [
            ("User~", "sends", "Message", "1", "N"),
            ("User~", "receives", "Message", "1", "N"),
        ],
        "notes": [
            ("User~", "-> shown in full in\\nPart 1 (Accounts)"),
            ("Message", "chatId groups a\\nclient-contractor thread"),
        ],
    },
    {
        "title": "Reviews & Ratings",
        "entities": [
            ("Review", ["*reviewId", "jobId (FK)", "clientId (FK)",
                        "contractorId (FK)", "rating", "reviewText",
                        "isVerified", "response"]),
            ("Job~", ["*jobId", "status"]),
            ("User~", ["*userId", "userType", "rating"]),
        ],
        "rels": [
            ("Job~", "rated by", "Review", "1", "1"),
            ("User~", "writes", "Review", "1", "N"),
            ("User~", "receives", "Review", "1", "N"),
        ],
        "notes": [
            ("Job~", "-> shown in full in\\nPart 2 (Jobs)"),
            ("User~", "-> shown in full in\\nPart 1 (Accounts)"),
        ],
    },
]


def nid(*bits):
    raw = "_".join(bits)
    return "n_" + re.sub(r"[^A-Za-z0-9]+", "_", raw).strip("_").lower()


def attrs(d, **extra):
    merged = dict(d)
    merged.update(extra)
    return ", ".join('%s="%s"' % (k, v) for k, v in merged.items())


def build_dot(part, index, total):
    title = "Part %d of %d - %s" % (index, total, part["title"])
    out = ["graph ERD {"]
    out.append('  graph [layout=neato, mode=KK, overlap=prism, splines=line, '
               'sep="+0.4", esep="+0.2", maxiter=8000, epsilon=0.0001, '
               'labelloc="t", fontname="Arial Bold", fontsize=18, '
               'label="%s", bgcolor="white"];' % title)
    out.append('  node [fontname="Arial", fontsize=12, penwidth=1.4];')
    out.append('  edge [fontname="Arial", fontsize=11, color="#000000", penwidth=1.1];')

    for name, attributes in part["entities"]:
        clean = name.rstrip("~")
        e = nid("e", clean)
        out.append('  %s [label="%s", %s];' % (e, clean, attrs(ENTITY, fontsize=16)))
        for a in attributes:
            is_pk = a.startswith("*")
            text = a.lstrip("*")
            a_id = nid("a", clean, text)
            label = ("<<u>%s</u>>" % text) if is_pk else '"%s"' % text
            out.append('  %s [label=%s, %s];' % (a_id, label, attrs(ATTR, fontsize=11)))
            out.append('  %s -- %s [len=0.38, weight=40];' % (e, a_id))

    for i, (left, verb, right, lcard, rcard) in enumerate(part["rels"]):
        r = nid("r", str(i), verb)
        out.append('  %s [label="%s", %s];' % (r, verb, attrs(REL, fontsize=12)))
        out.append('  %s -- %s [label="%s", len=1.55, weight=2, penwidth=1.4];'
                   % (nid("e", left.rstrip("~")), r, lcard))
        out.append('  %s -- %s [label="%s", len=1.55, weight=2, penwidth=1.4];'
                   % (r, nid("e", right.rstrip("~")), rcard))

    for i, (target, text) in enumerate(part.get("notes", [])):
        n = nid("note", str(i))
        out.append('  %s [label="%s", %s, fontsize=10];' % (n, text, attrs(NOTE)))
        out.append('  %s -- %s [style=invis, len=0.7, weight=8];'
                   % (nid("e", target.rstrip("~")), n))

    if part.get("global_note"):
        out.append('  n_omitnote [label="%s", %s, fontsize=10];' % (OMIT_NOTE, attrs(NOTE)))
        out.append('  %s -- n_omitnote [style=invis, len=1.1, weight=2];'
                   % nid("e", part["entities"][0][0].rstrip("~")))

    out.append("}")
    return "\n".join(out)


def render(source, out_path, attempts=4):
    last = None
    for n in range(attempts):
        try:
            r = requests.post(KROKI, data=source.encode("utf-8"),
                              headers={"Content-Type": "text/plain"}, timeout=180)
            r.raise_for_status()
            if not r.content.startswith(b"\x89PNG"):
                raise RuntimeError("not a PNG (%d bytes)" % len(r.content))
            with open(out_path, "wb") as f:
                f.write(r.content)
            return len(r.content)
        except Exception as e:
            last = e
            time.sleep(4 * (n + 1))
    raise last


def main():
    import sys

    wanted = {int(a) for a in sys.argv[1:]} or set(range(1, len(PARTS) + 1))
    for i, part in enumerate(PARTS, start=1):
        if i not in wanted:
            continue
        dot = build_dot(part, i, len(PARTS))
        dot_path = os.path.join(HERE, "thaika-erd-chen-part%d.dot" % i)
        png_path = dot_path[:-4] + ".png"
        with open(dot_path, "w", encoding="utf-8") as f:
            f.write(dot + "\n")
        try:
            size = render(dot, png_path)
            print("OK   %-34s %7d bytes" % (os.path.basename(png_path), size))
        except Exception as e:
            print("FAIL %-34s %s" % (os.path.basename(png_path), e))


if __name__ == "__main__":
    main()
