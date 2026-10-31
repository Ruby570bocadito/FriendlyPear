"""Peri as a rig for the website, like a Rive character: her body without eyes, each eye and the eye's shine as
separate pictures, so the page can move her eyes towards the cursor and blink them. Also copies her other moods
(with their blink frames) and the clay star, all cut out, from the app's drawables.

  python web/scripts/peri_rig.py      (needs Pillow, numpy, scipy and opencv-python-headless)

Writes web/public/peri/*.webp and web/app/peri-rig.json (where each eye sits, as a share of the picture).
"""
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[2]
DRAWABLES = ROOT / "android/app/src/main/res/drawable-nodpi"
OUT = ROOT / "web/public/peri"
SIZE = 720  # the pictures' side on the website
OUT.mkdir(parents=True, exist_ok=True)

src = np.array(Image.open(DRAWABLES / "peri_calm.webp").convert("RGBA"))
rgb, alpha = src[..., :3].copy(), src[..., 3]
side = src.shape[0]

dark = (rgb.astype(int).sum(2) < 200) & (alpha > 200)
labels, n = ndimage.label(dark)
blobs = sorted((ndimage.sum(dark, labels, i), i) for i in range(1, n + 1))[-6:]
eyes = []
for size, i in blobs:
    ys, xs = np.where(labels == i)
    # The eyes: two big round dark blobs in the face's upper half.
    if 0.38 * side < ys.mean() < 0.55 * side and size > 2500:
        eyes.append(labels == i)
eyes.sort(key=lambda m: np.where(m)[1].mean())
assert len(eyes) == 2, f"found {len(eyes)} eyes"

gy, gx = np.mgrid[:side, :side]
body = rgb.copy()
rig = {"eyes": []}
for k, eye in enumerate(eyes):
    ys, xs = np.where(eye)
    cy, cx = (ys.min() + ys.max()) / 2, (xs.min() + xs.max()) / 2
    r = max(xs.max() - xs.min(), ys.max() - ys.min()) / 2 + 2
    disc = (gy - cy) ** 2 + (gx - cx) ** 2 <= r * r
    # The shine: the largest near-white blob in the eye.
    bright, _ = ndimage.label(disc & (rgb.min(2) > 190))
    sizes = ndimage.sum(np.ones_like(bright), bright, range(1, bright.max() + 1))
    shine = bright == 1 + int(np.argmax(sizes))
    core = ndimage.binary_erosion(disc, iterations=8)
    glow = ndimage.binary_dilation(shine, iterations=9) & core
    inside = rgb.copy()
    inside[~core] = np.median(rgb[eye], axis=0)
    plain = np.where(glow[..., None], cv2.inpaint(inside, glow.astype(np.uint8) * 255, 7, cv2.INPAINT_TELEA), rgb)

    # The eye on its own (without its shine), cut to its box with a soft round edge.
    pad = int(r) + 4
    box = (int(cy) - pad, int(cy) + pad, int(cx) - pad, int(cx) + pad)
    a = cv2.GaussianBlur(disc.astype(np.float32), (5, 5), 0)[box[0]:box[1], box[2]:box[3]]
    eye_rgba = np.dstack([plain[box[0]:box[1], box[2]:box[3]], (a * 255).astype(np.uint8)])
    Image.fromarray(eye_rgba).resize((round(2 * pad * SIZE / side),) * 2, Image.LANCZOS).save(OUT / f"eye-{k}.webp", lossless=True)

    sy, sx = np.where(shine)
    sb = (sy.min() - 3, sy.max() + 4, sx.min() - 3, sx.max() + 4)
    sa = cv2.GaussianBlur(ndimage.binary_dilation(shine, iterations=1).astype(np.float32), (3, 3), 0)[sb[0]:sb[1], sb[2]:sb[3]]
    shine_rgba = np.dstack([rgb[sb[0]:sb[1], sb[2]:sb[3]], (sa * 255).astype(np.uint8)])
    sh, sw = shine_rgba.shape[:2]
    Image.fromarray(shine_rgba).resize((max(1, round(sw * SIZE / side)), max(1, round(sh * SIZE / side))), Image.LANCZOS).save(OUT / f"shine-{k}.webp", lossless=True)

    # The face under the eye: fill the socket with the clay around it.
    hole = ndimage.binary_dilation(disc, iterations=3).astype(np.uint8) * 255
    body = cv2.inpaint(body, hole, 9, cv2.INPAINT_TELEA)
    rig["eyes"].append({
        "x": round(cx / side, 4), "y": round(cy / side, 4), "size": round(2 * pad / side, 4), "r": round(r / side, 4),
        "shine": {"x": round(((sb[2] + sb[3]) / 2 - cx) / side, 4), "y": round(((sb[0] + sb[1]) / 2 - cy) / side, 4),
                  "w": round(sw / side, 4), "h": round(sh / side, 4)},
    })

Image.fromarray(np.dstack([body, alpha])).resize((SIZE, SIZE), Image.LANCZOS).save(OUT / "body.webp", quality=90)
(ROOT / "web/app/peri-rig.json").write_text(json.dumps(rig, indent=2) + "\n")

for mood in ["calm", "happy", "worried", "protective", "thinking", "listening"]:
    for frame in ["", "_blink"]:
        Image.open(DRAWABLES / f"peri_{mood}{frame}.webp").resize((SIZE, SIZE), Image.LANCZOS).save(OUT / f"{mood}{frame}.webp", quality=88)
Image.open(DRAWABLES / "peri_fx_star.webp").resize((160, 160), Image.LANCZOS).save(OUT / "star.webp", quality=90)
print(json.dumps(rig))
