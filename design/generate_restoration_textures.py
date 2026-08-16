from __future__ import annotations

from pathlib import Path
import random

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
BLOCK_DIR = ROOT / "src/main/resources/assets/meks/textures/block/restoration_switch"
GUI_DIR = ROOT / "src/main/resources/assets/meks/gui"

PALETTE = {
    "outline": (16, 20, 21),
    "body": (74, 78, 80),
    "body_dark": (62, 66, 68),
    "body_light": (88, 92, 94),
    "highlight": (104, 108, 110),
    "inner_highlight": (70, 76, 78),
    "inner": (52, 56, 58),
    "recess": (38, 42, 44),
    "shadow": (34, 38, 40),
    "vent": (24, 28, 30),
    "screen": (10, 14, 15),
    "glass": (24, 52, 56),
    "cyan": (70, 220, 200),
    "cyan_dark": (30, 96, 88),
    "green": (96, 232, 118),
    "green_dim": (40, 96, 52),
    "yellow": (255, 208, 48),
    "yellow_dim": (118, 92, 26),
    "orange": (255, 128, 40),
    "orange_dim": (120, 66, 30),
    "frame_dark": (20, 24, 25),
    "rivet": (112, 118, 120),
    "rivet_shadow": (34, 38, 40),
}

def px(draw, x, y, color):
    draw.point((x, y), fill=PALETTE[color])

def rect(draw, x1, y1, x2, y2, color):
    draw.rectangle((x1, y1, x2, y2), fill=PALETTE[color])

def border(draw, x1, y1, x2, y2, color):
    draw.rectangle((x1, y1, x2, y2), outline=PALETTE[color], width=1)

def bevel(draw):
    draw.line((1, 1, 14, 1), fill=PALETTE["highlight"])
    draw.line((1, 1, 1, 14), fill=PALETTE["highlight"])
    draw.line((1, 14, 14, 14), fill=PALETTE["shadow"])
    draw.line((14, 1, 14, 14), fill=PALETTE["shadow"])

def new_texture(seed):
    image = Image.new("RGBA", (16, 16), PALETTE["body"])
    draw = ImageDraw.Draw(image)
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            value = rng.random()
            if value < 0.14:
                px(draw, x, y, "body_dark")
            elif value < 0.26:
                px(draw, x, y, "body_light")
    draw.rectangle((0, 0, 15, 15), outline=PALETTE["outline"], width=1)
    bevel(draw)
    return image, draw

def make_front(active):
    image, draw = new_texture(4101 if active else 4100)
    rect(draw, 4, 2, 11, 2, "vent")
    for x in range(4, 12):
        px(draw, x, 3, "shadow")
    rect(draw, 3, 4, 12, 8, "recess")
    border(draw, 3, 4, 12, 8, "frame_dark")
    draw.line((3, 4, 12, 4), fill=PALETTE["inner_highlight"])
    for x, y in ((4, 7), (5, 6), (6, 7), (7, 5), (8, 6), (9, 5), (10, 6), (11, 5)):
        px(draw, x, y, "vent")
    px(draw, 10, 5, "cyan" if active else "cyan_dark")
    if active:
        px(draw, 11, 5, "cyan_dark")
    rect(draw, 2, 11, 13, 14, "frame_dark")
    rect(draw, 3, 12, 4, 13, "green" if active else "green_dim")
    rect(draw, 6, 12, 7, 13, "yellow" if active else "yellow_dim")
    for y in range(11, 15):
        for x in range(9, 14):
            if (x + y) % 2 == 0:
                px(draw, x, y, "orange" if active else "orange_dim")
    draw.line((2, 13, 13, 13), fill=PALETTE["body_dark"])
    return image

def make_side():
    image, draw = new_texture(4200)
    for y in range(2, 12, 2):
        rect(draw, 2, y, 7, y, "vent")
        draw.line((2, y + 1, 7, y + 1), fill=PALETTE["body_light"])
    rect(draw, 9, 3, 12, 10, "recess")
    border(draw, 9, 3, 12, 10, "frame_dark")
    for y in (4, 6, 8):
        rect(draw, 10, y, 11, y, "cyan" if y == 6 else "cyan_dark")
    rect(draw, 2, 13, 13, 15, "frame_dark")
    return image

def make_back():
    image, draw = new_texture(4300)
    rect(draw, 3, 2, 12, 13, "inner")
    border(draw, 3, 2, 12, 13, "frame_dark")
    draw.line((3, 2, 12, 2), fill=PALETTE["inner_highlight"])
    draw.line((3, 2, 3, 13), fill=PALETTE["inner_highlight"])
    rect(draw, 6, 4, 7, 11, "vent")
    rect(draw, 9, 4, 10, 11, "vent")
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        rect(draw, x, y, x, y + 1, "rivet")
        px(draw, x, y + 2, "rivet_shadow")
    return image

def make_top():
    image, draw = new_texture(4400)
    rect(draw, 2, 2, 13, 13, "inner")
    border(draw, 2, 2, 13, 13, "frame_dark")
    draw.line((2, 2, 13, 2), fill=PALETTE["inner_highlight"])
    draw.line((2, 2, 2, 13), fill=PALETTE["inner_highlight"])
    for x1, y1 in ((4, 5), (9, 5), (4, 9), (9, 9)):
        rect(draw, x1, y1, x1 + 1, y1 + 1, "vent")
    return image

def make_bottom():
    image, draw = new_texture(4500)
    rect(draw, 2, 2, 13, 13, "inner")
    border(draw, 2, 2, 13, 13, "frame_dark")
    draw.line((4, 4, 11, 4), fill=PALETTE["recess"])
    draw.line((4, 11, 11, 11), fill=PALETTE["recess"])
    draw.line((4, 4, 4, 11), fill=PALETTE["recess"])
    draw.line((11, 4, 11, 11), fill=PALETTE["recess"])
    draw.line((5, 5, 10, 10), fill=PALETTE["vent"])
    draw.line((5, 10, 10, 5), fill=PALETTE["vent"])
    return image

def make_gui():
    image = Image.new("RGBA", (176, 200), (198, 198, 198, 255))
    draw = ImageDraw.Draw(image)
    draw.line((0, 0, 175, 0), fill=(255, 255, 255, 255))
    draw.line((0, 0, 0, 199), fill=(255, 255, 255, 255))
    draw.line((0, 199, 175, 199), fill=(72, 72, 72, 255))
    draw.line((175, 0, 175, 199), fill=(72, 72, 72, 255))
    draw.rectangle((3, 3, 172, 112), outline=(120, 120, 120, 255))
    draw.line((4, 4, 171, 4), fill=(255, 255, 255, 255))
    draw.line((4, 4, 4, 111), fill=(255, 255, 255, 255))
    draw.line((4, 111, 171, 111), fill=(72, 72, 72, 255))
    draw.line((171, 4, 171, 111), fill=(72, 72, 72, 255))
    return image

def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)

def main():
    textures = {
        "front": make_front(False),
        "front_active": make_front(True),
        "side": make_side(),
        "back": make_back(),
        "top": make_top(),
        "bottom": make_bottom(),
    }
    for name, image in textures.items():
        save(image, BLOCK_DIR / f"{name}.png")
    save(make_gui(), GUI_DIR / "restoration_switch.png")
    print(f"Generated {len(textures)} block textures and 1 GUI background")

if __name__ == "__main__":
    main()
