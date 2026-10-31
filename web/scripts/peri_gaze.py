"""Peri's gaze video for the footer: her eyes go once round a circle, one frame every 5 degrees.

Takes assets/peri/peri_calm.webp, moves each dark eye (and its shine) a little towards the angle of the frame,
places her on a 1920x1080 cream canvas and writes:
  web/public/peri-gaze.mp4      all-intra, so the page can seek to any frame at once
  web/app/gaze-frames.json      [angle in radians, time in seconds] per frame (0 right, pi/2 down)

  python web/scripts/peri_gaze.py      (needs Pillow, numpy, scipy, opencv-python-headless and ffmpeg)
"""
import json, math, subprocess, tempfile
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "assets/peri/peri_calm.webp"
FRAMES = 72
FPS = 24
CANVAS = (1920, 1080)
SCALE = 0.6
TOP = 156  # where the top of the 1600 px source lands on the canvas
SHIFT = 9  # how far each eye moves, in source pixels
SHINE = 0.45  # how far the shine sits from the eye's centre, as a share of its radius

src = np.array(Image.open(SRC).convert("RGB"))
cream = tuple(int(v) for v in np.median(src[:40, :40].reshape(-1, 3), axis=0))

# The two eyes: the largest dark blobs in the face.
dark = src.astype(int).sum(2) < 200
labels, n = ndimage.label(dark)
eyes = []
for i in range(1, n + 1):
    ys, xs = np.where(labels == i)
    if 600 < ys.mean() < 850 and len(ys) > 8000:
        eyes.append((labels == i))
assert len(eyes) == 2, f"found {len(eyes)} eyes"

base = src.copy()
patches = []
for eye in eyes:
    # The eye as a circle round the dark blob, so the shine on its edge is inside it.
    ys, xs = np.where(eye)
    cy, cx = (ys.min() + ys.max()) / 2, (xs.min() + xs.max()) / 2
    r = max(xs.max() - xs.min(), ys.max() - ys.min()) / 2 + 3
    gy, gx = np.mgrid[:src.shape[0], :src.shape[1]]
    disc = (gy - cy) ** 2 + (gx - cx) ** 2 <= r * r
    # The shine: the largest near-white blob inside the eye.
    bright, _ = ndimage.label(disc & (src.astype(int).min(2) > 200))
    sizes = ndimage.sum(np.ones_like(bright), bright, range(1, bright.max() + 1))
    shine = bright == 1 + int(np.argmax(sizes))
    # The eye without its shine (with the shine's soft glow), the shine on its own, then the face without the eye.
    # Outside the eye is painted eye-dark first, so filling the glow never pulls green in from the face.
    core = ndimage.binary_erosion(disc, iterations=12)
    glow = ndimage.binary_dilation(shine, iterations=14) & core
    inside = src.copy()
    inside[~core] = np.median(src[eye], axis=0)
    plain = np.where(glow[..., None], cv2.inpaint(inside, glow.astype(np.uint8) * 255, 9, cv2.INPAINT_TELEA), src)
    shine = ndimage.binary_dilation(shine, iterations=2)
    sy, sx = np.where(shine)
    shine_box = (sy.min(), sy.max() + 1, sx.min(), sx.max() + 1)
    shine_rgb = src[shine_box[0]:shine_box[1], shine_box[2]:shine_box[3]]
    shine_a = cv2.GaussianBlur(shine[shine_box[0]:shine_box[1], shine_box[2]:shine_box[3]].astype(np.float32), (5, 5), 0)

    alpha = cv2.GaussianBlur(disc.astype(np.float32), (7, 7), 0)
    patches.append(dict(cy=cy, cx=cx, r=r, plain=plain, alpha=alpha, shine_rgb=shine_rgb, shine_a=shine_a,
                        shine_c=((shine_box[0] + shine_box[1]) / 2, (shine_box[2] + shine_box[3]) / 2)))
    hole = ndimage.binary_dilation(disc, iterations=4).astype(np.uint8) * 255
    base = cv2.inpaint(base, hole, 9, cv2.INPAINT_TELEA)


# The picture's own cream is not quite flat: fade its edges into the canvas so no square shows.
ramp = np.clip(np.minimum(np.arange(1600), np.arange(1600)[::-1]) / 140, 0, 1)
FADE = Image.fromarray((np.minimum.outer(ramp, ramp) * 255).astype(np.uint8))


def shifted(img, dx, dy):
    m = np.float32([[1, 0, dx], [0, 1, dy]])
    return cv2.warpAffine(img, m, (img.shape[1], img.shape[0]), flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REFLECT)


def frame(angle):
    out = base.astype(np.float32)
    ux, uy = math.cos(angle), math.sin(angle)
    for p in patches:
        dx, dy = ux * SHIFT, uy * SHIFT
        a = shifted(p["alpha"], dx, dy)[..., None]
        out = out * (1 - a) + shifted(p["plain"], dx, dy).astype(np.float32) * a
        # The shine sits on the side the eye looks to.
        sy = p["cy"] + dy + uy * p["r"] * SHINE - p["shine_rgb"].shape[0] / 2
        sx = p["cx"] + dx + ux * p["r"] * SHINE - p["shine_rgb"].shape[1] / 2
        y0, x0 = int(round(sy)), int(round(sx))
        h, w = p["shine_a"].shape
        sa = p["shine_a"][..., None]
        out[y0:y0 + h, x0:x0 + w] = out[y0:y0 + h, x0:x0 + w] * (1 - sa) + p["shine_rgb"].astype(np.float32) * sa
    peri = Image.fromarray(np.clip(out, 0, 255).astype(np.uint8))
    size = round(peri.width * SCALE)
    peri = peri.resize((size, size), Image.LANCZOS)
    canvas = Image.new("RGB", CANVAS, cream)
    canvas.paste(peri, ((CANVAS[0] - size) // 2, TOP), FADE.resize((size, size)))
    return canvas


eye_x = (patches[0]["cx"] + patches[1]["cx"]) / 2 * SCALE + (CANVAS[0] - round(1600 * SCALE)) // 2
eye_y = (patches[0]["cy"] + patches[1]["cy"]) / 2 * SCALE + TOP
print(f"cream #{cream[0]:02x}{cream[1]:02x}{cream[2]:02x}, eyes' midpoint ({eye_x:.0f},{eye_y:.0f})")

with tempfile.TemporaryDirectory() as tmp:
    table = []
    for k in range(FRAMES):
        angle = 2 * math.pi * k / FRAMES
        frame(angle).save(f"{tmp}/{k:03d}.png")
        table.append([round(angle, 6), round(k / FPS, 5)])
    subprocess.run(["ffmpeg", "-y", "-hide_banner", "-loglevel", "error", "-framerate", str(FPS), "-i", f"{tmp}/%03d.png",
                    "-an", "-c:v", "libx264", "-preset", "slow", "-crf", "20", "-g", "1", "-pix_fmt", "yuv420p",
                    "-movflags", "+faststart", str(ROOT / "web/public/peri-gaze.mp4")], check=True)
(ROOT / "web/app/gaze-frames.json").write_text(json.dumps(table, indent=2) + "\n")
