"""Texture atlas for the phiren staff: one 8x8 tile per material."""
from PIL import Image, ImageDraw
import phiren_model as M

T = M.TILE
W, H = M.TEX_W, M.TEX_H


def cell_box(cell):
    cx, cy = M.CELLS[cell]
    return cx * T, cy * T, (cx + 1) * T, (cy + 1) * T


def lerp(a, b, f):
    return tuple(int(round(a[i] + (b[i] - a[i]) * f)) for i in range(3))


def hgrad(d, box, left, right):
    x0, y0, x1, y1 = box
    n = x1 - x0
    for i in range(n):
        d.line([(x0 + i, y0), (x0 + i, y1 - 1)], fill=lerp(left, right, i / max(1, n - 1)))


def vgrad(d, box, top, bot):
    x0, y0, x1, y1 = box
    n = y1 - y0
    for i in range(n):
        d.line([(x0, y0 + i), (x1 - 1, y0 + i)], fill=lerp(top, bot, i / max(1, n - 1)))


def build_texture():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def tile(cell, fill):
        b = cell_box(cell)
        d.rectangle([b[0], b[1], b[2] - 1, b[3] - 1], fill=fill)
        return b

    # ---- wood: warm brown with a lighter core -----------------------------
    b = tile("wood", M.OUTLINE)
    hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.WOOD_DARK, M.WOOD)
    for i in range(b[0] + 1, b[2] - 1):
        f = (i - b[0]) / (b[2] - b[0])
        if 0.30 < f < 0.68:
            d.line([(i, b[1] + 1), (i, b[3] - 2)], fill=M.WOOD_LITE)
    d.point((b[0] + 3, b[1] + 4), fill=M.WOOD_DARK)
    b = tile("wood_side", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1), M.WOOD_DARK, M.WOOD_DEEP)
    b = tile("wood_top", M.WOOD_DARK)

    # ---- purple cloth: soft folds -----------------------------------------
    b = tile("purp", M.OUTLINE)
    hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.PURPLE_DARK, M.PURPLE)
    for i in range(b[0] + 1, b[2] - 1):
        f = (i - b[0]) / (b[2] - b[0])
        if 0.18 < f < 0.42:
            d.line([(i, b[1] + 1), (i, b[3] - 2)], fill=M.PURPLE_LITE)
    for y in (b[1] + 2, b[3] - 3):        # fold lines across the wrap
        d.line([(b[0] + 1, y), (b[2] - 2, y)], fill=M.PURPLE_DEEP)
    b = tile("purp_side", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1), M.PURPLE_DARK, M.PURPLE_DEEP)
    b = tile("purp_top", M.PURPLE_DARK)

    # ---- silver: brushed metal --------------------------------------------
    b = tile("silv", M.OUTLINE)
    hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.SILVER_DARK, M.SILVER)
    d.line([(b[0] + 2, b[1] + 1), (b[0] + 2, b[3] - 2)], fill=M.SILVER_LITE)
    d.line([(b[0] + 3, b[1] + 1), (b[0] + 3, b[3] - 2)], fill=M.SILVER_LITE)
    b = tile("silv_side", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1), M.SILVER_DARK, M.SILVER_DEEP)
    b = tile("silv_top", M.SILVER_DARK)

    # ---- cream / bone ------------------------------------------------------
    b = tile("cream", M.OUTLINE)
    hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.CREAM_DARK, M.CREAM)
    d.line([(b[0] + 3, b[1] + 1), (b[0] + 3, b[3] - 2)], fill=M.CREAM_LITE)
    d.line([(b[0] + 4, b[1] + 1), (b[0] + 4, b[3] - 2)], fill=M.CREAM_LITE)
    b = tile("cream_side", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1), M.CREAM_DARK, M.CREAM_DEEP)
    b = tile("cream_top", M.CREAM_DARK)

    # ---- red thread / bead -------------------------------------------------
    b = tile("red", M.OUTLINE)
    hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.RED_DARK, M.RED)
    d.line([(b[0] + 3, b[1] + 1), (b[0] + 3, b[3] - 2)], fill=M.RED_LITE)
    b = tile("red_side", M.RED_DARK)
    tile("red_top", M.RED_DARK)
    tile("bead", M.RED)
    b = cell_box("bead")
    d.ellipse([b[0] + 1, b[1] + 1, b[2] - 2, b[3] - 2], fill=M.RED)
    d.line([(b[0] + 2, b[1] + 2), (b[0] + 2, b[1] + 4)], fill=M.RED_LITE)
    d.point((b[0] + 3, b[1] + 2), fill=(255, 255, 255))

    # ---- embossed silver panel --------------------------------------------
    b = tile("panel", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.SILVER, M.SILVER_DARK)
    d.rectangle([b[0] + 1, b[1] + 1, b[2] - 2, b[3] - 2], outline=M.SILVER_DEEP)
    d.ellipse([b[0] + 2, b[1] + 2, b[0] + 5, b[1] + 5], outline=M.SILVER_LITE)
    d.ellipse([b[0] + 2, b[1] + 2, b[0] + 5, b[1] + 5], outline=M.SILVER_DEEP)
    d.point((b[0] + 3, b[1] + 3), fill=M.SILVER_LITE)
    d.line([(b[0] + 1, b[3] - 3), (b[0] + 6, b[3] - 3)], fill=M.SILVER_DEEP)
    b = tile("panel_side", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1), M.SILVER_DARK, M.SILVER_DEEP)
    tile("panel_top", M.SILVER_DARK)

    # ---- ribbon ------------------------------------------------------------
    b = tile("rib", M.OUTLINE)
    hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.PURPLE_DARK, M.PURPLE)
    d.line([(b[0] + 2, b[1] + 1), (b[0] + 2, b[3] - 2)], fill=M.PURPLE_LITE)
    b = tile("rib_side", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1), M.PURPLE_DARK, M.PURPLE_DEEP)
    b = tile("rib_tail", M.OUTLINE)
    hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.PURPLE, M.PURPLE_DEEP)

    return img


if __name__ == "__main__":
    im = build_texture()
    im.save(r"F:\Work\Frilien's Magic\tools\phiren\atlas.png")
    im.resize((W * 8, H * 8), Image.NEAREST).save(
        r"F:\Work\Frilien's Magic\tools\phiren\atlas_big.png")
    print("wrote atlas", im.size)
