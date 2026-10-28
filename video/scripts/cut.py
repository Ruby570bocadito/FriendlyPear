# Removes the cream background from the story's people (video/public/characters/<name>.webp) with Peri's own
# cutout (assets/peri/cutout.py), into <name>_cut.webp with transparency. Run from the repo root:
#   python video/scripts/cut.py carmen carmen_relieved
import os
import sys
import tempfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, os.path.join(ROOT, "assets", "peri"))
import cutout  # noqa: E402

PEOPLE = os.path.join(HERE, "..", "public", "characters")

for name in sys.argv[1:]:
    person = Image.open(os.path.join(PEOPLE, name + ".webp")).convert("RGB")
    w, h = person.size
    # Peri's cutout works on squares: the portrait goes in the middle of a cream square of its own height, at full size.
    square = Image.new("RGB", (h, h), person.getpixel((4, 4)))
    square.paste(person, ((h - w) // 2, 0))
    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, "square.png")
        square.save(path)
        cutout.SIZE = h
        cut = cutout.cut(path, remove_floor=False)
    cut = cut.crop(((h - w) // 2, 0, (h - w) // 2 + w, h))
    cut.save(os.path.join(PEOPLE, name + "_cut.webp"), "WEBP", quality=92)
    print(name, cut.size)
