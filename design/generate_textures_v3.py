from __future__ import annotations

from pathlib import Path
import random

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "src/main/resources/assets/meks/textures/block/exchange_switch"
DESIGN_DIR = ROOT / "design"

PALETTE = {
    "outline": (16, 20, 21), "body": (59, 65, 68), "body_dark": (51, 56, 59),
    "body_light": (68, 74, 78), "highlight": (87, 94, 98),
    "inner_highlight": (70, 77, 80), "inner": (45, 50, 52),
    "recess": (34, 39, 41), "shadow": (32, 37, 39), "vent": (20, 25, 27),
    "screen": (8, 13, 14), "glass": (18, 44, 48), "glass_dim": (13, 30, 33),
    "cyan": (55, 215, 168), "cyan_dark": (24, 83, 75),
    "orange": (255, 119, 26), "orange_light": (255, 170, 88),
    "orange_dim": (103, 59, 32), "core_idle": (59, 37, 22),
    "core_active": (255, 210, 86), "lamp_idle": (34, 65, 59),
    "frame_dark": (17, 22, 23), "rivet": (98, 105, 108),
    "rivet_shadow": (29, 34, 36),
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
            if value < 0.17:
                px(draw, x, y, "body_dark")
            elif value < 0.30:
                px(draw, x, y, "body_light")
    draw.rectangle((0, 0, 15, 15), outline=PALETTE["outline"], width=1)
    bevel(draw)
    return image, draw

def make_front(active):
    image, draw = new_texture(3101 if active else 3100)
    draw.line((2, 2, 13, 2), fill=PALETTE["inner_highlight"])
    for x1 in (3, 7, 11):
        rect(draw, x1, 2, x1 + 2, 2, "vent")
        for x in range(x1, x1 + 3):
            px(draw, x, 3, "shadow")

    rect(draw, 2, 4, 13, 13, "recess")
    draw.line((3, 4, 12, 4), fill=PALETTE["inner_highlight"])
    draw.line((3, 4, 3, 13), fill=PALETTE["inner_highlight"])
    draw.line((3, 13, 12, 13), fill=PALETTE["frame_dark"])
    draw.line((12, 4, 12, 13), fill=PALETTE["frame_dark"])
    rect(draw, 3, 5, 12, 12, "screen")

    for offset in range(8):
        x, y = 4 + offset, 11 - offset
        if 3 <= x <= 12 and 5 <= y <= 12:
            px(draw, x, y, "glass" if offset in (2, 3, 4) else "glass_dim")

    ring = "orange" if active else "orange_dim"
    cardinal = "orange_light" if active else "orange_dim"
    core = "core_active" if active else "core_idle"
    lamp = "cyan" if active else "lamp_idle"
    ring_points = {
        (4, 7), (4, 8), (4, 9), (7, 5), (8, 5), (9, 5),
        (11, 7), (11, 8), (11, 9), (7, 12), (8, 12), (9, 12),
        (5, 6), (6, 6), (9, 6), (10, 6),
        (5, 10), (6, 10), (9, 10), (10, 10),
    }
    cardinal_points = {(4, 8), (8, 5), (11, 8), (8, 12)}
    for x, y in ring_points:
        px(draw, x, y, cardinal if (x, y) in cardinal_points else ring)
    rect(draw, 7, 7, 8, 8, core)

    for x, y in ((4, 11), (7, 10), (11, 11)):
        px(draw, x, y, lamp)
        if active:
            px(draw, x + 1, y, "cyan_dark")

    draw.line((2, 13, 13, 13), fill=PALETTE["body_dark"])
    px(draw, 1, 3, "body_dark")
    px(draw, 14, 3, "body_dark")
    return image

def make_side():
    image, draw = new_texture(3200)
    for y in range(2, 14, 2):
        rect(draw, 2, y, 8, y, "vent")
        draw.line((2, y + 1, 8, y + 1), fill=PALETTE["body_light"])

    rect(draw, 10, 3, 12, 12, "recess")
    border(draw, 10, 3, 12, 12, "frame_dark")
    draw.line((10, 3, 12, 3), fill=PALETTE["inner_highlight"])
    for index, y in enumerate(range(5, 12)):
        rect(draw, 11, y, 11, y, "cyan" if index in (1, 4, 6) else "cyan_dark")

    for y in (3, 11):
        rect(draw, 13, y, 13, y + 1, "rivet")
        px(draw, 13, y + 2, "rivet_shadow")
    return image

def make_back():
    image, draw = new_texture(3300)
    rect(draw, 3, 2, 12, 13, "inner")
    border(draw, 3, 2, 12, 13, "frame_dark")
    draw.line((3, 2, 12, 2), fill=PALETTE["inner_highlight"])
    draw.line((3, 2, 3, 13), fill=PALETTE["inner_highlight"])
    rect(draw, 7, 3, 8, 12, "vent")
    draw.line((9, 3, 9, 12), fill=PALETTE["body_light"])
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        rect(draw, x, y, x, y + 1, "rivet")
        px(draw, x, y + 2, "rivet_shadow")
    return image

def make_top():
    image, draw = new_texture(3400)
    rect(draw, 2, 2, 13, 13, "inner")
    draw.line((2, 2, 13, 2), fill=PALETTE["inner_highlight"])
    draw.line((2, 2, 2, 13), fill=PALETTE["inner_highlight"])
    draw.line((2, 13, 13, 13), fill=PALETTE["frame_dark"])
    draw.line((13, 2, 13, 13), fill=PALETTE["frame_dark"])
    for x1 in (4, 9):
        for y1 in (5, 9):
            rect(draw, x1, y1, x1 + 2, y1 + 1, "vent")
            for x in range(x1, x1 + 3):
                px(draw, x, y1 + 2, "body_dark")
    return image

def make_bottom():
    image, draw = new_texture(3500)
    rect(draw, 2, 2, 13, 13, "inner")
    draw.line((2, 2, 13, 2), fill=PALETTE["inner_highlight"])
    draw.line((2, 2, 2, 13), fill=PALETTE["inner_highlight"])
    draw.line((2, 13, 13, 13), fill=PALETTE["frame_dark"])
    draw.line((13, 2, 13, 13), fill=PALETTE["frame_dark"])
    draw.line((4, 4, 11, 4), fill=PALETTE["recess"])
    draw.line((4, 11, 11, 11), fill=PALETTE["recess"])
    draw.line((4, 4, 4, 11), fill=PALETTE["recess"])
    draw.line((11, 4, 11, 11), fill=PALETTE["recess"])
    draw.line((5, 5, 10, 10), fill=PALETTE["vent"])
    draw.line((5, 10, 10, 5), fill=PALETTE["vent"])
    return image

def save(image, name):
    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    image.save(ASSET_DIR / name, optimize=True)

def make_preview(textures):
    scale = 9
    width, height = 16 * scale, 16 * scale
    sheet = Image.new("RGBA", (width * 4, height), (24, 25, 27, 255))
    for index, name in enumerate(("front", "front_active", "side", "top")):
        texture = textures[name].resize((width, height), Image.Resampling.NEAREST)
        sheet.alpha_composite(texture, (index * width, 0))
    sheet.save(DESIGN_DIR / "preview_v3.png", optimize=True)

def main():
    textures = {
        "front": make_front(False), "front_active": make_front(True),
        "side": make_side(), "back": make_back(),
        "top": make_top(), "bottom": make_bottom(),
    }
    for name, image in textures.items():
        save(image, f"{name}.png")
    make_preview(textures)
    print(f"Generated {len(textures)} textures in {ASSET_DIR}")

if __name__ == "__main__":
    main()
