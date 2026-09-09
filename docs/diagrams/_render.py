# Render every .puml in this folder to PNG using the public PlantUML server.
import glob
import io
import os
import zlib

import requests

HERE = os.path.dirname(os.path.abspath(__file__))
SERVER = "https://www.plantuml.com/plantuml/png/"


def _encode6bit(b):
    if b < 10:
        return chr(48 + b)
    b -= 10
    if b < 26:
        return chr(65 + b)
    b -= 26
    if b < 26:
        return chr(97 + b)
    b -= 26
    return "-" if b == 0 else "_"


def plantuml_encode(text):
    """Deflate + PlantUML's base64 variant, so long diagrams fit in a URL."""
    raw = zlib.compress(text.encode("utf-8"))[2:-4]
    out = []
    for i in range(0, len(raw), 3):
        chunk = raw[i:i + 3] + b"\x00" * (3 - len(raw[i:i + 3]))
        b1, b2, b3 = chunk[0], chunk[1], chunk[2]
        out.append(_encode6bit(b1 >> 2))
        out.append(_encode6bit(((b1 & 0x3) << 4) | (b2 >> 4)))
        out.append(_encode6bit(((b2 & 0xF) << 2) | (b3 >> 6)))
        out.append(_encode6bit(b3 & 0x3F))
    return "".join(out)


def render(path):
    text = io.open(path, encoding="utf-8").read()
    url = SERVER + plantuml_encode(text)
    r = requests.get(url, timeout=90)
    r.raise_for_status()
    if not r.content.startswith(b"\x89PNG"):
        raise RuntimeError("Not a PNG for %s (%d bytes)" % (path, len(r.content)))
    out = path[:-5] + ".png"
    with open(out, "wb") as f:
        f.write(r.content)
    return out, len(r.content)


def main():
    import sys

    pattern = sys.argv[1] if len(sys.argv) > 1 else "*"
    files = sorted(glob.glob(os.path.join(HERE, "%s.puml" % pattern)))
    for f in files:
        try:
            out, size = render(f)
            print("OK   %-46s %7d bytes" % (os.path.basename(out), size))
        except Exception as e:
            print("FAIL %-46s %s" % (os.path.basename(f), e))


if __name__ == "__main__":
    main()
