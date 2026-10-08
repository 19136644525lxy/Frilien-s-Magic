"""Z-buffered 3/4 view of the phiren model.

The earlier painter's-algorithm preview can show spurious slivers where quads
interleave; this rasterises with a real depth buffer so the geometry is judged
fairly.
"""
import math
import numpy as np
from PIL import Image
import phiren_model as M
import phiren_texture as TEX
from phiren_render import CORNERS, FACE_DIR, rot_pt

OUT = r"F:\Work\Frilien's Magic\tools\phiren\preview_3d.png"


def render(yaw=30.0, pitch=12.0, W=760, H=760, scale=5.4):
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    cam = np.array([sy * cp, -sp, cy * cp])

    def proj(p):
        x, y, z = p[0], p[1] - 64.0, p[2]
        x1 = x * cy + z * sy
        z1 = -x * sy + z * cy
        y1 = y * cp - z1 * sp
        z2 = y * sp + z1 * cp
        return np.array([x1, y1, z2])

    atlas = np.asarray(TEX.build_texture())
    colour = np.full((H, W, 3), 247, np.uint8)
    zbuf = np.full((H, W), -1e9, np.float64)

    tris = []
    for e in M.BOXES:
        sprites = e["sprite"] if isinstance(e["sprite"], dict) else {
            k: e["sprite"] for k in CORNERS}
        origin = e.get("origin") or ((e["from"][0] + e["to"][0]) / 2,
                                     (e["from"][1] + e["to"][1]) / 2,
                                     (e["from"][2] + e["to"][2]) / 2)
        x1, y1, z1 = e["from"]; x2, y2, z2 = e["to"]
        for fname, cs in CORNERS.items():
            pts = [rot_pt((x2 if sx > 0 else x1, y2 if sy_ > 0 else y1,
                           z2 if sz > 0 else z1), e.get("rot"), origin)
                   for sx, sy_, sz in cs]
            n = np.array(rot_pt(FACE_DIR[fname], e.get("rot"), (0, 0, 0)), float)
            if np.dot(n, cam) >= 0:
                continue
            u1, v1, u2, v2 = M.uv_of(sprites[fname])
            tile = atlas[int(v1):int(v2), int(u1):int(u2)].astype(float)
            tile = np.kron(tile, np.ones((4, 4, 1)))          # 8x8 -> 32x32
            sh = {"south": 1.0, "north": 0.86, "east": 0.92, "west": 0.92,
                  "up": 1.0, "down": 0.80}[fname]
            tile[..., :3] = np.clip(tile[..., :3] * sh, 0, 255)
            p = [proj(q) for q in pts]
            uv = [(0, 0), (1, 0), (1, 1), (0, 1)]
            for tri in ((0, 1, 2), (0, 2, 3)):
                tris.append(([p[i] for i in tri], [uv[i] for i in tri], tile))

    for p3, uv3, tile in tris:
        scr = []
        for q in p3:
            scr.append((W / 2 + q[0] * scale, H * 0.58 - q[1] * scale, q[2]))
        xs = [s[0] for s in scr]; ys = [s[1] for s in scr]
        x0, x1 = int(max(0, math.floor(min(xs)))), int(min(W - 1, math.ceil(max(xs))))
        y0, y1 = int(max(0, math.floor(min(ys)))), int(min(H - 1, math.ceil(max(ys))))
        if x1 < x0 or y1 < y0:
            continue
        (ax, ay, az), (bx, by, bz), (cx_, cy_, cz) = scr
        den = (by - cy_) * (ax - cx_) + (cx_ - bx) * (ay - cy_)
        if abs(den) < 1e-9:
            continue
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1):
                px, py = xx + 0.5, yy + 0.5
                w0 = ((by - cy_) * (px - cx_) + (cx_ - bx) * (py - cy_)) / den
                w1 = ((cy_ - ay) * (px - cx_) + (ax - cx_) * (py - cy_)) / den
                w2 = 1 - w0 - w1
                if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                    continue
                z = w0 * az + w1 * bz + w2 * cz
                if z <= zbuf[yy, xx]:
                    continue
                tu = w0 * uv3[0][0] + w1 * uv3[1][0] + w2 * uv3[2][0]
                tv = w0 * uv3[0][1] + w1 * uv3[1][1] + w2 * uv3[2][1]
                ti = int(np.clip(tv * tile.shape[0], 0, tile.shape[0] - 1))
                tj = int(np.clip(tu * tile.shape[1], 0, tile.shape[1] - 1))
                texel = tile[ti, tj]
                if texel[3] < 40:
                    continue
                zbuf[yy, xx] = z
                colour[yy, xx] = texel[:3]
    return Image.fromarray(colour)


if __name__ == "__main__":
    a = render(yaw=30, pitch=12)
    b = render(yaw=-30, pitch=12)
    SRC = (r"F:\Work\Frilien's Magic\src\main\resources\assets\friliensmagic"
           r"\textures\item\phiren_staff.png")
    art = Image.open(SRC).convert("RGB")
    art = art.resize((int(art.width * 760 / art.height), 760), Image.LANCZOS)
    sheet = Image.new("RGB", (art.width + 760 * 2 + 30, 760), (255, 255, 255))
    sheet.paste(art, (0, 0))
    sheet.paste(a, (art.width + 10, 0))
    sheet.paste(b, (art.width + 780, 0))
    sheet.save(OUT)
    print("wrote", OUT)
