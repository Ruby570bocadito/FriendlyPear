# Removes the cream background from the story's pictures (people, Peri's story frames, props) with Peri's own cutout
# (assets/peri/cutout.py), into video/public/cut/<name>.webp with transparency (git-ignored: made again from here).
# Run from the repo root, naming each picture by its folder in video/public:
#   python video/scripts/cut.py characters/carmen_walk_01 peri/story_wave_01 props/phone_ring_01
import os
import sys
import tempfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, os.path.join(ROOT, "assets", "peri"))
import cutout  # noqa: E402

PUBLIC = os.path.join(HERE, "..", "public")
OUT = os.path.join(PUBLIC, "cut")
os.makedirs(OUT, exist_ok=True)

for name in sys.argv[1:]:
    picture = Image.open(os.path.join(PUBLIC, name + ".webp")).convert("RGB")
    w, h = picture.size
    side = max(w, h)
    # Peri's cutout works on squares: a portrait goes in the middle of a cream square of its own height, at full size.
    square = Image.new("RGB", (side, side), picture.getpixel((4, 4)))
    square.paste(picture, ((side - w) // 2, (side - h) // 2))
    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, "square.png")
        square.save(path)
        cutout.SIZE = side
        cut = cutout.cut(path, remove_floor=False)
    cut = cut.crop(((side - w) // 2, (side - h) // 2, (side - w) // 2 + w, (side - h) // 2 + h))
    cut.save(os.path.join(OUT, os.path.basename(name) + ".webp"), "WEBP", quality=92)
    print(name, cut.size)
