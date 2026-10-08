# Frieren's staff - Blockbench model pipeline

Generates a Blockbench-importable 3D model of Frieren's staff from the concept art
`src/main/resources/assets/friliensmagic/textures/item/frieren_staff.png`.

## Generated files

| File | What it is |
| --- | --- |
| `blockbench/frieren_staff.bbmodel` | Blockbench project (5 groups, 94 cuboids, texture embedded) |
| `src/main/resources/assets/friliensmagic/models/item/frieren_staff.json` | Java item model, ready for the resource pack |
| `src/main/resources/assets/friliensmagic/textures/item/frieren_staff_model.png` | texture the item model references |

## Using it

**Blockbench**: `File > Open Model` and pick `blockbench/frieren_staff.bbmodel`.
The texture is embedded, so it appears immediately. Groups:

* `staff_head` - gem, gold ring, crescent, finial, spokes
* `collar` - the banded gold fitting under the head
* `ferrule` - the gold foot of the staff
* `ribbon` - the red silk band
* `shaft` - the red rod

**Minecraft**: the item model and its texture are already in the resource tree, so
`friliensmagic:frieren_staff` resolves as a custom item model. Display transforms
for `gui`, `ground`, `fixed`, `thirdperson_*` and `firstperson_*` are included and
can be re-tuned with Blockbench's *Display* tab.

## Coordinate conventions

Model units are Minecraft "pixels" (16 units = 1 block). The front view maps
1:1 onto the artwork:

* `y = 0` bottom of the ferrule, `y = 128` tip of the finial
* `x = 0` shaft axis (artwork x = 837 px), `z = 0` mid-plane
* 1 model unit = 18.727 artwork px, i.e. the shaft is 3 units, the staff 128 tall

## Display / held pose

`display` transforms live in `export.py` (`DISPLAY`). They were checked by
rendering the model through Minecraft's transform pipeline
(`render_hand.py` -> `preview_hand.png`):

| view | rotation | translation | scale |
| --- | --- | --- | --- |
| `gui` | 0,0,0 | 0,0,0 | 0.118 |
| `ground` | 0,0,0 | 0,0,0 | 0.070 |
| `fixed` | 0,0,0 | 0,-1.2,0 | 0.105 |
| `thirdperson_righthand` | 0,55,22 | 0,1.0,1.0 | 0.30 |
| `firstperson_righthand` | 0,50,20 | 0,1.2,1.2 | 0.30 |

The model is centred on the staff's middle, so each transform also needs the
grip (about 22 units up) brought to the held origin. That is folded into the
translation at export time: `t = display_t + R(0,-22,0)/16`.

The GUI scale keeps the 128-unit staff at 15.1 px, inside the 16x16 item slot.

## Pipeline

```
staff_model.py   geometry (shaft, ferrule, collar, ring, gem, crescent, finial, ribbon)
texture.py       104x80 pixel-art atlas, one 8x8 tile per material
export.py        writes the .bbmodel, the Java item model and the texture,
                 then runs cheap self-checks (faces, UVs, texture refs, GUI fit)
```

Rebuild everything with:

```
cd tools/staff
python export.py
```

Requires Python 3 with Pillow (`pip install pillow`).

## How the shape was derived

The artwork was measured rather than eyeballed:

* the crescent's outer rim was traced and least-squares fitted to an ellipse
  (275 x 273 px, -6 deg tilt) centred just above the gem
* the crescent spans a little over 228 degrees: its two tips sit at model
  angles -22 and 206 degrees, leaving the open mouth at the lower right
* the band width was measured along the rim's inward normal: ~3.2-4.5 units,
  thinner across the top than on the arms
* the red-painted stretch of the crescent is the -23 deg .. 40 deg arc
* the gem and its ring were read off a pixel map of the artwork: the gem is
  ~180 px across and the ring's outer edge ~260 px
* the finished model is scaled and shifted so it spans exactly 128 units with
  the base at y = 0 (see `FIT` / `SHIFT_UP` in `staff_model.py`)
