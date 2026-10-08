"""Turn the concept art into Minecraft item textures.

The art is a large illustration on a white background and the staffs are long and
thin (about 1:5), while an item slot is square.  Kept upright, the art would only
occupy a few pixels of width, so it is rotated 45 degrees first - the same trick
vanilla uses for sticks, rods and bones.

Steps:
  white -> transparent, crop to the art, rotate 45, fit into a square, downscale.

Usage:
    python concept_to_item.py [size] [--sheet]

    size     texture edge length, default 64 (32 is the pixel-art look,
             128 is the most legible)
    --sheet  also write tools/_item_options.png comparing sizes and orientations
"""
import sys
import numpy as np
from PIL import Image

ROOT = r"F:\Work\Frilien's Magic"
SRC_DIR = ROOT + r"\tools\source_art"
OUT_DIR = ROOT + r"\src\main\resources\assets\friliensmagic\textures\item"

JOBS = [("frieren_staff_concept.png", "frieren_staff.png"),
        ("phiren_staff_concept.png", "phiren_staff.png")]


def cutout(path):
    """Drop the white background and crop to the artwork."""
    im = Image.open(path).convert("RGBA")
    a = np.asarray(im).astype(np.int16)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    white = np.minimum(np.minimum(r, g), b)
    # keep anti-aliased edges partly opaque instead of hard-cutting them
    a[..., 3] = np.minimum(a[..., 3], np.clip((240 - white) * 255 // 40, 0, 255))
    im = Image.fromarray(a.astype(np.uint8))
    ys, xs = np.nonzero(np.asarray(im)[..., 3] > 8)
    return im.crop((int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1))


def to_item(art, size, rotate=True, margin=1):
    src = art
    if rotate:
        diag = int(np.ceil(np.hypot(src.width, src.height)))
        canvas = Image.new("RGBA", (diag, diag), (0, 0, 0, 0))
        canvas.paste(src, ((diag - src.width) // 2, (diag - src.height) // 2))
        src = canvas.rotate(-45, resample=Image.BICUBIC, expand=False)
        ys, xs = np.nonzero(np.asarray(src)[..., 3] > 8)
        src = src.crop((int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1))

    inner = size - 2 * margin
    k = inner / max(src.width, src.height)
    src = src.resize((max(1, round(src.width * k)), max(1, round(src.height * k))),
                     Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(src, ((size - src.width) // 2, (size - src.height) // 2))
    return out


def write_sheet():
    """Side-by-side comparison of size and orientation, for picking one."""
    tiles = []
    for name, _ in JOBS:
        art = cutout(f"{SRC_DIR}\\{name}")
        for size in (32, 64, 128):
            for rot in (False, True):
                t = to_item(art, size, rotate=rot)
                tiles.append((t, size, rot))
    cell = 160
    cols = 3
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell, rows * cell), (70, 70, 78))
    for i, (t, _, _) in enumerate(tiles):
        big = t.resize((cell - 20, cell - 20), Image.NEAREST)
        bg = Image.new("RGBA", big.size, (70, 70, 78, 255))
        sheet.paste(Image.alpha_composite(bg, big).convert("RGB"),
                    ((i % cols) * cell + 10, (i // cols) * cell + 10))
    sheet.save(ROOT + r"\tools\_item_options.png")
    print("wrote tools/_item_options.png")


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    size = int(args[0]) if args else 64
    for src, dst in JOBS:
        t = to_item(cutout(f"{SRC_DIR}\\{src}"), size)
        t.save(f"{OUT_DIR}\\{dst}")
        op = int((np.asarray(t)[..., 3] > 0).sum())
        print(f"{dst}: {size}x{size}, opaque {op}/{size * size}")
    if "--sheet" in sys.argv:
        write_sheet()
