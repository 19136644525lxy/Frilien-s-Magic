"""Generate the model's texture atlas (104x80) as flat pixel-art tiles."""
import math
from PIL import Image, ImageDraw
import staff_model as M

T = M.TILE
W, H = M.TEX_W, M.TEX_H


def cell_box(cell):
    cx, cy = M.CELLS[cell]
    return cx * T, cy * T, (cx + 1) * T, (cy + 1) * T


def lerp(a, b, f):
    return tuple(int(round(a[i] + (b[i] - a[i]) * f)) for i in range(3))


def vgrad(d, box, top, bottom, steps=4):
    x0, y0, x1, y1 = box
    n = y1 - y0
    for i in range(n):
        f = i / max(1, n - 1)
        col = lerp(top, bottom, f)
        d.line([(x0, y0 + i), (x1 - 1, y0 + i)], fill=col)


def hgrad(d, box, left, right, steps=4):
    x0, y0, x1, y1 = box
    n = x1 - x0
    for i in range(n):
        f = i / max(1, n - 1)
        d.line([(x0 + i, y0), (x0 + i, y1 - 1)], fill=lerp(left, right, f))


def frame(d, box, colour=None):
    x0, y0, x1, y1 = box
    p = [(x0, y0), (x1 - 1, y0), (x1 - 1, y1 - 1), (x0, y1 - 1), (x0, y0)]
    d.line(p, fill=colour or M.OUTLINE)


def build_texture():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def tile(cell, fill):
        b = cell_box(cell)
        d.rectangle([b[0], b[1], b[2] - 1, b[3] - 1], fill=fill)
        return b

    # ---- shaft: red rod, lighter down the middle, dark rim ------------------
    b = tile("shaft", M.OUTLINE)
    hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.RED_MID, M.RED_MID)
    d.rectangle([b[0] + 1, b[1], b[0] + 1, b[3] - 1], fill=M.RED_DARK)
    d.rectangle([b[2] - 2, b[1], b[2] - 2, b[3] - 1], fill=M.RED_DARK)
    d.rectangle([b[0] + 3, b[1], b[0] + 3, b[3] - 1], fill=M.RED_LITE)
    b = tile("shaft_side", M.OUTLINE)
    d.rectangle([b[0] + 1, b[1], b[2] - 2, b[3] - 1], fill=M.RED_MID)
    d.rectangle([b[0] + 2, b[1], b[2] - 3, b[3] - 1], fill=M.RED_DARK)
    b = tile("shaft_top", M.RED_DARK)
    d.rectangle([b[0] + 2, b[1] + 2, b[2] - 3, b[3] - 3], fill=M.RED_MID)
    d.point((b[0] + 3, b[1] + 3), fill=M.RED_LITE)

    # ---- gold bands (collar / ferrule): vertical metal gradients ------------
    bands = {
        "col1": (M.GOLD_DARK, M.GOLD_MID),
        "col2": (M.GOLD_MID, M.GOLD_DARK),
        "col3": (M.GOLD_LITE, M.GOLD_MID),
        "col4": (M.GOLD_LITE, M.GOLD_DARK),
        "col5": (M.GOLD_MID, M.GOLD_DARK),
        "col6": (M.GOLD_LITE, M.GOLD_MID),
        "fer1": (M.GOLD_MID, M.GOLD_DARK),
        "fer2": (M.GOLD_LITE, M.GOLD_MID),
        "fer3": (M.GOLD_MID, M.GOLD_DARK),
        "fer4": (M.GOLD_LITE, M.GOLD_MID),
    }
    for cell, (top, bot) in bands.items():
        b = tile(cell, M.OUTLINE)
        vgrad(d, (b[0], b[1] + 1, b[2], b[3] - 1), top, bot)
        # side shading
        d.rectangle([b[0], b[1] + 1, b[0], b[3] - 2], fill=M.GOLD_DARK)
        d.rectangle([b[2] - 1, b[1] + 1, b[2] - 1, b[3] - 2], fill=M.GOLD_DARK)
        # specular streak
        d.rectangle([b[0] + 2, b[1] + 1, b[0] + 3, b[1] + 1], fill=M.GOLD_PALE)
        d.rectangle([b[0] + 5, b[1] + 1, b[0] + 6, b[1] + 1], fill=M.GOLD_PALE)

    # ---- finial: pointed gold cap ------------------------------------------
    b = tile("finial", M.OUTLINE)
    x0, y0, x1, y1 = b
    cx = (x0 + x1 - 1) / 2.0
    # profile: widest near the base, sharp point at the top
    prof = [1.2, 2.2, 2.7, 2.9, 2.7, 2.1, 1.3, 0.2]
    for i in range(T):
        half = prof[i]
        col = lerp(M.GOLD_MID, M.GOLD_PALE, i / (T - 1.0) * 0.5)
        if i == 0:
            col = M.GOLD_DARK
        d.line([(cx - half, y0 + i), (cx + half, y0 + i)], fill=col)
        if half >= 0.5:
            d.point((cx - half, y0 + i), fill=M.GOLD_DEEP)
            d.point((cx + half, y0 + i), fill=M.GOLD_DEEP)
    # inner highlight
    d.line([(cx - 1, y0 + 1), (cx - 1, y0 + 5)], fill=M.GOLD_PALE)
    b = tile("finial_side", M.OUTLINE)
    vgrad(d, (b[0], b[1] + 1, b[2], b[3] - 1), M.GOLD_MID, M.GOLD_DARK)
    d.rectangle([b[0] + 3, b[1] + 1, b[0] + 3, b[3] - 2], fill=M.GOLD_LITE)
    d.rectangle([b[0] + 1, b[1] + 1, b[0] + 1, b[3] - 2], fill=M.GOLD_DEEP)

    # ---- gem: a disc with a bright centre and a dark rim --------------------
    # the centre of the sprite maps to the centre of the gem, so the gradient
    # reads as a sphere from every angle
    b = tile("gem", (0, 0, 0, 0))
    x0, y0, x1, y1 = b
    cx, cy = (x0 + x1 - 1) / 2.0, (y0 + y1 - 1) / 2.0
    R = 4.0
    for py in range(y0, y1):
        for px in range(x0, x1):
            dd = math.hypot(px - cx, py - cy)
            if dd > R:
                continue                       # cut-out corner -> round silhouette
            f = min(1.0, dd / R)
            if f < 0.42:
                col = lerp(M.GEM_LITE, M.GEM_MID, f / 0.42)
            else:
                col = lerp(M.GEM_MID, M.GEM_DEEP, (f - 0.42) / 0.58)
            img.putpixel((px, py), col + (255,))
    # specular glint, up and to the left of centre
    for py in range(y0, y1):
        for px in range(x0, x1):
            gx, gy = px - (cx - 1.0), py - (cy - 1.2)
            dd = math.hypot(gx, gy * 1.25)
            if dd < 1.15:
                img.putpixel((px, py), M.WHITE + (255,))
            elif dd < 1.9 and math.hypot(px - cx, py - cy) <= R:
                img.putpixel((px, py), M.GEM_LITE + (255,))
    # inboard shadow along the left/bottom rim
    for py in range(y0, y1):
        for px in range(x0, x1):
            dd = math.hypot(px - cx, py - cy)
            if dd > R:
                continue
            nx, ny = (px - cx) / max(dd, 1e-6), (py - cy) / max(dd, 1e-6)
            lit = -nx * 0.7 - ny * 0.7
            if lit < -0.35:
                img.putpixel((px, py), M.GEM_DEEP + (255,))

    b = tile("gem_side", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1), M.GEM_MID, M.GEM_DARK)
    d.rectangle([b[0] + 1, b[1] + 1, b[2] - 2, b[3] - 2], fill=M.GEM_DARK)
    d.rectangle([b[0] + 2, b[1] + 2, b[2] - 3, b[3] - 3], fill=M.GEM_MID)
    d.point((b[0] + 3, b[1] + 2), fill=M.GEM_LITE)

    # ---- ring: gold band rounded across its width --------------------------
    b = tile("ring", M.OUTLINE)
    for i in range(1, T - 1):
        f = abs(i - (T - 1) / 2.0) / ((T - 1) / 2.0)
        col = lerp(M.GOLD_PALE, M.GOLD_DEEP, f ** 0.9)
        d.line([(b[0] + i, b[1]), (b[0] + i, b[3] - 1)], fill=col)
    tile("ring_top", M.GOLD_MID)
    d.rectangle([b[0] + 1, b[1] + 1, b[2] - 2, b[3] - 2], fill=M.GOLD_LITE)

    # ---- crescent band ------------------------------------------------------
    # front/back faces: soft gold gradient (lighter outboard, darker inboard)
    def arc_tile(cell, light, mid, dark, deep):
        b = tile(cell, M.OUTLINE)
        ramp = [deep, dark, mid, mid, light, mid, mid, dark, deep]
        for i in range(1, T - 1):
            f = (i - 1) / (T - 3.0)
            col = lerp(light, mid, f)
            if i == 1:
                col = light
            elif i == T - 2:
                col = dark
            d.line([(b[0] + i, b[1]), (b[0] + i, b[3] - 1)], fill=col)
        return b

    arc_tile("arc_gold", M.GOLD_LITE, M.GOLD_PALE, M.GOLD_MID, M.GOLD_DARK)
    tile("arc_gold_out", M.GOLD_LITE)
    d.rectangle(cell_box("arc_gold_out"), fill=M.GOLD_PALE)
    tile("arc_gold_in", M.GOLD_DARK)
    d.rectangle(cell_box("arc_gold_in"), fill=M.GOLD_MID)
    tile("arc_gold_end", M.GOLD_MID)
    d.rectangle(cell_box("arc_gold_end"), fill=M.GOLD_LITE)

    arc_tile("arc_red", M.RED_LITE, M.RED_MID, M.RED_MID, M.RED_DARK)
    tile("arc_red_out", M.RED_LITE)
    tile("arc_red_in", M.RED_DEEP)
    d.rectangle(cell_box("arc_red_in"), fill=M.RED_DARK)
    tile("arc_red_end", M.RED_MID)
    d.rectangle(cell_box("arc_red_end"), fill=M.RED_LITE)

    # ---- spokes / bar -------------------------------------------------------
    b = tile("spoke_f", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.GOLD_LITE, M.GOLD_DARK)
    d.rectangle([b[0] + 3, b[1], b[0] + 3, b[3] - 1], fill=M.GOLD_PALE)
    b = tile("spoke_u", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.GOLD_MID, M.GOLD_DARK)
    b = tile("bar", M.OUTLINE)
    vgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.GOLD_LITE, M.GOLD_MID)

    # ---- ribbon -------------------------------------------------------------
    rib = {
        "rib_1": (M.RED_LITE, M.RED_MID, 1.0),
        "rib_2": (M.RED_LITE, M.RED_DARK, 1.0),
        "rib_3": (M.RED_MID, M.RED_DARK, 1.0),
        "rib_4": (M.RED_MID, M.RED_DEEP, 1.0),
        "rib_5": (M.RED_DARK, M.RED_DEEP, 1.0),
    }
    for cell, (top, bot, _) in rib.items():
        b = tile(cell, M.OUTLINE)
        hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), top, bot)
        d.rectangle([b[0] + 1, b[1], b[0] + 1, b[3] - 1], fill=M.RED_DARK)
    b = tile("rib_tail", M.OUTLINE)
    hgrad(d, (b[0] + 1, b[1], b[2] - 1, b[3]), M.RED_LITE, M.RED_DEEP)

    return img


if __name__ == "__main__":
    import os
    out = r"F:\Work\Frilien's Magic\tools\staff\atlas.png"
    img = build_texture()
    img.save(out)
    img.resize((W * 8, H * 8), Image.NEAREST).save(
        r"F:\Work\Frilien's Magic\tools\staff\atlas_big.png")
    print("wrote", out, img.size)
