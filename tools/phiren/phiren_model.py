"""
3D model of the "phiren" staff (phiren_staff.png).

Model units are Minecraft pixels.  Measured off the artwork:

    y = 0    bottom of the wooden foot
    y ~128   tip of the bone/cream head
    x = 0    the lower shaft's axis (artwork x = 866 px)
    1 model unit = 19.23 artwork px

The lower two thirds are a straight shaft (red-brown wood, purple cloth wrap,
silver bands and one embossed silver panel).  Above model y ~90 the staff bends
about 17 degrees to the left and ends in a flared bone-coloured head.  A purple
ribbon with a red bead hangs from the bend.
"""
import math

# ---------------------------------------------------------------- artwork map
IMG_W, IMG_H = 1606, 2610
BASE_Y, TIP_Y = 2535.0, 73.0
SHAFT_X = 866.0
HEIGHT = 128.0
PX_PER_UNIT = (BASE_Y - TIP_Y) / HEIGHT          # 19.23

# ---------------------------------------------------------------- palette
WOOD      = (0x8B, 0x5A, 0x33)
WOOD_LITE = (0xA9, 0x72, 0x45)
WOOD_DARK = (0x5E, 0x3A, 0x1F)
WOOD_DEEP = (0x3E, 0x24, 0x12)
PURPLE      = (0x8C, 0x4F, 0xC4)
PURPLE_LITE = (0xAA, 0x74, 0xDC)
PURPLE_DARK = (0x63, 0x33, 0x93)
PURPLE_DEEP = (0x3E, 0x1E, 0x5E)
SILVER      = (0xC3, 0xC8, 0xD2)
SILVER_LITE = (0xE4, 0xE8, 0xEF)
SILVER_DARK = (0x8D, 0x94, 0xA2)
SILVER_DEEP = (0x5C, 0x62, 0x6E)
CREAM      = (0xF2, 0xDF, 0xA8)
CREAM_LITE = (0xFC, 0xF0, 0xC8)
CREAM_DARK = (0xD3, 0xB6, 0x72)
CREAM_DEEP = (0x9E, 0x7F, 0x42)
RED      = (0xE0, 0x30, 0x2A)
RED_LITE = (0xF6, 0x6B, 0x62)
RED_DARK = (0x9E, 0x14, 0x14)
OUTLINE  = (0x2A, 0x1A, 0x10)

# ---------------------------------------------------------------- atlas cells
TILE = 8
COLS, ROWS = 10, 8
TEX_W, TEX_H = COLS * TILE, ROWS * TILE

CELLS = {}
_cells = [
    "wood", "wood_side", "wood_top",
    "purp", "purp_side", "purp_top",
    "silv", "silv_side", "silv_top",
    "cream", "cream_side", "cream_top",
    "red", "red_side", "red_top",
    "panel", "panel_side", "panel_top",
    "rib", "rib_side", "rib_tail",
    "bead",
]
for _i, _n in enumerate(_cells):
    CELLS[_n] = (_i % COLS, _i // COLS)


def uv_of(cell):
    cx, cy = CELLS[cell]
    x0, y0 = cx * TILE, cy * TILE
    return (x0, y0, x0 + TILE, y0 + TILE)


# ---------------------------------------------------------------- geometry
GROUPS = {"shaft": [], "wrap": [], "bands": [], "head": [], "ribbon": []}
BOXES = []


def box(group, name, x1, y1, z1, x2, y2, z2, sprite, rot=None, origin=None):
    e = {
        "name": name,
        "from": [round(min(x1, x2), 3), round(min(y1, y2), 3), round(min(z1, z2), 3)],
        "to": [round(max(x1, x2), 3), round(max(y1, y2), 3), round(max(z1, z2), 3)],
        "sprite": sprite,
    }
    if rot:
        e["rot"] = rot
    if origin:
        e["origin"] = list(origin)
    GROUPS[group].append(e)
    BOXES.append(e)
    return e


def seg(group, name, y0, y1, half, sprite, cx=0.0, z0=0.0, rot=None, depth=None):
    """A length of the shaft.  `half` is the half-width in x; the depth in z is
    capped so a wide part never becomes a cube seen from the front."""
    d = min(half, 2.9) if depth is None else depth
    org = None
    if rot:
        org = (cx, (y0 + y1) / 2.0, z0)
    return box(group, name, cx - half, y0, z0 - d, cx + half, y1, z0 + d,
               {"north": sprite, "south": sprite, "east": sprite + "_side",
                "west": sprite + "_side", "up": sprite + "_top",
                "down": sprite + "_top"},
               rot=rot, origin=org)


def lerp(a, b, f):
    return tuple(a[i] + (b[i] - a[i]) * f for i in range(3))


# =============================================================== lower shaft
# widths taken from the artwork's silhouette (image px / 19.23)
seg("shaft", "foot_tip", 0.0, 2.6, 1.70, "wood", cx=0.15)
seg("shaft", "foot", 2.6, 11.65, 1.75, "wood", cx=0.15)
# purple cloth wrap, gradually swelling
seg("wrap", "wrap_lo", 11.65, 22.3, 1.65, "purp", cx=0.25)
seg("wrap", "wrap_mid", 22.3, 41.5, 1.85, "purp", cx=0.40)
seg("wrap", "wrap_hi", 41.5, 59.3, 2.20, "purp", cx=0.70)
# thin red thread where the wrap ends
seg("bands", "thread", 59.3, 61.0, 2.25, "red", cx=0.80)
# wooden collar and the embossed silver panel
seg("shaft", "collar_lo", 61.0, 64.4, 2.35, "wood", cx=0.90)
seg("bands", "panel", 64.4, 77.2, 3.15, "panel", cx=1.00)
seg("shaft", "neck", 77.2, 83.0, 2.40, "wood", cx=1.05)
seg("bands", "ring_a", 83.0, 85.0, 2.75, "silv", cx=1.05)
seg("shaft", "upper", 85.0, 90.2, 2.20, "wood", cx=1.05)

# =============================================================== the bend
# Axis-aligned steps again: no rotation, so nothing pokes out of the silhouette.
BEND = [
    # (y_bottom, y_top, centre x, half width, material)
    (84.5, 90.0, 1.30, 2.35, "wood"),
    (89.5, 94.5, 0.70, 2.30, "wood"),
    (94.0, 98.0, 0.05, 2.30, "wood"),
]
for i, (y0, y1, cx, hw, sp) in enumerate(BEND):
    seg("head", f"bend_{i}", y0, y1, hw, sp, cx=cx)
# wide silver collar wrapped round the joint, with a rivet (artwork y 96..101.5)
seg("bands", "joint", 95.5, 101.5, 3.05, "silv", cx=-0.50)
box("bands", "joint_rivet", 2.30, 96.6, -0.6, 4.80, 100.2, 0.6, "silv_side")
# the shaft continues up out of the collar before the head proper begins
seg("head", "neck_wood", 100.5, 106.2, 2.55, "wood", cx=-2.60)
seg("bands", "head_ring", 105.8, 108.8, 3.05, "silv", cx=-4.30)
seg("head", "head_wrap", 108.4, 112.4, 2.85, "purp", cx=-5.70)

# =============================================================== bone head
# A big rotated box would balloon its bounding silhouette, so the diagonal head
# is approximated by many small axis-aligned prisms stepped along its
# centreline.  Measured centreline: (-4.0,106) ... (-8.7,120) ... (-12.7,127.5)
def _path_from_edges(rows):
    """Turn (y, x_left, x_right) edge samples into (y, centre, half width)."""
    out = []
    for y, xl, xr in rows:
        out.append((y, (xl + xr) / 2.0, (xr - xl) / 2.0))
    return out


HEAD_PATH = _path_from_edges([
    # y      x_left  x_right      edges measured off the artwork's silhouette
    (106.0, -6.30, -1.80),
    (108.0, -7.60, -2.60),
    (110.0, -9.00, -3.40),
    (112.0, -10.40, -4.20),
    (114.0, -11.90, -5.10),
    (116.0, -13.30, -6.00),
    (118.0, -14.60, -6.90),
    (120.0, -15.00, -7.60),
    (122.0, -15.20, -8.60),
    (124.0, -14.90, -9.80),
    (126.0, -14.20, -11.20),
    (127.6, -13.60, -12.30),
])


def path_sprite(y):
    """Which material the head carries at this height."""
    if y < 108.6:
        return "silv"
    if y < 111.6:
        return "purp"
    return "cream"


def build_head_path():
    """Follow the lean with axis-aligned prisms whose depth tracks their width.

    Rotating the boxes makes their corners poke out of the taper, and stepping
    them with a fixed shallow depth leaves each step's top face exposed; making
    the cross-section square and overlapping the steps avoids both.
    """
    for i in range(len(HEAD_PATH) - 1):
        y0, x0, h0 = HEAD_PATH[i]
        y1, x1, h1 = HEAD_PATH[i + 1]
        ym, xm, hm = (y0 + y1) / 2.0, (x0 + x1) / 2.0, (h0 + h1) / 2.0
        sp = path_sprite(y0)
        # constant depth for every step: varying it with the width made the
        # steps' top faces read as fins in 3/4 view
        box("head", f"head_{i}", xm - hm, y0 - 1.2, -3.0, xm + hm, y1 + 1.2, 3.0,
            {"north": sp, "south": sp, "east": sp + "_side", "west": sp + "_side",
             "up": sp + "_top", "down": sp + "_top"})


build_head_path()

# =============================================================== ribbon + bead
# hangs from the bend, down and to the left, ending in a red teardrop bead
RIB = [
    # (y_bottom, y_top, cx, half)
    (99.0, 103.5, -3.2, 1.5),
    (95.0, 99.0, -4.4, 1.4),
    (91.0, 95.0, -5.6, 1.35),
    (87.0, 91.0, -6.6, 1.3),
    (83.5, 87.0, -7.5, 1.25),
]
for i, (y0, y1, cx, hw) in enumerate(RIB):
    sp = "rib" if i < 3 else "rib_tail"
    box("ribbon", f"ribbon_{i}", cx - hw, y0, -0.7, cx + hw, y1, 0.7,
        {"north": sp, "south": sp, "east": "rib_side", "west": "rib_side",
         "up": sp, "down": "rib_side"})
box("ribbon", "ribbon_tail", -9.0, 80.6, -0.7, -7.2, 83.5, 0.7,
    {"north": "rib_tail", "south": "rib_tail", "east": "rib_side", "west": "rib_side",
     "up": "rib_tail", "down": "rib_tail"})
box("ribbon", "bead", -8.6, 77.0, -0.85, -7.0, 80.6, 0.85,
    {"north": "bead", "south": "bead", "east": "bead", "west": "bead",
     "up": "bead", "down": "bead"})


# ---------------------------------------------------------------- normalise
_ys = [e[k][1] for e in BOXES for k in ("from", "to")]
_MIN, _MAX = min(_ys), max(_ys)
FIT = min(1.0, HEIGHT / (_MAX - _MIN))
_MIN = _MAX - HEIGHT / FIT


def _fy(v):
    return round((v - _MIN) * FIT, 3)


def _fx(v):
    return round(v * FIT, 3)


for _e in BOXES:
    _e["from"] = [_fx(_e["from"][0]), _fy(_e["from"][1]), _e["from"][2]]
    _e["to"] = [_fx(_e["to"][0]), _fy(_e["to"][1]), _e["to"][2]]
    if _e.get("origin"):
        _e["origin"] = [_fx(_e["origin"][0]), _fy(_e["origin"][1]), _e["origin"][2]]

_ys = [e[k][1] for e in BOXES for k in ("from", "to")]
SHIFT = -min(_ys)
if SHIFT:
    for _e in BOXES:
        _e["from"][1] = round(_e["from"][1] + SHIFT, 3)
        _e["to"][1] = round(_e["to"][1] + SHIFT, 3)
        if _e.get("origin"):
            _e["origin"][1] = round(_e["origin"][1] + SHIFT, 3)


def model_to_image(x, y):
    """Map a fitted-model point back onto the artwork."""
    raw_y = (y - SHIFT) / FIT + _MIN
    return SHAFT_X + x / FIT * PX_PER_UNIT, BASE_Y - raw_y * PX_PER_UNIT


def image_to_model(ix, iy):
    return (ix - SHAFT_X) / PX_PER_UNIT * FIT, \
           (((BASE_Y - iy) / PX_PER_UNIT - _MIN) * FIT) + SHIFT


if __name__ == "__main__":
    print(f"FIT {FIT:.4f}  SHIFT {SHIFT:.2f}  boxes {len(BOXES)}")
    for g, items in GROUPS.items():
        print(f"  {g:8s} {len(items)}")
    xs = [e[k][0] for e in BOXES for k in ("from", "to")]
    ys = [e[k][1] for e in BOXES for k in ("from", "to")]
    print(f"bbox x {min(xs):.2f}..{max(xs):.2f}  y {min(ys):.2f}..{max(ys):.2f}")
