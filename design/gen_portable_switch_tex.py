#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generate the 16x16 item texture for the Portable Exchange Switch:
base = Mekanism's portable_qio_dashboard.png (shell/bezels/LEDs kept),
screen area re-colored to a dark display, with an up-arrow (left)
and down-arrow (right) drawn in the Channel Upgrade green (0x42FF9C).
Also generates the 176x280 GUI background texture in Mekanism base.png
style (light gray panel, white top highlight, black outer frame).

Place portable_qio_dashboard.png next to this script (extract it from the
Mekanism jar: assets/mekanism/textures/item/portable_qio_dashboard.png).
"""
import os
import struct
import sys
import zlib

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SRC_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, "..", "src", "main", "resources"))


def read_png(path):
    """Decode PNG to RGBA pixels (supports color types 0/2/3/6)."""
    with open(path, "rb") as f:
        data = f.read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos = 8
    w = h = None
    idat = b""
    palette = None
    trns = None
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        ctype = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        if ctype == b"IHDR":
            w, h, depth, color, comp, filt, inter = struct.unpack(">IIBBBBB", chunk)
            assert depth == 8 and color in (0, 2, 3, 6) and comp == 0 and filt == 0 and inter == 0
        elif ctype == b"PLTE":
            palette = [tuple(chunk[i:i + 3]) for i in range(0, len(chunk), 3)]
        elif ctype == b"tRNS":
            trns = bytes(chunk)
        elif ctype == b"IDAT":
            idat += chunk
        elif ctype == b"IEND":
            break
        pos += 12 + length
    raw = zlib.decompress(idat)
    if color == 3:
        bpp_in, bpp = 1, 4
    elif color == 2:
        bpp_in, bpp = 3, 4
    else:
        bpp_in = bpp = {0: 1, 6: 4}[color]
    stride_in = w * bpp_in
    out = bytearray()
    prev = bytearray(stride_in)
    for y in range(h):
        ftype = raw[y * (stride_in + 1)]
        line = bytearray(raw[y * (stride_in + 1) + 1:(y + 1) * (stride_in + 1)])
        for i in range(stride_in):
            a = line[i - bpp_in] if i >= bpp_in else 0
            b = prev[i]
            c = prev[i - bpp_in] if i >= bpp_in else 0
            if ftype == 1:
                line[i] = (line[i] + a) & 0xFF
            elif ftype == 2:
                line[i] = (line[i] + b) & 0xFF
            elif ftype == 3:
                line[i] = (line[i] + ((a + b) >> 1)) & 0xFF
            elif ftype == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        for i in range(0, stride_in, bpp_in):
            if color == 3:
                idx = line[i]
                r, g, b = palette[idx]
                alpha = trns[idx] if trns is not None and idx < len(trns) else 255
            elif color == 2:
                r, g, b = line[i], line[i + 1], line[i + 2]
                alpha = trns[0] if trns is not None else 255
            elif color == 6:
                r, g, b, alpha = line[i], line[i + 1], line[i + 2], line[i + 3]
            else:
                r, g, b, alpha = line[i], line[i], line[i], trns[0] if trns is not None else 255
            out += bytes((r, g, b, alpha))
        prev = line
    return w, h, 4, 4, out


def write_png(path, w, h, bpp, pixels):
    def chunk(ctype, payload):
        c = ctype + payload
        return struct.pack(">I", len(payload)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)
    color_type = {1: 0, 3: 2, 4: 6}[bpp]
    stride = w * bpp
    idat = b""
    for y in range(h):
        line = bytes(pixels[y * stride:(y + 1) * stride])
        idat += b"\x00" + line  # filter type 0 (None)
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, color_type, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(idat, 9))
    data += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(data)


def main():
    # ============ 1. item texture ============
    base = os.path.join(SCRIPT_DIR, "portable_qio_dashboard.png")
    if not os.path.exists(base):
        mek_dir = os.environ.get("MEK_ASSET_DIR", "")
        base = os.path.join(mek_dir, "portable_qio_dashboard.png") if mek_dir else base
    W, H, color, bpp, px = read_png(base)
    assert (W, H) == (16, 16)

    def set(x, y, rgba):
        i = (y * W + x) * bpp
        px[i:i + 4] = bytes(rgba)

    SCREEN_BG = (0x1F, 0x3E, 0x33, 0xFF)       # dark teal display
    SCREEN_TOP = (0x35, 0x6B, 0x57, 0xFF)      # lit rim of the display
    ARROW = (0x42, 0xFF, 0x9C, 0xFF)           # Channel Upgrade green
    ARROW_EDGE = (0x2E, 0xB8, 0x72, 0xFF)      # darker green outline

    # The original dashboard screen occupies x4..x10, y6..y10. Re-color it to a
    # dark display with a bright top rim.
    for y in range(6, 11):
        for x in range(4, 11):
            set(x, y, SCREEN_BG)
    for x in range(4, 11):
        set(x, 6, SCREEN_TOP)

    # Arrows in Channel-Upgrade style: up arrow (left), down arrow (right),
    # each 3 wide x 4 tall, y7..y10.
    def arrow_up(xc, x0, y0):
        rows = {0: [xc], 1: [xc - 1, xc, xc + 1], 2: [xc], 3: [xc]}
        outline = {0: [xc], 1: [xc - 1, xc + 1], 2: [xc], 3: [xc]}
        for r, cols in outline.items():
            for xx in cols:
                set(x0 + xx, y0 + r, ARROW_EDGE)
        for r, cols in rows.items():
            for xx in cols:
                if (r, xx) not in [(0, xc), (2, xc), (3, xc)]:
                    set(x0 + xx, y0 + r, ARROW)
        set(x0 + xc, y0, ARROW)
        set(x0 + xc, y0 + 2, ARROW)
        set(x0 + xc, y0 + 3, ARROW)

    def arrow_down(xc, x0, y0):
        rows = {0: [xc], 1: [xc], 2: [xc - 1, xc, xc + 1], 3: [xc]}
        outline = {0: [xc], 1: [xc], 2: [xc - 1, xc + 1], 3: [xc]}
        for r, cols in outline.items():
            for xx in cols:
                set(x0 + xx, y0 + r, ARROW_EDGE)
        for r, cols in rows.items():
            for xx in cols:
                if (r, xx) not in [(0, xc), (1, xc), (3, xc)]:
                    set(x0 + xx, y0 + r, ARROW)
        set(x0 + xc, y0, ARROW)
        set(x0 + xc, y0 + 1, ARROW)
        set(x0 + xc, y0 + 3, ARROW)

    arrow_up(xc=1, x0=4, y0=7)      # up arrow, x4..x6
    arrow_down(xc=1, x0=8, y0=7)    # down arrow, x8..x10

    out_item = os.path.join(SRC_DIR, "assets", "meks", "textures", "item", "portable_exchange_switch.png")
    write_png(out_item, W, H, bpp, px)
    print("item texture written:", out_item)

    # ============ 2. GUI background 176x280 ============
    out_w, out_h = 176, 212
    buf = bytearray(out_w * out_h * 4)

    def gset(x, y, c):
        i = (y * out_w + x) * 4
        buf[i:i + 4] = bytes(c)

    for y in range(out_h):
        for x in range(out_w):
            gset(x, y, (0xBE, 0xBE, 0xBE, 0xFF))
    for x in range(out_w):
        gset(x, 0, (0x00, 0x00, 0x00, 0xFF))
        gset(x, 1, (0xFF, 0xFF, 0xFF, 0xFF))
        gset(x, 2, (0xFF, 0xFF, 0xFF, 0xFF))
    for y in range(out_h):
        gset(0, y, (0x00, 0x00, 0x00, 0xFF))
        gset(1, y, (0xFF, 0xFF, 0xFF, 0xFF))
    for y in range(out_h):
        gset(out_w - 2, y, (0x43, 0x43, 0x43, 0xFF))
        gset(out_w - 1, y, (0x00, 0x00, 0x00, 0xFF))
    for y in range(out_h - 4, out_h):
        t = (y - (out_h - 4)) / 3
        v = round(0xBE - (0xBE - 0x43) * t)
        for x in range(2, out_w - 2):
            gset(x, y, (v, v, v, 0xFF))

    out_gui = os.path.join(SRC_DIR, "assets", "meks", "gui", "portable_exchange_switch.png")
    write_png(out_gui, out_w, out_h, 4, buf)
    print("gui background written:", out_gui)


if __name__ == "__main__":
    sys.exit(main())