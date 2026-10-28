"""
Finds Peri's eyes in each of her illustrations, so the app can dress her: glasses go on her eyes, a hat above them
and a bow tie below, turned with her head. Writes android/.../ui/peri/PeriAnchors.kt (generated, do not edit) and,
with --sheet <png>, a contact sheet with the eyes marked to check them by eye.

    python assets/peri/anchors.py [--sheet sheet.png]

Eyes are the two largest dark blobs at the same height in the upper part of her body; each mood's still image and
each pose (an idea, her cape...) has its own. A mood's frames are edits of its still image, but a step or a gesture
moves her a little (up to 22 px): each frame gets the still's eyes moved as far as her head moved, found by matching
the still's head (leaf, stem and the top of her face, never the eyes or the mouth, which change) in the frame.
Where a pose hides her eyes (peekaboo, spirals), the calm Peri's are used.
"""
import os
import sys

import cv2
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
DRAWABLES = os.path.join(HERE, "..", "..", "android", "app", "src", "main", "res", "drawable-nodpi")
KOTLIN = os.path.join(HERE, "..", "..", "android", "app", "src", "main", "java", "app", "friendlypear", "ui", "peri", "PeriAnchors.kt")

MOODS = ["calm", "listening", "thinking", "worried", "protective", "calling", "waiting", "happy", "sleeping"]
POSES = ["calm_wave1", "calm_wave2", "calm_idea", "calm_sorry", "calm_thumbsup", "calm_laugh", "calm_love", "calm_dizzy",
         "calm_yawn", "calm_letter", "calm_hero", "calm_medal", "calm_peekaboo", "calm_clap", "calm_detective",
         "calm_surprised", "calm_party", "calm_glasses", "calm_scarf", "calm_bye"]
FRAMES = ["_blink", "_blinkhalf", "_talk", "_talkwide", "_talko", "_glanceleft", "_glanceright", "_stepleft", "_stepright",
          "_gesturea", "_gestureb", "_wink"]
# A frame never moves her more than this (px) from her still image; a match further away is a mistake.
REACH = 48
# Poses whose eyes can't be found (hidden by her hands, or spirals): the calm Peri's eyes stand in.
HIDDEN = {"calm_peekaboo", "calm_dizzy"}


def eyes(path: str):
    rgba = np.asarray(Image.open(path).convert("RGBA"))
    alpha = rgba[..., 3] > 128
    ys, xs = np.nonzero(alpha)
    top, bottom, left, right = ys.min(), ys.max(), xs.min(), xs.max()
    height = bottom - top
    rgb = rgba[..., :3].astype(np.int32)
    luma = 0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]
    dark = (luma < 60) & alpha
    band = np.zeros_like(dark)
    band[int(top + 0.28 * height): int(top + 0.66 * height), :] = True
    dark &= band
    count, labels, stats, centroids = cv2.connectedComponentsWithStats(dark.astype(np.uint8), connectivity=8)
    blobs = [(stats[i, cv2.CC_STAT_AREA], centroids[i]) for i in range(1, count) if stats[i, cv2.CC_STAT_AREA] > 120]
    blobs.sort(key=lambda b: -b[0])
    best = None
    for i in range(len(blobs)):
        for j in range(i + 1, len(blobs)):
            (a1, c1), (a2, c2) = blobs[i], blobs[j]
            dx, dy = abs(c1[0] - c2[0]), abs(c1[1] - c2[1])
            if dx < 60 or dx > 360 or dy > 0.35 * dx or min(a1, a2) < 0.25 * max(a1, a2):
                continue
            score = a1 + a2
            if best is None or score > best[0]:
                best = (score, c1, c2)
    if best is None:
        return None
    _, c1, c2 = best
    l, r = (c1, c2) if c1[0] < c2[0] else (c2, c1)
    return float(l[0]), float(l[1]), float(r[0]), float(r[1])


def grey(path: str) -> np.ndarray:
    """Her picture on a mid grey, in grey levels: her outline counts as much as what's inside it."""
    rgba = np.asarray(Image.open(path).convert("RGBA")).astype(np.float32)
    a = rgba[..., 3:4] / 255.0
    rgb = rgba[..., :3] * a + 128.0 * (1 - a)
    return cv2.cvtColor(rgb.astype(np.uint8), cv2.COLOR_RGB2GRAY)


def shift(still: np.ndarray, frame: np.ndarray, eyes_still) -> tuple[int, int, float]:
    """How far her head moved from the still image to a frame, by matching the still's head in the frame."""
    lx, ly, rx, ry = eyes_still
    d = ((rx - lx) ** 2 + (ry - ly) ** 2) ** 0.5
    cx, cy = (lx + rx) / 2, (ly + ry) / 2
    top, bottom = max(0, int(cy - 1.9 * d)), int(cy - 0.35 * d)
    left, right = max(0, int(cx - 1.4 * d)), min(still.shape[1], int(cx + 1.4 * d))
    head = still[top:bottom, left:right]
    y0, x0 = max(0, top - REACH), max(0, left - REACH)
    area = frame[y0: bottom + REACH, x0: right + REACH]
    scores = cv2.matchTemplate(area, head, cv2.TM_CCOEFF_NORMED)
    _, best, _, (bx, by) = cv2.minMaxLoc(scores)
    return bx + x0 - left, by + y0 - top, float(best)


def main() -> None:
    sheet_path = sys.argv[sys.argv.index("--sheet") + 1] if "--sheet" in sys.argv else None
    found = {}
    for name in MOODS + POSES:
        path = os.path.join(DRAWABLES, f"peri_{name}.webp")
        e = None if name in HIDDEN else eyes(path)
        if e is None:
            print(f"{name}: not found, calm's eyes used")
        found[name] = e
    calm = found["calm"]
    assert calm is not None, "the calm Peri's eyes must be found"
    for name, e in found.items():
        if e is None:
            found[name] = calm
    # Each mood's frames: the still's eyes, moved as far as her head moved in that frame.
    for mood in MOODS:
        still = grey(os.path.join(DRAWABLES, f"peri_{mood}.webp"))
        lx, ly, rx, ry = found[mood]
        moved = []
        for f in FRAMES:
            path = os.path.join(DRAWABLES, f"peri_{mood}{f}.webp")
            if not os.path.exists(path):
                continue
            dx, dy, score = shift(still, grey(path), found[mood])
            if score < 0.6:
                dx, dy = 0, 0
            found[f"{mood}{f}"] = (lx + dx, ly + dy, rx + dx, ry + dy)
            if dx or dy:
                moved.append(f"{f[1:]} {dx:+d},{dy:+d}")
        print(f"{mood}: " + (", ".join(moved) if moved else "frames in place"))

    lines = [
        "package app.friendlypear.ui.peri",
        "",
        "import app.friendlypear.R",
        "",
        "/**",
        " * Generated by assets/peri/anchors.py: where Peri's eyes are in each illustration (left x, y, right x, y, in its",
        " * 1024 px square), so her outfit sits on her whatever the pose and whichever frame is on show.",
        " */",
        "internal object PeriAnchors {",
        "    val EYES: Map<Int, FloatArray> = mapOf(",
    ]
    for name, (lx, ly, rx, ry) in found.items():
        lines.append(f"        R.drawable.peri_{name} to floatArrayOf({lx:.0f}f, {ly:.0f}f, {rx:.0f}f, {ry:.0f}f),")
    lines += ["    )", "}", ""]
    open(KOTLIN, "w", encoding="utf-8", newline="\n").write("\n".join(lines))
    print(f"wrote {os.path.relpath(KOTLIN, HERE)} ({len(found)} images)")

    if sheet_path:
        cell = 256
        names = list(found)
        cols = 8
        sheet = Image.new("RGB", (cols * cell, ((len(names) + cols - 1) // cols) * cell), (230, 240, 250))
        for i, name in enumerate(names):
            im = Image.open(os.path.join(DRAWABLES, f"peri_{name}.webp")).convert("RGBA").resize((cell, cell))
            bg = Image.new("RGBA", im.size, (230, 240, 250, 255))
            bg.alpha_composite(im)
            d = ImageDraw.Draw(bg)
            lx, ly, rx, ry = (v * cell / 1024 for v in found[name])
            for x, y in ((lx, ly), (rx, ry)):
                d.ellipse((x - 5, y - 5, x + 5, y + 5), outline=(255, 0, 0), width=2)
            d.text((4, 4), name, fill=(0, 0, 0))
            sheet.paste(bg.convert("RGB"), ((i % cols) * cell, (i // cols) * cell))
        sheet.save(sheet_path)
        print(f"sheet: {sheet_path}")


if __name__ == "__main__":
    main()
