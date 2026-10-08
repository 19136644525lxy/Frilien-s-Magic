"""Check that every registered item has a model, a texture and both lang entries.

Items are registered in Java; models are NOT - they are data-driven, found by
convention at models/item/<name>.json.  This script cross-checks the two sides.
"""
import json
import os
import re

ROOT = r"F:\Work\Frilien's Magic"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "friliensmagic")
JAVA_SRC = os.path.join(ROOT, "src", "main", "java")
ID = "friliensmagic"

# scan every source file so registration can live in its own package
src = ""
for dirpath, _dirs, files in os.walk(JAVA_SRC):
    for f in files:
        if f.endswith(".java"):
            src += open(os.path.join(dirpath, f), encoding="utf-8").read()

registered = sorted(set(re.findall(r'ITEMS\.register\w*\(\s*"([a-z0-9_]+)"', src)))
print("registered items in Java:", registered)

en = json.load(open(os.path.join(ASSETS, "lang", "en_us.json"), encoding="utf-8"))
zh = json.load(open(os.path.join(ASSETS, "lang", "zh_cn.json"), encoding="utf-8"))

problems = []
for name in registered:
    key = f"item.{ID}.{name}"
    for lang, data in (("en_us", en), ("zh_cn", zh)):
        if key not in data:
            problems.append(f"{lang} missing {key}")

    model = os.path.join(ASSETS, "models", "item", f"{name}.json")
    if not os.path.isfile(model):
        problems.append(f"{name}: missing models/item/{name}.json")
        continue

    m = json.load(open(model, encoding="utf-8"))
    parent = m.get("parent", "")
    kind = "2D" if parent.endswith("item/generated") else "3D"

    refs = m.get("textures", {})
    if kind == "2D":
        # a generated model draws its layers; layer0 is the required one
        if "layer0" not in refs:
            problems.append(f"{name}: 2D model has no layer0 texture")
            continue
        refs = {"layer0": refs["layer0"]}

    for slot, ref in refs.items():
        if ":" in ref:
            ns, path = ref.split(":", 1)
            if ns != ID:
                problems.append(f"{name}: texture {slot} -> {ref} wrong namespace")
                continue
        else:
            path = ref
        tex = os.path.join(ASSETS, "textures", path + ".png")
        if not os.path.isfile(tex):
            problems.append(f"{name}: texture {ref} not found at textures/{path}.png")
            continue
        from PIL import Image
        im = Image.open(tex)
        extra = ""
        if kind == "2D":
            # a square texture is expected: the art is drawn rotated to fill the slot
            if im.width != im.height:
                extra = f"  (not square: {im.width}x{im.height})"
        print(f"  {name}: {kind} model ok, texture {im.size} {im.mode}{extra}")

for lang, data in (("en_us", en), ("zh_cn", zh)):
    for name in registered:
        v = data.get('item.' + ID + '.' + name, "")
        # print codepoints too: a console that mangles CJK still shows the real text
        print(f"  {lang}: {v}   {' '.join(hex(ord(c)) for c in v)}")

for p in problems:
    print("  !", p)
print("ALL OK" if not problems else f"{len(problems)} PROBLEM(S)")
