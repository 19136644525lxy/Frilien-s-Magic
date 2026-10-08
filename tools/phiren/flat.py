"""Flat-colour front view of the phiren model: confirms GEOMETRY only."""
import numpy as np
from PIL import Image, ImageDraw
import phiren_model as M
from phiren_render import CORNERS, FACE_DIR, rot_pt

SRC = (r"F:\Work\Frilien's Magic\src\main\resources\assets\friliensmagic"
       r"\textures\item\phiren_staff.png")

SPRITE_COLOUR = {
    "wood": (139, 90, 51), "wood_side": (94, 58, 31), "wood_top": (94, 58, 31),
    "purp": (140, 79, 196), "purp_side": (99, 51, 147), "purp_top": (99, 51, 147),
    "silv": (195, 200, 210), "silv_side": (141, 148, 162), "silv_top": (141, 148, 162),
    "cream": (242, 223, 168), "cream_side": (211, 182, 114), "cream_top": (211, 182, 114),
    "red": (224, 48, 42), "red_side": (158, 20, 20), "red_top": (158, 20, 20),
    "panel": (195, 200, 210), "panel_side": (141, 148, 162), "panel_top": (141, 148, 162),
    "rib": (140, 79, 196), "rib_side": (99, 51, 147), "rib_tail": (140, 79, 196),
    "bead": (224, 48, 42),
}

img = Image.new("RGB", (M.IMG_W, M.IMG_H), (255, 255, 255))
quads = []
for e in M.BOXES:
    sprites = e["sprite"] if isinstance(e["sprite"], dict) else {k: e["sprite"] for k in CORNERS}
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
d = ImageDraw.Draw(img)
for _, fname, poly, sprite in quads:
    col = SPRITE_COLOUR.get(sprite, (255, 0, 255))
    d.polygon(poly, fill=col, outline=(40, 24, 12))

art = Image.open(SRC).convert("RGB")
sheet = Image.new("RGB", (art.width * 2 + 30, art.height), (255, 255, 255))
sheet.paste(art, (0, 0))
sheet.paste(img, (art.width + 30, 0))
sheet.save(r"F:\Work\Frilien's Magic\tools\phiren\_flat.png")

a = np.asarray(art).astype(int)
m = np.asarray(img).astype(int)
af = (np.abs(a - 255).max(axis=2) > 18)
mf = (np.abs(m - 255).max(axis=2) > 18)
print("IoU (flat geometry):", round((af & mf).sum() / max(1, (af | mf).sum()), 3))
