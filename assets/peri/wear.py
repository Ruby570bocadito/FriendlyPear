"""
Cuts out the accessories Peri can wear, and the sprites of her effects (peri_fx_*: stars), (assets/peri/peri_wear_*.webp, from `generate.ts wear`), trims each to what's
drawn, centres it on its own mirror axis (a bow tie's knot, the bridge of a pair of glasses) and writes the copies the
app uses, at most 512 px on their longer side. For glasses it also says how wide to wear them so each lens sits on an
eye (`width` in Outfit.kt and backend/src/pears/look.ts).

    python assets/peri/wear.py
"""
import glob
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from cutout import OUT, cut  # noqa: E402

MAX_SIDE = 512
# Her effects' sprites (stars that burst out of her) are small on screen; her village's scenery a little bigger.
FX_SIDE = 192
SCENE_SIDE = 384
# Shaped for a pear's head: only the top of the flower headband, its arch, like a crown.
KEEP_TOP = {"peri_wear_flowers.webp": 0.40}
# A white frame nearly the colour of the cream: cut closer, or the cream takes it.
THRESHOLDS = {"peri_wear_hearts.webp": 36}
# Worn on her eyes: one lens on each.
EYEWEAR = {"peri_wear_glasses.webp", "peri_wear_hearts.webp", "peri_wear_sunglasses.webp"}


def trimmed(image: Image.Image) -> Image.Image:
    return image.crop(image.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox())


def mirror_axis(image: Image.Image) -> float:
    """The x (px) about which the picture best matches its own mirror image."""
    alpha = np.asarray(image.getchannel("A"), dtype=np.float32) / 255
    w = alpha.shape[1]
    best, axis = -1.0, (w - 1) / 2
    # Twice the axis, in whole pixels, within a tenth of the width of the middle.
    for twice in range(int(w - 1 - 0.1 * w), int(w - 1 + 0.1 * w) + 1):
        lo, hi = max(0, twice - (w - 1)), min(w - 1, twice)
        if hi - lo < w // 2:
            continue
        xs = np.arange(lo, hi + 1)
        score = np.minimum(alpha[:, xs], alpha[:, twice - xs]).sum()
        if score > best:
            best, axis = score, twice / 2
    return axis


def centred(image: Image.Image) -> Image.Image:
    """Padded on one side so its mirror axis is the middle of the picture: placed at 0.5, it sits square."""
    axis = mirror_axis(image)
    left, right = axis, image.width - 1 - axis
    half = int(np.ceil(max(left, right)))
    out = Image.new("RGBA", (2 * half + 1, image.height), (0, 0, 0, 0))
    out.paste(image, (int(round(half - axis)), 0))
    return out


def lens_span(image: Image.Image) -> float:
    """How far apart the two lenses' centres are, as a share of the picture's width."""
    alpha = np.asarray(image.getchannel("A"), dtype=np.float32)
    w = alpha.shape[1]
    xs = np.arange(w)
    weight = alpha.sum(axis=0)
    left = (xs[: w // 2] * weight[: w // 2]).sum() / weight[: w // 2].sum()
    right = (xs[w // 2:] * weight[w // 2:]).sum() / weight[w // 2:].sum()
    return (right - left) / w


def main() -> None:
    for path in sorted(glob.glob(os.path.join(HERE, "peri_wear_*.webp")) + glob.glob(os.path.join(HERE, "peri_fx_*.webp")) + glob.glob(os.path.join(HERE, "peri_scene_*.webp"))):
        name = os.path.basename(path)
        image = trimmed(cut(path, remove_floor=False, threshold=THRESHOLDS.get(name, 80)))
        if name in KEEP_TOP:
            image = trimmed(image.crop((0, 0, image.width, int(image.height * KEEP_TOP[name]))))
        image = centred(image)
        side = FX_SIDE if name.startswith("peri_fx_") else SCENE_SIDE if name.startswith("peri_scene_") else MAX_SIDE
        image.thumbnail((side, side), Image.LANCZOS)
        image.save(os.path.join(OUT, name), "WEBP", quality=92, alpha_quality=100, method=6)
        note = f", lenses on the eyes at width {1 / lens_span(image):.2f}" if name in EYEWEAR else ""
        print(f"{name}: {image.width}x{image.height}{note}")


if __name__ == "__main__":
    main()
