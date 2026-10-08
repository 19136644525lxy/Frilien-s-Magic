"""
Build a Blockbench-importable 3D model of Frieren's staff from the concept art.

Geometry is described in model pixels (Minecraft scale); the front view maps 1:1
onto the source artwork, so the model can be verified by overlaying the render.

  y = 0    bottom of the ferrule
  y = 128  tip of the finial
  x = 0    shaft axis (artwork shaft centre: image x 837)
  z = 0    staff mid-plane
"""
import math

# ---------------------------------------------------------------- world mapping
IMG_W, IMG_H = 1672, 2508
SHAFT_CX = 837.0          # artwork x of the shaft axis
BASE_Y = 2456.0           # artwork y of the staff base
HEIGHT = 128.0
IMG_TOP = 59.0
SCALE = HEIGHT / (BASE_Y - IMG_TOP)      # model units per artwork pixel (0.05340)
PX = 1.0 / SCALE                         # artwork pixels per model unit (18.727)

# ---------------------------------------------------------------- palette
OUTLINE   = (0x3A, 0x24, 0x0E)
GOLD_PALE = (0xFD, 0xEC, 0xA8)
GOLD_LITE = (0xF9, 0xE0, 0x8C)
GOLD_MID  = (0xEF, 0xC9, 0x63)
GOLD_DARK = (0xD3, 0xA1, 0x3C)
GOLD_DEEP = (0xA8, 0x76, 0x22)
RED_LITE  = (0xF6, 0x55, 0x4A)
RED_MID   = (0xEE, 0x31, 0x2F)
RED_DARK  = (0xC4, 0x1F, 0x22)
RED_DEEP  = (0x8E, 0x12, 0x16)
GEM_LITE  = (0xFF, 0x8A, 0x84)
GEM_MID   = (0xE4, 0x23, 0x28)
GEM_DARK  = (0xAF, 0x13, 0x21)
GEM_DEEP  = (0x74, 0x0A, 0x14)
WHITE     = (0xFF, 0xFF, 0xFF)

# ---------------------------------------------------------------- atlas layout
TILE = 8
COLS, ROWS = 13, 10
ATLAS_W, ATLAS_H = COLS, ROWS              # logical texture size in 8x8 cells
TEX_W, TEX_H = COLS * TILE, ROWS * TILE    # real PNG size (104 x 80)

CELLS = {}
_cells = [
    "shaft", "shaft_side", "shaft_top",
    "col1", "col2", "col3", "col4", "col5", "col6",
    "fer1", "fer2", "fer3", "fer4",
    "finial", "finial_side",
    "gem", "gem_side",
    "ring", "ring_top",
    "arc_red", "arc_red_out", "arc_red_in", "arc_red_end",
    "arc_gold", "arc_gold_out", "arc_gold_in", "arc_gold_end",
    "spoke_f", "spoke_u",
    "rib_1", "rib_2", "rib_3", "rib_4", "rib_5", "rib_tail",
    "bar",
]
for i, name in enumerate(_cells):
    CELLS[name] = (i % COLS, i // COLS)


def uv_of(cell):
    """(u1,v1,u2,v2) in texture pixels for a cell."""
    cx, cy = CELLS[cell]
    x0, y0 = cx * TILE, cy * TILE
    return (x0, y0, x0 + TILE, y0 + TILE)


# ---------------------------------------------------------------- geometry
GROUPS = {"shaft": [], "collar": [], "ferrule": [], "head": [], "ribbon": []}
BOXES = []


def box(group, name, x1, y1, z1, x2, y2, z2, sprite, rot=None, origin=None):
    e = {
        "name": name,
        "from": [round(min(x1, x2), 3), round(min(y1, y2), 3), round(min(z1, z2), 3)],
        "to":   [round(max(x1, x2), 3), round(max(y1, y2), 3), round(max(z1, z2), 3)],
        "sprite": sprite,
    }
    if rot:
        e["rot"] = rot
    if origin:
        e["origin"] = list(origin)
    GROUPS[group].append(e)
    BOXES.append(e)
    return e


def rbox(group, name, length, width, depth, cx, cy, cz, angle_deg, axis, sprite, origin=None):
    """Box of (length x width x depth) centred at (cx,cy,cz), rotated angle_deg
    about `axis`; `length` lies along the rotating radial direction."""
    a = math.radians(angle_deg)
    c, s = math.cos(a), math.sin(a)
    if axis == "y":
        hx, hy, hz = length / 2, width / 2, depth / 2
        world = [(cx + px * c + pz * s, cy + py, cz - px * s + pz * c)
                 for px in (-hx, hx) for py in (-hy, hy) for pz in (-hz, hz)]
        rot = (0, angle_deg, 0)
    elif axis == "z":
        hx, hy, hz = length / 2, width / 2, depth / 2
        world = [(cx + px * c - py * s, cy + px * s + py * c, cz + pz)
                 for px in (-hx, hx) for py in (-hy, hy) for pz in (-hz, hz)]
        rot = (0, 0, angle_deg)
    else:
        raise ValueError(axis)
    xs = [p[0] for p in world]; ys = [p[1] for p in world]; zs = [p[2] for p in world]
    return box(group, name, min(xs), min(ys), min(zs), max(xs), max(ys), max(zs),
               sprite, rot=tuple(round(v, 3) for v in rot),
               origin=(round(cx, 3), round(cy, 3), round(cz, 3)))


def ring_of_boxes(group, name, radius, half_radial, half_depth, cy, n, sprite, z=0.0):
    for i in range(n):
        ang = 360.0 * i / n
        a = math.radians(ang)
        rbox(group, f"{name}{i}", half_radial * 2, half_depth * 2, half_radial * 2,
             radius * math.cos(a), cy + radius * math.sin(a), z, ang, "z", sprite)


# ------------------------------------------------------------------ constants
SHAFT_TOP = 72.0
RING_CY = 112.20      # centre of the gem and its ring (measured off the artwork)
RING_R = 5.25         # ring centreline radius
RING_BAND = 1.15      # ring radial half-thickness
RING_DEPTH = 1.7      # ring half-depth (z)
RING_Z = 0.8          # ring sits slightly proud of the crescent
GEM_R = 4.55          # gem radius (artwork: ~180 px across)
ARC_DEPTH = 1.3
CRESCENT_Z = -2.6     # the crescent sits behind the gem and its ring

# ------------------------------------------------------------------ the shaft
box("shaft", "shaft", -1.5, 13.0, -1.5, 1.5, SHAFT_TOP, 1.5,
    {"north": "shaft", "south": "shaft", "east": "shaft_side", "west": "shaft_side",
     "up": "shaft_top", "down": "shaft_top"})

# ------------------------------------------------------------------- ferrule
ferrule = [
    (0.0, 1.6, 2.4, "fer1"),
    (1.6, 3.4, 2.1, "fer2"),
    (3.4, 8.2, 1.75, "fer3"),
    (8.2, 10.8, 2.0, "fer4"),
    (10.8, 12.0, 1.9, "fer3"),
    (12.0, 13.2, 2.6, "fer1"),
]
for i, (y0, y1, hw, sp) in enumerate(ferrule):
    box("ferrule", f"ferrule_{i}", -hw, y0, -hw, hw, y1, hw,
        {"north": sp, "south": sp, "east": sp, "west": sp, "up": sp, "down": "fer2"})

# ------------------------------------------------------------------- collar
# Measured off the artwork: the banded fittings sit between the plain red shaft
# (up to model y ~71) and the flared cup that meets the crescent.  The gold ends
# around model y 89.5, where the red shaft resumes.
collar = [
    (71.0, 73.6, 1.75, "col1"),      # first ring above the shaft
    (73.6, 75.2, 2.35, "col2"),      # lip
    (75.2, 77.2, 1.60, "col3"),      # neck
    (77.2, 78.8, 2.35, "col2"),      # second lip
    (78.8, 80.8, 1.60, "col3"),      # neck
    (80.8, 82.8, 2.15, "col4"),      # third ring
    (82.8, 84.8, 1.65, "col3"),      # neck
    (84.8, 87.2, 2.55, "col5"),      # cup starts to flare
    (87.2, 89.5, 3.05, "col6"),      # flared cup meeting the crescent
]
for i, (y0, y1, hw, sp) in enumerate(collar):
    box("collar", f"collar_{i}", -hw, y0, -hw, hw, y1, hw,
        {"north": sp, "south": sp, "east": sp, "west": sp, "up": sp, "down": "col2"})

# ------------------------------------------------------------------- gem ring
ring_of_boxes("head", "ring_", RING_R, RING_BAND, RING_DEPTH, RING_CY, 18, "ring",
              z=RING_Z)
# gem: three stacked cuboids whose half-widths follow a circle, so the silhouette
# reads as a sphere while every part stays an editable cuboid.  The front and
# back faces are the round gradient sprite.
GEM_DY = 4.45                                  # half-height of the sphere
_gem_z = RING_Z                                # keeps the gem clear of the crescent
for _i, (_y0, _y1, _hw) in enumerate([
    (RING_CY - 4.45, RING_CY - 2.22, 2.55),
    (RING_CY - 2.22, RING_CY + 2.22, 4.75),
    (RING_CY + 2.22, RING_CY + 4.45, 2.55),
]):
    _half_depth = 1.25 if _i == 1 else 0.55
    box("head", f"gem_{_i}", -_hw, _y0, _gem_z - _half_depth, _hw, _y1, _gem_z + _half_depth,
        {"north": "gem", "south": "gem", "east": "gem_side", "west": "gem_side",
         "up": "gem", "down": "gem"})

# ------------------------------------------------------------- crescent arc
# The crescent's outer rim is an ellipse (286 x 267 artwork px) centred slightly
# below the gem centre; the band is wide on the two arms and tapers across the
# top, where a broad gold spike stands on it.
# The crescent's outer rim is an ellipse centred on the gem (artwork 275 x 288 px);
# the band is wide on the two arms and tapers across the top, where a broad gold
# spike stands on it.
CRESCENT_CX, CRESCENT_CY = 0.95, 114.60
RIM_A, RIM_B = 12.45, 12.15            # outer-rim semi-axes (model units)
RIM_TILT = -6.0                        # degrees, outer ellipse rotation
# The artwork's crescent reads as a thick C: its mouth sits on the left, a little
# below horizontal, and the two horns converge to points there.
# The crescent's outer rim is a near-circle (fitted from the digitised concept
# art: centre (1.0, 114.5), radius 12.9).  The band is ~3.2 units across and the
# two horns taper to points either side of a wide mouth on the left/lower-left.
CRESCENT_CX, CRESCENT_CY = 0.90, 112.20
CRESCENT_R = 14.60                     # outer rim radius
CRESCENT_W = 3.60                      # band width
# `ARC_A0`/`ARC_A1` and `MOUTH_DEG` are in the FINAL fitted model frame; the rim
# centre is stored at (0, 133.0) so a 201.6 degree spin of the artwork lines the
# crescent's mouth up with the concept art's (see the module's __main__ block).
ARC_A0, ARC_A1 = 209.0, 537.0          # = -151 deg .. 177 deg, wraps through 0
ARC_SEGMENTS = 56
MOUTH_DEG = 193.0                      # direction the mouth faces (lower left)
RED_FROM, RED_TO = 268.0, 408.0        # red-painted stretch (right arm, lower)


def crescent_outer(angle):
    """Outer rim radius (a circle, as in the concept art)."""
    return CRESCENT_R


def crescent_inner(angle):
    """Inner edge: tapers to meet the outer rim at the two horns."""
    return CRESCENT_R - CRESCENT_W * max(
        0.10, (1.0 - math.cos(math.radians(angle - MOUTH_DEG))) / 2.0)


def crescent_outer_ellipse(angle):
    """Radius of the outer-rim ellipse along `angle` (used by the tune-up tools)."""
    a = math.radians(angle)
    t = math.radians(RIM_TILT)
    vx = math.cos(a) * math.cos(t) + math.sin(a) * math.sin(t)
    vy = -math.cos(a) * math.sin(t) + math.sin(a) * math.cos(t)
    return 1.0 / math.sqrt((vx / RIM_A) ** 2 + (vy / RIM_B) ** 2)


def build_crescent():
    for i in range(ARC_SEGMENTS):
        d = (ARC_A1 - ARC_A0) / ARC_SEGMENTS
        s0 = ARC_A0 + d * i
        s1 = s0 + d
        mid = (s0 + s1) / 2.0
        th = math.radians(mid)
        r_in = crescent_inner(mid)
        r_out = crescent_outer(mid)
        half = (r_out - r_in) / 2.0
        r = (r_out + r_in) / 2.0
        cx = CRESCENT_CX + r * math.cos(th)
        cy = CRESCENT_CY + r * math.sin(th)
        red = RED_FROM <= mid <= RED_TO
        sp = "arc_red" if red else "arc_gold"
        sp_out = "arc_red_out" if red else "arc_gold_out"
        sp_in = "arc_red_in" if red else "arc_gold_in"
        sp_end = "arc_red_end" if red else "arc_gold_end"
        ox, oy = math.cos(th), math.sin(th)
        dirs = [((0, 1), "north"), ((0, -1), "south"), ((1, 0), "east"), ((-1, 0), "west")]
        scored = sorted(((dx * ox + dy * oy, f) for (dx, dy), f in dirs), reverse=True)
        outer_face, inner_face = scored[0][1], scored[-1][1]
        faces = {"north": sp, "south": sp, "up": sp_end, "down": sp_end,
                 "east": sp_end, "west": sp_end}
        faces[outer_face] = sp_out
        faces[inner_face] = sp_in
        seg_len = math.radians(d + 7.0) * r + 0.5
        # the arc sits behind the gem ring in z, as in the artwork
        # (rbox takes: tangential length, radial width, depth)
        rbox("head", f"arc_{i}", seg_len, half * 2, ARC_DEPTH * 2,
             cx, cy, CRESCENT_Z, mid, "z", faces)


build_crescent()

# -------------------------------------------------------------------- finial
# broad gold spike standing on top of the crescent (artwork: y 59..212, ~100 px wide)
FINIAL_BOTTOM = CRESCENT_CY + crescent_outer(90.0) - 1.9
_finial_stack = [
    (0.0, 1.7, 1.85, 1.55),
    (1.7, 3.0, 1.30, 1.10),
    (3.0, 4.2, 0.70, 0.60),
]
for i, (y0, y1, hw, dep) in enumerate(_finial_stack):
    box("head", f"finial_{i}", -hw, FINIAL_BOTTOM + y0, CRESCENT_Z - dep,
        hw, FINIAL_BOTTOM + y1, CRESCENT_Z + dep,
        {"north": "finial", "south": "finial", "east": "finial_side", "west": "finial_side",
         "up": "finial", "down": "finial_side"})

# ------------------------------------------------------------------- spokes
# vertical stem linking the ring to the crescent: the upper one runs up to the
# crescent's inner rim, the lower one down to the red foot of the crescent
box("head", "spoke_up", -1.1, RING_CY + RING_R + RING_BAND - 0.4, CRESCENT_Z - 0.9,
    1.1, CRESCENT_CY + crescent_inner(90.0) - 0.2, CRESCENT_Z + 0.9,
    {"north": "spoke_f", "south": "spoke_f", "east": "spoke_u", "west": "spoke_u",
     "up": "spoke_u", "down": "spoke_u"})
box("head", "spoke_down", -1.1, 99.0, CRESCENT_Z - 0.9,
    1.1, RING_CY - RING_R - RING_BAND + 0.4, CRESCENT_Z + 0.9,
    {"north": "spoke_f", "south": "spoke_f", "east": "spoke_u", "west": "spoke_u",
     "up": "spoke_u", "down": "spoke_u"})
# short horizontal bar from the ring out to the crescent, on the right
box("head", "bar_right", RING_R - 1.2, RING_CY + 1.4, CRESCENT_Z - 0.9,
    crescent_inner(0.0) - 0.4, RING_CY + 2.8, CRESCENT_Z + 0.9,
    {"north": "bar", "south": "bar", "east": "bar", "west": "bar",
     "up": "bar", "down": "bar"})

# ------------------------------------------------------------------- ribbon
# Measured off the artwork: a slim red band that starts beside the shaft just
# under the collar, sweeps down and to the left, and widens as it falls.  Values
# are in FINAL model coordinates.
_RIB = [
    # (y_bottom, y_top, x_left, x_right)
    (95.8, 99.1, -4.20, -2.10),
    (92.9, 95.8, -5.20, -2.95),
    (90.0, 92.9, -6.25, -3.85),
    (87.1, 90.0, -7.30, -4.70),
    (85.2, 87.1, -8.30, -5.55),
    (83.6, 85.2, -9.20, -6.35),
]
RIBBON = [(y0, y1, xl, xr, f"rib_{min(4, i // 2) + 1}")
          for i, (y0, y1, xl, xr) in enumerate(_RIB)]
for i, (y0, y1, xl, xr, sp) in enumerate(RIBBON):
    box("ribbon", f"ribbon_{i}", xl, y0, -0.8, xr, y1, 0.8,
        {"north": sp, "south": sp, "east": "rib_5", "west": "rib_5",
         "up": sp, "down": "rib_5"})
# swallow tail: two prongs with a notch between them
box("ribbon", "ribbon_tail_l", -9.20, 81.5, -0.8, -7.55, 83.6, 0.8,
    {"north": "rib_tail", "south": "rib_tail", "east": "rib_5", "west": "rib_5",
     "up": "rib_tail", "down": "rib_tail"})
box("ribbon", "ribbon_tail_r", -6.95, 82.4, -0.8, -5.45, 83.6, 0.8,
    {"north": "rib_tail", "south": "rib_tail", "east": "rib_5", "west": "rib_5",
     "up": "rib_tail", "down": "rib_tail"})

# ------------------------------------------------------------- head offset
# Head parts are laid out in artwork space; nudge the whole head group so it
# sits on the collar exactly as in the artwork.
HEAD_DY = -4.0
for _e in GROUPS["head"]:
    _e["from"][1] = round(_e["from"][1] + HEAD_DY, 3)
    _e["to"][1] = round(_e["to"][1] + HEAD_DY, 3)
    if _e.get("origin"):
        _e["origin"][1] = round(_e["origin"][1] + HEAD_DY, 3)

# ------------------------------------------------------- normalise the height
# The head is laid out from measurements in artwork space.  Scale the finished
# model so it never exceeds HEIGHT, then shift it so the base sits at y = 0 and
# the tip lands as close to HEIGHT as the proportions allow.
_ys = [e[k][1] for e in BOXES for k in ("from", "to")]
_RAW_MIN, _RAW_MAX = min(_ys), max(_ys)
_RAW_H = _RAW_MAX - _RAW_MIN
FIT = min(1.0, HEIGHT / _RAW_H)
_RAW_MIN = _RAW_MAX - HEIGHT / FIT          # anchor the top, not the bottom


def _fit_x(v):
    """Horizontal scale only - x is measured from the shaft axis, so it must NOT
    pick up the vertical offset."""
    return round(v * FIT, 3)


def _fit(v):
    return round((v - _RAW_MIN) * FIT, 3)


for _e in BOXES:
    _e["from"] = [_fit_x(_e["from"][0]), _fit(_e["from"][1]), _e["from"][2]]
    _e["to"] = [_fit_x(_e["to"][0]), _fit(_e["to"][1]), _e["to"][2]]
    if _e.get("origin"):
        _e["origin"] = [_fit_x(_e["origin"][0]), _fit(_e["origin"][1]), _e["origin"][2]]

_ys = [e[k][1] for e in BOXES for k in ("from", "to")]
SHIFT_UP = -min(_ys)                        # base to y = 0
if SHIFT_UP > 0:
    for _e in BOXES:
        _e["from"][1] = round(_e["from"][1] + SHIFT_UP, 3)
        _e["to"][1] = round(_e["to"][1] + SHIFT_UP, 3)
        if _e.get("origin"):
            _e["origin"][1] = round(_e["origin"][1] + SHIFT_UP, 3)

# ------------------------------------------------------------ image mapping


def model_to_image(x, y):
    """Map a point of the *fitted* model back onto the artwork."""
    raw_y = (y - SHIFT_UP) / FIT + _RAW_MIN
    return SHAFT_CX + x * PX, BASE_Y - raw_y * PX


def image_to_model(ix, iy):
    return (ix - SHAFT_CX) / PX * FIT, (((BASE_Y - iy) / PX - _RAW_MIN) * FIT) + SHIFT_UP


if __name__ == "__main__":
    print(f"1 raw unit = {PX:.2f} artwork px;  fit {FIT:.4f};  shift {SHIFT_UP:.2f};"
          f"  {len(BOXES)} boxes")
    for g, items in GROUPS.items():
        print(f"  {g:8s} {len(items)}")
    ys = [e[k][1] for e in BOXES for k in ("from", "to")]
    xs = [e[k][0] for e in BOXES for k in ("from", "to")]
    print(f"\nmodel bbox: x {min(xs):.2f}..{max(xs):.2f}   y {min(ys):.2f}..{max(ys):.2f}")
