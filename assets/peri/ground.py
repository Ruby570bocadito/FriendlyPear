"""Puts Peri's feet on peri_calm's ground line in the frames of a move: python assets/peri/ground.py anim_jump anim_drop

Muse keeps her near the middle whatever the prompt says, so a frame standing on the ground is moved up or down (the
cream background fills the gap) until her feet touch the line, and a frame in the air keeps its picture: the height it
is missing goes to sprites/anim_offsets.json (name -> how much higher to draw it, as a fraction of the image height).
"""
import json
import os
import re
import sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SPRITES = os.path.join(HERE, "sprites")
PROMPTS = json.load(open(os.path.join(SPRITES, "prompts.json"), encoding="utf8"))
WORDS = {"tenth": 1 / 10, "sixth": 1 / 6, "fifth": 1 / 5, "quarter": 1 / 4, "third": 1 / 3}


def feet(im):
    """The lowest row with Peri in it, as a fraction of the height."""
    small = im.convert("RGB").resize((400, 400))
    px, bg = small.load(), small.getpixel((2, 2))
    for y in range(399, 0, -1):
        if sum(1 for x in range(400) if sum(abs(a - b) for a, b in zip(px[x, y], bg)) > 70) > 3:
            return y / 400
    return 1.0


ground = feet(Image.open(os.path.join(HERE, "peri_calm.webp")))
log = os.path.join(SPRITES, "anim_offsets.json")
offsets = json.load(open(log)) if os.path.exists(log) else {}
for prefix in sys.argv[1:]:
    for name in sorted(n for n in PROMPTS if n.startswith(prefix + "_") and not n.endswith("_noface")):
        path = os.path.join(SPRITES, f"{name}.webp")
        im = Image.open(path).convert("RGB")
        lift = re.search(r"a (\w+) of the image above where they usually are", PROMPTS[name])
        if lift:
            offsets[name] = round(WORDS[lift.group(1)] - (ground - feet(im)), 3)
            print(f"{name}: in the air, draw {offsets[name]:+.3f} higher")
            continue
        shift = round((ground - feet(im)) * im.height)
        if shift:
            moved = Image.new("RGB", im.size, im.getpixel((2, 2)))
            moved.paste(im, (0, shift))
            moved.save(path, quality=92)
        offsets.pop(name, None)
        print(f"{name}: moved {shift:+d} px onto the ground")
json.dump(offsets, open(log, "w"), indent=2)
