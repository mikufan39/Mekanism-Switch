"""
交换机 (Exchange Switch) 方块贴图概念生成器 v3
================================================
设计目标：与 Mekanism 机器完全融合的转化桌式装置。

视觉语言（从 Mekanism 1.21.x 真实贴图提取）：
- 近黑外框 #171717，顶/左亮边、底/右暗边的倒角
- 面板为中深灰 + 低对比成簇噪点（复用真实灰阶 #28282a / #353535 / #4e4e4e）
- 功能色只用两种：橙色 = 能量/运行（Mekanism 加热元件色 #FA8B37），
  青绿 = 状态/屏幕（Mekanism 屏幕色 #66A98A）
- 待机态：暗棕环 + 空心核心 + 三颗暗灯
  运行态：亮橙环 + 亮核心 + 三颗亮灯（几何不变，只变亮度）

用法：python generate_textures.py   （需要 Pillow）
输出：exchange_front_idle.png / exchange_front_active.png /
      exchange_side.png / exchange_top.png
"""

from PIL import Image, ImageDraw
import random

# ---------------------------------------------------------------- palette
F = (23, 23, 23)        # 外框
L = (82, 82, 82)        # 顶部/左侧亮边
G = (27, 27, 27)        # 底部/右侧暗边
P0 = (43, 43, 43)       # 面板基色
P1 = (38, 38, 38)       # 噪点-暗
P2 = (53, 53, 53)       # 噪点-亮1
P3 = (62, 62, 62)       # 噪点-亮2
PL = (78, 78, 78)       # 稀疏高光点（约9%密度，对齐真实贴图）
V = (20, 20, 20)        # 通风口深槽
VL = (58, 58, 58)       # 通风口下唇高光
E = (30, 30, 30)        # 屏幕外框
D0 = (21, 21, 21)       # 屏幕底
D1 = (27, 27, 27)       # 屏幕噪点
GL = (54, 54, 54)       # 屏幕玻璃斜纹
R_ACT = (250, 139, 55)  # 运行环（Mekanism 橙）
R_MID_ACT = (217, 122, 46)
R_IDLE = (150, 105, 45)  # 待机环（暗棕橙，屏幕上有足够对比）
R_MID_IDLE = (96, 68, 30)
CORE_ACT = (255, 217, 160)  # 运行核心
LED_DIM = (70, 108, 92)     # 待机灯（暗青绿）
LED_BRIGHT = (118, 195, 160)  # 运行灯（亮青绿）

PAL = {
    'F': F, 'L': L, 'G': G, 'P': P0, 'B': P1, 'C': P2, 'H': P3, 'S': PL,
    'V': V, 'W': VL, 'E': E, 'D': D0, 'N': D1, 'T': GL,
    'R': R_ACT, 'M': R_MID_ACT, 'I': R_IDLE, 'J': R_MID_IDLE,
    'O': CORE_ACT, 'b': LED_DIM, 'a': LED_BRIGHT,
}


def noisy(rows, seed):
    """低对比成簇噪点：2x2 块级亮度变化 + 稀疏高光点。"""
    rng = random.Random(seed)
    h, w = len(rows), len(rows[0])
    out = [list(r) for r in rows]
    for y in range(h):
        for x in range(w):
            if out[y][x] != 'P':
                continue
            r = rng.random()
            if r < 0.055:
                out[y][x] = 'S'      # 高光点
            elif r < 0.095:
                out[y][x] = 'H'      # 亮噪点
            elif r < 0.14:
                out[y][x] = 'C'      # 亮噪点2
            elif r < 0.30:
                out[y][x] = 'B'      # 暗噪点
    # 2x2 块级微调：模拟成簇的金属颗粒
    for y in range(0, h - 1, 2):
        for x in range(0, w - 1, 2):
            r = rng.random()
            if r < 0.16:
                t = 'B' if r < 0.08 else 'C'
                for dy in range(2):
                    for dx in range(2):
                        if out[y + dy][x + dx] == 'P':
                            out[y + dy][x + dx] = t
    return [''.join(r) for r in out]


def put(rows, r, c, ch):
    rows[r] = rows[r][:c] + ch + rows[r][c + 1:]


def build_front(active):
    rows = [
        'FFFFFFFFFFFFFFFF',
        'FLLLLLLLLLLLLLLF',
        'FPPPPPPPPPPPPPPF',
        'FPVVPVVPVVPVVPPF',   # 4 个 2px 通风槽，1px 间隔
        'FWHHWHHWHHWHHPPF',   # 每个槽下缘亮唇  -> 用 H 填充
        'FEEEEEEEEEEEEEEF',   # 屏幕外框
        'FEDDDDDDDDDDDDEF',
        'FEDDDDDDDDDDDDEF',
        'FEDDDDDDDDDDDDEF',
        'FEDDDDDDDDDDDDEF',
        'FEDDDDDDDDDDDDEF',
        'FEDDDDDDDDDDDDEF',
        'FEEEEEEEEEEEEEEF',
        'FPPPPPPPPPPPPPPF',
        'FGGGGGGGGGGGGGGF',
        'FFFFFFFFFFFFFFFF',
    ]
    # 通风槽下唇（把 W 的亮唇换成中间灰，避免过亮）
    for c in (2, 5, 8, 11):
        put(rows, 4, c, 'H')
        put(rows, 4, c + 1, 'H')

    # 环形转化符号：rows 6-10, cols 4-9，圆角矩形环
    ring = 'R' if active else 'I'
    mid = 'M' if active else 'J'
    put(rows, 6, 4, mid); put(rows, 6, 5, ring); put(rows, 6, 6, ring)
    put(rows, 6, 7, ring); put(rows, 6, 8, ring); put(rows, 6, 9, mid)
    put(rows, 7, 4, ring); put(rows, 7, 9, ring)
    put(rows, 8, 4, ring); put(rows, 8, 9, ring)
    put(rows, 9, 4, ring); put(rows, 9, 9, ring)
    put(rows, 10, 4, mid); put(rows, 10, 5, ring); put(rows, 10, 6, ring)
    put(rows, 10, 7, ring); put(rows, 10, 8, ring); put(rows, 10, 9, mid)
    # 核心：待机空心 / 运行亮实心
    if active:
        put(rows, 8, 6, 'O'); put(rows, 8, 7, 'O')
    # 状态灯：位置固定（c5/c7/c9），只变亮暗
    led = 'a' if active else 'b'
    put(rows, 11, 5, led); put(rows, 11, 7, led); put(rows, 11, 9, led)
    # 屏幕玻璃斜纹（右下角两点）
    put(rows, 10, 12, 'T'); put(rows, 11, 11, 'T')

    rows = noisy(rows, seed=17 if not active else 23)
    return rows


def build_side():
    rows = [
        'FFFFFFFFFFFFFFFF',
        'FLLLLLLLLLLLLLLF',
        'FPPPPPPPPPPPPPPF',
        'FHHHHHHHHHHHHHPF',   # 亮色板条1
        'FVVVVVVVVVVVVVPF',   # 深色缝
        'FHHHHHHHHHHHHHPF',
        'FVVVVVVVVVVVVVPF',
        'FHHHHHHHHHHHHHPF',
        'FVVVVVVVVVVVVVPF',
        'FHHHHHHHHHHHHHPF',
        'FVVVVVVVVVVVVVPF',
        'FPPPPPPPPPPPPPPF',
        'FPPSPPPPPPPPPSPF',   # 铆钉带（两端铆钉）
        'FPPBPPPPPPPPPBPP'.replace('B', 'B') and 'FPPPPPPPPPPPPPPF',
        'FGGGGGGGGGGGGGGF',
        'FFFFFFFFFFFFFFFF',
    ]
    # 状态液柱：右侧 c13，亮→暗分段
    for y in range(2, 13):
        put(rows, y, 13, 'a' if y < 8 else 'b')
    # 铆钉：亮 + 下方阴影
    put(rows, 12, 2, 'S'); put(rows, 12, 13, 'S')
    put(rows, 13, 2, 'B'); put(rows, 13, 13, 'B')
    rows = noisy(rows, seed=31)
    return rows


def build_top():
    rows = [
        'FFFFFFFFFFFFFFFF',
        'FLLLLLLLLLLLLLLF',
        'FPPPPPPPPPPPPPPF',
        'FPPPPPPPPPPPPPPF',
        'FPPEEEEEEEEEEEPPF',
        'FPPEDDDDDDDDDDEPPF',
        'FPPEDDDDDDDDDDEPPF',
        'FPPEDVVVDDVVVDEPPF',  # 4 槽 -> 2 组 3px 槽
        'FPPEDVVVDDVVVDEPPF',
        'FPPEDDDDDDDDDDEPPF',
        'FPPEDDDDDDDDDDEPPF',
        'FPPEDDDDDDDDDDEPPF',
        'FPPEEEEEEEEEEEPPF',
        'FPPPPPPPPPPPPPPF',
        'FGGGGGGGGGGGGGGF',
        'FFFFFFFFFFFFFFFF',
    ]
    # 四个小槽位：2px 宽，1px 间隔
    for c in (4, 7, 10, 13):
        put(rows, 7, c, 'V'); put(rows, 7, c + 1, 'V')
        put(rows, 8, c, 'V'); put(rows, 8, c + 1, 'V')
    rows = noisy(rows, seed=41)
    return rows


def render(rows, scale=16):
    h, w = len(rows), len(rows[0])
    im = Image.new('RGB', (w * scale, h * scale), (0, 0, 0))
    d = ImageDraw.Draw(im)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            d.rectangle([x * scale, y * scale,
                         x * scale + scale - 1, y * scale + scale - 1],
                        fill=PAL[ch])
    return im


if __name__ == '__main__':
    out = __file__ and __file__.rsplit('/', 1)[0]
    render(build_front(False)).save(out + '/exchange_front_idle.png')
    render(build_front(True)).save(out + '/exchange_front_active.png')
    render(build_side()).save(out + '/exchange_side.png')
    render(build_top()).save(out + '/exchange_top.png')
    print('saved v3 textures ->', out)
