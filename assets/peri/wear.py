"""
Cuts out the accessories Peri can wear (assets/peri/peri_wear_*.webp, from `generate.ts wear`), trims each to what's
drawn and writes the copies the app uses, at most 512 px on their longer side.

    python assets/peri/wear.py
"""
import glob
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from cutout import OUT, cut  # noqa: E402

MAX_SIDE = 512
# Shaped for a pear's head: only the top of the flower headband, its arch, like a crown.
KEEP_TOP = {"peri_wear_flowers.webp": 0.40}


def main() -> None:
    for path in sorted(glob.glob(os.path.join(HERE, "peri_wear_*.webp"))):
        name = os.path.basename(path)
        image = cut(path, remove_floor=False)
        box = image.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox()
        image = image.crop(box)
        if name in KEEP_TOP:
            image = image.crop((0, 0, image.width, int(image.height * KEEP_TOP[name])))
            image = image.crop(image.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox())
        image.thumbnail((MAX_SIDE, MAX_SIDE), Image.LANCZOS)
        image.save(os.path.join(OUT, name), "WEBP", quality=92, alpha_quality=100, method=6)
        print(f"{name}: {image.width}x{image.height}")


if __name__ == "__main__":
    main()
