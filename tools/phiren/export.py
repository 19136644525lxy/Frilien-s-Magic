"""Export the phiren staff: .bbmodel + Minecraft item model + texture."""
import base64
import io
import json
import os
import uuid
from PIL import Image

import phiren_model as M
import phiren_texture as TEX

ROOT = r"F:\Work\Frilien's Magic"
BB_DIR = os.path.join(ROOT, "blockbench")
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "friliensmagic")

GROUP_ORDER = ["head", "bands", "wrap", "shaft", "ribbon"]
GROUP_NAMES = {
    "head": "staff_head",
    "bands": "silver_bands",
    "wrap": "cloth_wrap",
    "shaft": "shaft",
    "ribbon": "ribbon",
}
BONE_UUIDS = {g: uuid.uuid4().hex for g in GROUP_ORDER}

# ---------------------------------------------------------------- model space
# Minecraft's block-model loader rejects any element coordinate outside [-16, 32]
# (BlockElement.Deserializer), so the 128-unit geometry is fitted into that box
# and the display scales are enlarged by the same factor.
LIMIT_LO, LIMIT_HI = -16.0, 32.0
_CENTRE = (LIMIT_LO + LIMIT_HI) / 2.0
_HALF = (LIMIT_HI - LIMIT_LO) / 2.0

_ys = [e[k][1] for e in M.BOXES for k in ("from", "to")]
FIT = min(_HALF / max(abs(max(_ys) - _CENTRE), abs(min(_ys) - _CENTRE)), 1.0)


def _scale(v, centre):
    return round((v - centre) * FIT, 4)


for _e in M.BOXES:
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
FACES = ("north", "south", "east", "west", "up", "down")


def faces_for(e):
    sprites = e["sprite"] if isinstance(e["sprite"], dict) else {k: e["sprite"] for k in FACES}
    out = {}
    for f in FACES:
        u1, v1, u2, v2 = M.uv_of(sprites[f])
        out[f] = {"uv": [u1, v1, u2, v2], "texture": 0}
    return out


# ---------------------------------------------------------------- display
# The geometry is fitted into the [-16, 32] box Minecraft allows, so each scale
# below is divided by FIT (see _display) to keep the on-screen size unchanged.
DISPLAY = {
    "gui": _display([0, 0, 0], [0, 0, 0], 0.118),
    "ground": _display([0, 0, 0], [0, 0, 0], 0.070),
    "fixed": _display([0, 0, 0], [0, -1.2, 0], 0.105),
    "thirdperson_righthand": _display([0, 55, 22], [0, 1.0, 1.0], 0.30),
    "thirdperson_lefthand": _display([0, -55, -22], [0, 1.0, 1.0], 0.30),
    "firstperson_righthand": _display([0, 50, 20], [0, 1.2, 1.2], 0.30),
    "firstperson_lefthand": _display([0, -50, -20], [0, 1.2, 1.2], 0.30),
}


def build_bbmodel(atlas):
    elements, outliner = [], []
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
                    (e["from"][2] + e["to"][2]) / 2]),
                "faces": faces_for(e),
                "type": "cube",
                "uuid": uuid.uuid4().hex,
                # Blockbench needs the owning bone here or the cube renders unparented
                "parent": bone_uuid,
            }
            if e.get("rot") and any(e["rot"]):
                cube["rotation"] = list(e["rot"])
            elements.append(cube)
            children.append(cube["uuid"])
        outliner.append({"name": GROUP_NAMES[g], "origin": [0, 0, 0],
                         "uuid": bone_uuid, "children": children})

    buf = io.BytesIO()
    atlas.save(buf, format="png")
    data = buf.getvalue()
    model = {
        "meta": {"format_version": "4.5", "model_format": "java_block", "box_uv": False},
        "name": "phiren_staff",
        "parent": "",
        "ambientocclusion": True,
        "front_gui_light": False,
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "resolution": {"width": M.TEX_W, "height": M.TEX_H},
        "elements": elements,
        "outliner": outliner,
        "textures": [{
            "path": "", "name": "phiren_staff_model.png", "folder": "item",
            "namespace": "friliensmagic", "id": "0", "particle": False,
            "render_mode": "default", "visible": True, "mode": "bitmap",
            "saved": False, "uuid": uuid.uuid4().hex,
            "source": "data:image/png;base64," + base64.b64encode(data).decode("ascii"),
        }],
        "display": DISPLAY,
    }
    return model, data


def build_item_model():
    elements = []
    for g in GROUP_ORDER:
        for e in M.GROUPS[g]:
            el = {"from": e["from"], "to": e["to"], "faces": {}}
            if e.get("rot") and any(e["rot"]):
                el["rotation"] = {"origin": list(e["origin"]), "axis": "z",
                                  "angle": e["rot"][2], "rescale": False}
            sprites = e["sprite"] if isinstance(e["sprite"], dict) else {
                k: e["sprite"] for k in FACES}
            for f in FACES:
                u1, v1, u2, v2 = M.uv_of(sprites[f])
                el["faces"][f] = {"uv": [u1, v1, u2, v2], "texture": "#0"}
            elements.append(el)
    return {
        "credit": "Generated from phiren_staff.png concept art",
        "texture_size": [M.TEX_W, M.TEX_H],
        "textures": {"0": "friliensmagic:item/phiren_staff",
                     "particle": "friliensmagic:item/phiren_staff"},
        "elements": elements,
        "display": DISPLAY,
    }


def validate(bb_model, item_model):
    problems = []
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

    if bb_model["resolution"] != {"width": M.TEX_W, "height": M.TEX_H}:
        problems.append("bbmodel resolution mismatch")
    if not bb_model["textures"][0]["source"].startswith("data:image/png;base64,"):
        problems.append("bbmodel texture is not embedded")
    ids = {e["uuid"] for e in bb_model["elements"]}
    dangling = [c for b in bb_model["outliner"] for c in b["children"] if c not in ids]
    if dangling:
        problems.append(f"{len(dangling)} dangling bone children")
    bone_ids = {b["uuid"] for b in bb_model["outliner"]}
    unparented = [e["name"] for e in bb_model["elements"] if e.get("parent") not in bone_ids]
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

    # the x axis must straddle the shaft: the model should be roughly centred
    xs = [e[k][0] for e in item_model["elements"] for k in ("from", "to")]
    if abs((min(xs) + max(xs)) / 2.0) > 6.0:
        problems.append(f"model x centre is {(min(xs) + max(xs)) / 2:.1f}, "
                        "the shaft should sit near x = 0")

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
    model, _ = build_bbmodel(atlas)
    item = build_item_model()

    bb_path = os.path.join(BB_DIR, "phiren_staff.bbmodel")
    with open(bb_path, "w", encoding="utf-8") as f:
        json.dump(model, f, indent=1)
    print("wrote", bb_path, f"({os.path.getsize(bb_path) / 1024:.0f} KB)")

    res_tex = os.path.join(RES, "textures", "item", "phiren_staff.png")
    atlas.save(res_tex)
    print("wrote", res_tex)

    res_model = os.path.join(RES, "models", "item", "phiren_staff.json")
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
