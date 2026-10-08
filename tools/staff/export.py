"""
Export the staff model:

  * <project>/blockbench/frieren_staff.bbmodel        - Blockbench project (embedded texture)
  * <project>/blockbench/frieren_staff_atlas.png      - the texture on its own
  * resources/.../models/item/frieren_staff.json      - Java item model (display transforms)
  * resources/.../textures/item/frieren_staff_model.png - texture for the resource pack
"""
import base64
import io
import json
import os
from PIL import Image

import staff_model as M
import texture as TEX

ROOT = r"F:\Work\Frilien's Magic"
BB_DIR = os.path.join(ROOT, "blockbench")
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "friliensmagic")

# ------------------------------------------------------------------ geometry
GROUP_ORDER = ["head", "collar", "ferrule", "ribbon", "shaft"]
GROUP_NAMES = {
    "head": "staff_head",
    "collar": "collar",
    "ferrule": "ferrule",
    "ribbon": "ribbon",
    "shaft": "shaft",
}

BONE_UUIDS = {g: __import__("uuid").uuid4().hex for g in GROUP_ORDER}

# ---------------------------------------------------------------- model space
# Minecraft's block-model loader rejects any element coordinate outside [-16, 32]
# (BlockElement.Deserializer).  A 128-unit staff is far outside that, so the
# geometry is fitted into the box and the display scales are enlarged by the same
# factor to keep the on-screen size unchanged.
LIMIT_LO, LIMIT_HI = -16.0, 32.0
_CENTRE = (LIMIT_LO + LIMIT_HI) / 2.0
_HALF = (LIMIT_HI - LIMIT_LO) / 2.0

_ys = [e[k][1] for e in M.BOXES for k in ("from", "to")]
FIT = _HALF / max(abs(max(_ys) - _CENTRE), abs(min(_ys) - _CENTRE))
FIT = min(FIT, 1.0)


def _scale(v, centre):
    return round((v - centre) * FIT, 4)


for _e in M.BOXES:
    _cy = (_e["from"][1] + _e["to"][1]) / 2.0
    _e["from"] = [_scale(_e["from"][0], 0.0), _scale(_e["from"][1], _CENTRE),
                  _scale(_e["from"][2], 0.0)]
    _e["to"] = [_scale(_e["to"][0], 0.0), _scale(_e["to"][1], _CENTRE),
                _scale(_e["to"][2], 0.0)]
    if _e.get("origin"):
        _o = _e["origin"]
        _e["origin"] = [_scale(_o[0], 0.0), _scale(_o[1], _CENTRE), _scale(_o[2], 0.0)]


def _display(rot, trans, scale):
    """Display transform, enlarged to compensate for the geometry fit."""
    s = scale / FIT
    return {"rotation": rot, "translation": trans, "scale": [s, s, s]}


def faces_for(e):
    sprites = e["sprite"] if isinstance(e["sprite"], dict) else {
        k: e["sprite"] for k in ("north", "south", "east", "west", "up", "down")}
    faces = {}
    for fname in ("north", "south", "east", "west", "up", "down"):
        u1, v1, u2, v2 = M.uv_of(sprites[fname])
        faces[fname] = {"uv": [u1, v1, u2, v2], "texture": 0}
    return faces


def build_bbmodel(atlas):
    elements = []
    outliner = []
    for g in GROUP_ORDER:
        bone_uuid = BONE_UUIDS[g]
        children = []
        for e in M.GROUPS[g]:
            cube = {
                "name": e["name"],
                "from": e["from"],
                "to": e["to"],
                "origin": list(e.get("origin") or [
                    (e["from"][0] + e["to"][0]) / 2,
                    (e["from"][1] + e["to"][1]) / 2,
                    (e["from"][2] + e["to"][2]) / 2,
                ]),
                "faces": faces_for(e),
                "type": "cube",
                "uuid": __import__("uuid").uuid4().hex,
                # Blockbench needs the owning bone's uuid here, otherwise the
                # cube is treated as a top-level element and renders unparented
                "parent": bone_uuid,
            }
            if e.get("rot") and any(e["rot"]):
                cube["rotation"] = list(e["rot"])
            elements.append(cube)
            children.append(cube["uuid"])
        outliner.append({
            "name": GROUP_NAMES[g],
            "origin": [0, 0, 0],
            "uuid": bone_uuid,
            "children": children,
        })

    buf = io.BytesIO()
    atlas.save(buf, format="png")
    data = buf.getvalue()

    model = {
        "meta": {
            "format_version": "4.5",
            "model_format": "java_block",
            "box_uv": False,
        },
        "name": "frieren_staff",
        "parent": "",
        "ambientocclusion": True,
        "front_gui_light": False,
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "resolution": {"width": M.TEX_W, "height": M.TEX_H},
        "elements": elements,
        "outliner": outliner,
        "textures": [{
            "path": "",
            "name": "frieren_staff_model.png",
            "folder": "item",
            "namespace": "friliensmagic",
            "id": "0",
            "particle": False,
            "render_mode": "default",
            "visible": True,
            "mode": "bitmap",
            "saved": False,
            "uuid": __import__("uuid").uuid4().hex,
            "source": "data:image/png;base64," + base64.b64encode(data).decode("ascii"),
        }],
        "display": DISPLAY,
    }
    return model, data


# ------------------------------------------------------------------ display
# The geometry is fitted into the [-16, 32] box Minecraft allows, so every scale
# here is divided by FIT (see _display) to keep the on-screen size unchanged.
DISPLAY = {
    "gui": _display([0, 0, 0], [0, 0, 0], 0.118),
    "ground": _display([0, 0, 0], [0, 0, 0], 0.070),
    "fixed": _display([0, 0, 0], [0, -1.2, 0], 0.105),
    "thirdperson_righthand": _display([0, 55, 22], [0, 1.0, 1.0], 0.30),
    "thirdperson_lefthand": _display([0, -55, -22], [0, 1.0, 1.0], 0.30),
    "firstperson_righthand": _display([0, 50, 20], [0, 1.2, 1.2], 0.30),
    "firstperson_lefthand": _display([0, -50, -20], [0, 1.2, 1.2], 0.30),
}


def build_item_model():
    """A plain Java item model (same geometry, no display block needed if the
    loader applies it, but we include Minecraft's own display format)."""
    elements = []
    for g in GROUP_ORDER:
        for e in M.GROUPS[g]:
            el = {"from": e["from"], "to": e["to"], "faces": {}}
            if e.get("rot") and any(e["rot"]):
                el["rotation"] = {"origin": list(e["origin"]), "axis": "z",
                                  "angle": e["rot"][2], "rescale": False}
            sprites = e["sprite"] if isinstance(e["sprite"], dict) else {
                k: e["sprite"] for k in ("north", "south", "east", "west", "up", "down")}
            for fname in ("north", "south", "east", "west", "up", "down"):
                u1, v1, u2, v2 = M.uv_of(sprites[fname])
                el["faces"][fname] = {"uv": [u1, v1, u2, v2], "texture": "#0"}
            elements.append(el)
    return {
        "credit": "Generated from frieren_staff.png concept art",
        "texture_size": [M.TEX_W, M.TEX_H],
        "textures": {"0": "friliensmagic:item/frieren_staff",
                     "particle": "friliensmagic:item/frieren_staff"},
        "elements": elements,
        "display": DISPLAY,
    }


def validate(bb_model, item_model):
    """Cheap self-checks on what we just wrote, so a bad export is obvious."""
    problems = []
    FACES = ("north", "south", "east", "west", "up", "down")

    # every element needs all six faces with UVs inside the texture
    bad = 0
    for e in item_model["elements"]:
        if set(e["faces"]) != set(FACES):
            bad += 1
            continue
        for f in e["faces"].values():
            u1, v1, u2, v2 = f["uv"]
            if not (0 <= min(u1, u2) and max(u1, u2) <= M.TEX_W
                    and 0 <= min(v1, v2) and max(v1, v2) <= M.TEX_H):
                bad += 1
            if f.get("texture") != "#0":
                bad += 1
        if "rotation" in e and ("origin" not in e["rotation"]
                                or e["rotation"].get("axis") not in "xyz"):
            bad += 1
        if any(e["from"][i] > e["to"][i] for i in range(3)):
            bad += 1
    if bad:
        problems.append(f"{bad} malformed faces/rotations")

    # texture reference must resolve inside the mod's assets
    for key, ref in item_model["textures"].items():
        if ":" in ref:
            ns, path = ref.split(":", 1)
            if ns != "friliensmagic":
                problems.append(f"texture {key} uses namespace {ns}")
                continue
        else:
            path = ref
        if not os.path.isfile(os.path.join(RES, "textures", path + ".png")):
            problems.append(f"texture {key} -> {ref} is missing")
    if tuple(item_model.get("texture_size", ())) != (M.TEX_W, M.TEX_H):
        problems.append("texture_size does not match the atlas")

    # bbmodel integrity
    if bb_model["resolution"] != {"width": M.TEX_W, "height": M.TEX_H}:
        problems.append("bbmodel resolution mismatch")
    if not bb_model["textures"][0]["source"].startswith("data:image/png;base64,"):
        problems.append("bbmodel texture is not embedded")
    ids = {e["uuid"] for e in bb_model["elements"]}
    dangling = [c for b in bb_model["outliner"] for c in b["children"] if c not in ids]
    if dangling:
        problems.append(f"{len(dangling)} dangling bone children")
    bone_ids = {b["uuid"] for b in bb_model["outliner"]}
    unparented = [e["name"] for e in bb_model["elements"]
                  if e.get("parent") not in bone_ids]
    if unparented:
        problems.append(f"{len(unparented)} cubes without a parent bone "
                        f"(e.g. {unparented[:3]})")
    if len(bb_model["elements"]) != len(item_model["elements"]):
        problems.append("bbmodel and item model disagree on element count")
    if sorted(tuple(e["from"]) + tuple(e["to"]) for e in bb_model["elements"]) != \
       sorted(tuple(e["from"]) + tuple(e["to"]) for e in item_model["elements"]):
        problems.append("bbmodel and item model disagree on box bounds")

    # Minecraft's model loader rejects any coordinate outside [-16, 32]
    outside = 0
    for e in item_model["elements"]:
        for key in ("from", "to"):
            for v in e[key]:
                if v < -16.0 or v > 32.0:
                    outside += 1
    if outside:
        problems.append(f"{outside} coordinates outside Minecraft's [-16, 32] limit")

    # GUI view must fit the 16x16 slot
    ys = [e[k][1] for e in item_model["elements"] for k in ("from", "to")]
    span = (max(ys) - min(ys)) * item_model["display"]["gui"]["scale"][1]
    if span > 16.0:
        problems.append(f"gui view is {span:.1f}px, taller than the 16px slot")

    for p in problems:
        print("  !", p)
    return not problems


def main():
    os.makedirs(BB_DIR, exist_ok=True)
    atlas = TEX.build_texture()
    model, data = build_bbmodel(atlas)
    item = build_item_model()

    bb_path = os.path.join(BB_DIR, "frieren_staff.bbmodel")
    with open(bb_path, "w", encoding="utf-8") as f:
        json.dump(model, f, indent=1)
    print("wrote", bb_path, f"({os.path.getsize(bb_path) / 1024:.0f} KB)")

    # resource-pack assets: the item model and the texture share the item's name
    res_tex = os.path.join(RES, "textures", "item", "frieren_staff.png")
    atlas.save(res_tex)
    print("wrote", res_tex)

    res_model = os.path.join(RES, "models", "item", "frieren_staff.json")
    os.makedirs(os.path.dirname(res_model), exist_ok=True)
    with open(res_model, "w", encoding="utf-8") as f:
        json.dump(item, f, indent=1)
    print("wrote", res_model)

    print("validating...")
    if validate(model, item):
        print("all checks passed")
    else:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
