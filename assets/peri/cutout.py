"""
Cuts the plain cream background out of Peri's Muse Image illustrations, so she stands on any screen (the app's
own background, or other apps in Mini Peri mode), and writes the copies the app uses at 1024 px.

    python assets/peri/cutout.py            needs Pillow and numpy

The background is what is connected to the image's border and close to the cream colour, plus cream pockets
enclosed by Peri (between an arm and the body) that are larger than an eye. Edges get a light feather and the
cream is unmixed from them, so there is no pale halo. The baked floor shadow goes too: the app draws its own,
which moves with her.
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "android", "app", "src", "main", "res", "drawable-nodpi")

STATES = ["calm", "listening", "thinking", "worried", "protective", "calling", "waiting", "happy", "sleeping"]
# Frames the app animates with; the sway frames stay in assets/ (the app sways her with a smooth tilt instead).
FRAMES = ["", "_blink", "_blinkhalf", "_talk", "_talkwide", "_talko", "_glanceleft", "_glanceright"]
EXTRAS = ["calm_wave1", "calm_wave2", "family", "report", "shield", "icon"]

FILL_THRESHOLD = 80  # sum of channel differences from the cream, for the flood fill from the border
POCKET_THRESHOLD = 18  # enclosed pockets must be almost exactly cream
POCKET_MIN_AREA = 2500  # and larger than an eye: eye whites are sometimes the very same cream
SIZE = 1024  # Muse Image returns 1600 px; 1024 is sharp on any phone and light on memory
SENTINEL = (1, 254, 3)


def background_colour(rgb: np.ndarray) -> np.ndarray:
    border = np.concatenate([rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]])
    return np.median(border, axis=0)


def large_regions(mask: Image.Image) -> np.ndarray:
    """The white regions of a mask with at least POCKET_MIN_AREA pixels."""
    labels = mask.convert("I")
    keep = np.zeros((mask.height, mask.width), dtype=bool)
    label = 2
    while True:
        ys, xs = np.nonzero(np.asarray(labels) == 255)
        if len(ys) == 0:
            return keep
        ImageDraw.floodfill(labels, (int(xs[0]), int(ys[0])), label)
        region = np.asarray(labels) == label
        if region.sum() >= POCKET_MIN_AREA:
            keep |= region
        label += 1


def cut(path: str) -> Image.Image:
    image = Image.open(path).convert("RGB").resize((SIZE, SIZE), Image.LANCZOS)
    rgb = np.asarray(image).astype(np.int32)
    bg = background_colour(rgb)
    diff = np.abs(rgb - bg).sum(axis=2)

    filled = image.copy()
    h, w = rgb.shape[:2]
    step = 8
    seeds = [(x, 0) for x in range(0, w, step)] + [(x, h - 1) for x in range(0, w, step)]
    seeds += [(0, y) for y in range(0, h, step)] + [(w - 1, y) for y in range(0, h, step)]
    for x, y in seeds:
        if diff[y, x] < FILL_THRESHOLD and filled.getpixel((x, y)) != SENTINEL:
            ImageDraw.floodfill(filled, (x, y), SENTINEL, thresh=FILL_THRESHOLD)
    outside = np.all(np.asarray(filled) == SENTINEL, axis=2)

    # Cream pockets enclosed by her outline: an opening drops thin strokes and small highlights.
    pockets = Image.fromarray(((diff < POCKET_THRESHOLD) & ~outside).astype(np.uint8) * 255)
    pockets = pockets.filter(ImageFilter.MinFilter(7)).filter(ImageFilter.MaxFilter(7))
    transparent = outside | large_regions(pockets)

    alpha = Image.fromarray(np.where(transparent, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.8))
    a = np.asarray(alpha).astype(np.float32) / 255.0
    # Unmix the cream from half-transparent edge pixels: c = a * fg + (1 - a) * bg.
    safe = np.maximum(a, 1e-3)[..., None]
    fg = np.clip((rgb - (1 - a[..., None]) * bg) / safe, 0, 255)
    fg = np.where(a[..., None] > 0.98, rgb, fg)
    out = np.dstack([fg, a * 255]).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def main() -> None:
    names = [f"{s}{f}" for s in STATES for f in FRAMES] + EXTRAS
    only = sys.argv[1:]
    os.makedirs(OUT, exist_ok=True)
    for name in names:
        if only and name not in only:
            continue
        src = os.path.join(HERE, f"peri_{name}.webp")
        if not os.path.exists(src):
            print(f"missing {name}")
            continue
        cut(src).save(os.path.join(OUT, f"peri_{name}.webp"), "WEBP", quality=92, alpha_quality=100, method=6)
        print(f"{name}: ok")
    # Sway frames are no longer animated in the app.
    for s in STATES:
        for f in ("_swayleft", "_swayright"):
            stale = os.path.join(OUT, f"peri_{s}{f}.webp")
            if os.path.exists(stale):
                os.remove(stale)


if __name__ == "__main__":
    main()
