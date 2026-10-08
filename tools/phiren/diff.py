"""Overlay the model's front view on the concept art at matching scale.

The model is normalised to 128 units tall with its base at y=0, while the artwork
spans BASE_Y..IMG_TOP.  Map model coordinates straight onto artwork pixels via
model_to_image(), then blend.
"""
import numpy as np
from PIL import Image, ImageDraw
import math

import staff_model as M
import texture as TEX

SRC = (r"F:\Work\Frilien's Magic\src\main\resources\assets\friliensmagic"
       r"\textures\item\frieren_staff.png")
OUT = r"F:\Work\Frilien's Magic\tools\staff"

CORNERS = {
    "south": [(-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1)],
    "north": [(1, -1, -1), (-1, -1, -1), (-1, 1, -1), (1, 1, -1)],
    "east": [(1, -1, 1), (1, -1, -1), (1, 1, -1), (1, 1, 1)],
    "west": [(-1, -1, -1), (-1, -1, 1), (-1, 1, 1), (-1, 1, -1)],
    "up": [(-1, 1, 1), (1, 1, 1), (1, 1, -1), (-1, 1, -1)],
    "down": [(-1, -1, -1), (1, -1, -1), (1, -1, 1), (-1, -1, 1)],
}
FACE_DIR = {"south": (0, 0, 1), "north": (0, 0, -1), "east": (1, 0, 0),
            "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def rot_pt(p, rot, origin):
    if not rot:
        return p
    x, y, z = p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]
    for axis, ang in zip("xyz", rot):
        if not ang:
            continue
        c, s = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        if axis == "x":
            y, z = y * c - z * s, y * s + z * c
        elif axis == "y":
            x, z = x * c + z * s, -x * s + z * c
        else:
            x, y = x * c - y * s, x * s + y * c
    return x + origin[0], y + origin[1], z + origin[2]


def render_front(atlas):
    img = Image.new("RGBA", (M.IMG_W, M.IMG_H), (0, 0, 0, 0))
    quads = []
    for e in M.BOXES:
        sprites = e["sprite"] if isinstance(e["sprite"], dict) else {
            k: e["sprite"] for k in CORNERS}
        origin = e.get("origin") or ((e["from"][0] + e["to"][0]) / 2,
                                     (e["from"][1] + e["to"][1]) / 2,
                                     (e["from"][2] + e["to"][2]) / 2)
        x1, y1, z1 = e["from"]; x2, y2, z2 = e["to"]
        for fname, cs in CORNERS.items():
            pts = [rot_pt((x2 if sx > 0 else x1, y2 if sy > 0 else y1,
                           z2 if sz > 0 else z1), e.get("rot"), origin)
                   for sx, sy, sz in cs]
            n = rot_pt(FACE_DIR[fname], e.get("rot"), (0, 0, 0))
            if n[2] <= 0.02:
                continue
            pp = [M.model_to_image(p[0], p[1]) for p in pts]
            quads.append((sum(p[2] for p in pts) / 4, fname, pp, sprites[fname]))
    quads.sort(key=lambda q: q[0])
    SHADE = {"south": 1.0, "north": 0.7, "east": 0.87, "west": 0.82, "up": 1.05, "down": 0.65}
    for _, fname, poly, sprite in quads:
        u1, v1, u2, v2 = M.uv_of(sprite)
        tile = atlas.crop((u1, v1, u2, v2)).resize((32, 32), Image.NEAREST)
        arr = np.asarray(tile).astype(float)
        arr[..., :3] = np.clip(arr[..., :3] * SHADE[fname], 0, 255)
        tile = Image.fromarray(arr.astype("uint8"))
        xs = [p[0] for p in poly]; ys = [p[1] for p in poly]
        x0, y0 = int(min(xs)), int(min(ys))
        w = max(2, int(math.ceil(max(xs) - x0))); h = max(2, int(math.ceil(max(ys) - y0)))
        if w > 4000 or h > 4000:
            continue
        t = tile.resize((w, h), Image.NEAREST)
        local = [(p[0] - x0, p[1] - y0) for p in poly]
        nw, sw, se, ne = local[3], local[0], local[1], local[2]
        try:
            warped = t.transform((w, h), Image.QUAD,
                                 (nw[0], nw[1], sw[0], sw[1], se[0], se[1],
                                  ne[0], ne[1]), Image.NEAREST)
        except Exception:
            continue
        mask = Image.new("L", (w, h), 0)
        ImageDraw.Draw(mask).polygon(local, fill=255)
        a2 = np.asarray(warped.getchannel("A")).astype(np.uint16)
        m2 = np.asarray(mask).astype(np.uint16)
        img.paste(warped, (x0, y0), Image.fromarray((a2 * m2 // 255).astype("uint8")))
    return img


if __name__ == "__main__":
    atlas = TEX.build_texture()
    model = render_front(atlas)
    art = Image.open(SRC).convert("RGBA")

    box = (560, 20, 1120, 2500)
    A = art.crop(box)
    Mp = model.crop(box)

    # 1. side by side, identical scale
    sheet = Image.new("RGB", (A.width * 2 + 30, A.height), (255, 255, 255))
    sheet.paste(A.convert("RGB"), (0, 0))
    bg = Image.new("RGBA", A.size, (255, 255, 255, 255))
    sheet.paste(Image.alpha_composite(bg, Mp).convert("RGB"), (A.width + 30, 0))
    sheet.save(f"{OUT}/_cmp_side.png")

    # 2. blend: artwork in red channel, model silhouette in green
    a = np.asarray(A.convert("RGB")).astype(int)
    m = np.asarray(Image.alpha_composite(Image.new("RGBA", A.size, (255, 255, 255, 255)),
                                         Mp).convert("RGB")).astype(int)
    art_fg = (np.abs(a - 255).max(axis=2) > 18)
    mod_fg = (np.abs(m - 255).max(axis=2) > 18)
    out = np.full(a.shape, 255, dtype=np.uint8)
    out[art_fg] = [255, 150, 150]          # artwork only  -> pink
    out[mod_fg] = [150, 255, 150]          # model only    -> green
    out[art_fg & mod_fg] = [60, 60, 60]    # both agree    -> dark
    Image.fromarray(out).save(f"{OUT}/_cmp_overlay.png")
    print("artwork pixels", art_fg.sum(), " model pixels", mod_fg.sum(),
          " overlap", (art_fg & mod_fg).sum())
    print("iou = %.3f" % ((art_fg & mod_fg).sum() / max(1, (art_fg | mod_fg).sum())))
    print("wrote _cmp_side.png and _cmp_overlay.png")
