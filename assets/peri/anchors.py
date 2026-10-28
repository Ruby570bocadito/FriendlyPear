"""
Finds Peri's eyes in each of her illustrations, so the app can dress her: glasses go on her eyes, a hat above them
and a bow tie below, turned with her head. Writes android/.../ui/peri/PeriAnchors.kt (generated, do not edit) and,
with --sheet <png>, a contact sheet with the eyes marked to check them by eye.

    python assets/peri/anchors.py [--sheet sheet.png]

Eyes are the two largest dark blobs at the same height in the upper part of her body, each then taken whole (its lit
side and its glint too, or a lit eye would pull the outfit to the left); each mood's still image and
each pose (an idea, her cape...) has its own. A mood's frames are edits of its still image, but a step or a gesture
moves her a little (up to 22 px): each frame gets the still's eyes moved as far as her head moved, found by matching
the still's head (leaf, stem and the top of her face, never the eyes or the mouth, which change) in the frame.
Where a pose hides her eyes (peekaboo, spirals), the calm Peri's are used.

Two anchors per picture: HEADS, where the top of her head is (the still's eyes moved as far as her leaf and stem moved),
for hats; and EYES, her face, for glasses and what she wears under her smile. In a frame whose own eyes can be measured
(not a blink) they are used for the face: when she looks to one side her face turns while the top of her head stays.
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
         "calm_surprised", "calm_party", "calm_glasses", "calm_scarf", "calm_bye", "calm_hum", "calm_butterfly", "calm_tea", "calm_flower", "calm_phone"]
FRAMES = ["_blink", "_blinkhalf", "_talk", "_talkwide", "_talko", "_glanceleft", "_glanceright", "_stepleft", "_stepright",
          "_gesturea", "_gestureb", "_wink", "_steplefthalf", "_steprighthalf"]
# A frame never moves her more than this (px) from her still image; a match further away is a mistake.
REACH = 48
# Poses whose eyes can't be found (hidden by her hands, or spirals): the calm Peri's eyes stand in.
HIDDEN = {"calm_peekaboo", "calm_dizzy"}
# Darker than this around a pupil is still the eye: its lit side and the rim of its glint.
WHOLE_EYE = 80


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
    (lx, ly), (rx, ry) = whole(luma, l), whole(luma, r)
    return lx, ly, rx, ry


def whole(luma: np.ndarray, at) -> tuple[float, float]:
    """The centre of the whole eye around a dark blob: a looser threshold, then its outline's convex hull."""
    x, y = int(at[0]), int(at[1])
    r = 80
    x0, y0 = max(0, x - r), max(0, y - r)
    crop = (luma[y0: y + r, x0: x + r] < WHOLE_EYE).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(crop, connectivity=4)
    label = labels[y - y0, x - x0]
    if label == 0:
        # The blob's centre can fall on a lighter spot of the eye: then the biggest dark shape around it.
        if count < 2:
            return float(at[0]), float(at[1])
        label = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    contours, _ = cv2.findContours((labels == label).astype(np.uint8), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    hull = cv2.convexHull(max(contours, key=cv2.contourArea))
    m = cv2.moments(cv2.drawContours(np.zeros(crop.shape, np.uint8), [hull], -1, 1, -1), True)
    if m["m00"] == 0:
        return float(at[0]), float(at[1])
    return x0 + m["m10"] / m["m00"], y0 + m["m01"] / m["m00"]


def trusted(seen, head) -> bool:
    """A frame's own eyes, if they look like her eyes: as far apart as usual and close to where her head says (a blink
    or a wink finds something else)."""
    if seen is None:
        return False
    lx, ly, rx, ry = head
    d0 = ((rx - lx) ** 2 + (ry - ly) ** 2) ** 0.5
    d1 = ((seen[2] - seen[0]) ** 2 + (seen[3] - seen[1]) ** 2) ** 0.5
    near = max(abs(seen[0] - lx), abs(seen[1] - ly), abs(seen[2] - rx), abs(seen[3] - ry)) < 0.35 * d0
    return 0.9 <= d1 / d0 <= 1.1 and near


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
    # The top of her head only (leaf, stem, its outline): her face may turn while it stays.
    top, bottom = max(0, int(cy - 1.9 * d)), int(cy - 0.9 * d)
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
    heads = dict(found)
    faces = 0
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
            head = (lx + dx, ly + dy, rx + dx, ry + dy)
            heads[f"{mood}{f}"] = head
            seen = eyes(path)
            if trusted(seen, head):
                faces += 1
            found[f"{mood}{f}"] = seen if trusted(seen, head) else head
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
        "    /** Her face: glasses go on these eyes, and what she wears under her smile below them. */",
        "    val EYES: Map<Int, FloatArray> = mapOf(",
    ]
    for name, (lx, ly, rx, ry) in found.items():
        lines.append(f"        R.drawable.peri_{name} to floatArrayOf({lx:.0f}f, {ly:.0f}f, {rx:.0f}f, {ry:.0f}f),")
    lines += ["    )", "", "    /** The top of her head, given as where her eyes would be if her face hadn't turned: hats go by these. */",
              "    val HEADS: Map<Int, FloatArray> = mapOf("]
    for name, (lx, ly, rx, ry) in heads.items():
        lines.append(f"        R.drawable.peri_{name} to floatArrayOf({lx:.0f}f, {ly:.0f}f, {rx:.0f}f, {ry:.0f}f),")
    lines += ["    )", "}", ""]
    print(f"{faces} frames use their own eyes for her face")
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
