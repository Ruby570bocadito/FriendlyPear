"""
Finds Peri's eyes in each of her illustrations, so the app can dress her: glasses go on her eyes, a hat above them
and a bow tie below, turned with her head. Writes android/.../ui/peri/PeriAnchors.kt (generated, do not edit) and,
with --sheet <png>, a contact sheet with the eyes marked to check them by eye.

    python assets/peri/anchors.py [--sheet sheet.png]

Each mood's still image has its eyes found: the two largest dark blobs at the same height in the upper part of her
body, each then taken whole (its lit side and its glint too, or a lit eye would pull the outfit to the left). Every
other picture is an edit of one of those, and its eyes are the still's eyes carried over by matching the two pictures:
features of her head and face (leaf, stem, outline, cheeks) give the turn, size and shift between them (SIFT, RANSAC).
So a pose with her eyes shut (humming, smelling a flower) no longer takes her mouth, an arm or the flower for her eyes,
and a half blink or a step never moves her glasses on its own. Poses are matched to the calm Peri; a mood's frames to
its still image.

Two anchors per picture: HEADS, where the top of her head is, for hats; and EYES, her face, for glasses and what she
wears under her smile. They are the same except in a glance, where her face turns while her head stays: there her own
eyes, when they can be measured, place her face.
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
# Frames where her face turns away from her head: their own eyes place her face.
GLANCES = {"_glanceleft", "_glanceright"}
# A match needs this many features agreeing, and a turn and size she can have; otherwise it's a mistake.
AGREE = 12
MAX_TURN = 25.0
SIZES = (0.7, 1.35)
SIFT = cv2.SIFT_create(nfeatures=4000)
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


def grey(path: str):
    """Her picture on cream in grey levels, and where she is (her outline, a little inside)."""
    rgba = np.asarray(Image.open(path).convert("RGBA")).astype(np.float32)
    a = rgba[..., 3:4] / 255.0
    rgb = rgba[..., :3] * a + np.array([250, 240, 214], np.float32) * (1 - a)
    inside = cv2.erode((rgba[..., 3] > 128).astype(np.uint8), np.ones((9, 9), np.uint8))
    return cv2.cvtColor(rgb.astype(np.uint8), cv2.COLOR_RGB2GRAY), inside


def follow(ref: str, ref_eyes, path: str):
    """[ref_eyes] carried from the picture [ref] to the picture [path]: the turn, size and shift between them, from the
    features of her head and face that both share (most do: leaf, stem, outline, cheeks); None if too few agree."""
    g0, in0 = grey(ref)
    g1, in1 = grey(path)
    eye_y = (ref_eyes[1] + ref_eyes[3]) / 2
    # Her head and face, down to a little below her mouth (her arms and what she holds move).
    in0[int(eye_y + 150):] = 0
    in1[int(eye_y + 260):] = 0
    k0, d0 = SIFT.detectAndCompute(g0, in0 * 255)
    k1, d1 = SIFT.detectAndCompute(g1, in1 * 255)
    if d0 is None or d1 is None or len(k0) < AGREE or len(k1) < AGREE:
        return None
    good = [m for m, n in (p for p in cv2.BFMatcher().knnMatch(d0, d1, k=2) if len(p) == 2) if m.distance < 0.75 * n.distance]
    if len(good) < AGREE:
        return None
    src = np.float32([k0[m.queryIdx].pt for m in good])
    dst = np.float32([k1[m.trainIdx].pt for m in good])
    m, agree = cv2.estimateAffinePartial2D(src, dst, method=cv2.RANSAC, ransacReprojThreshold=4.0, maxIters=5000, confidence=0.999)
    if m is None or agree.sum() < AGREE:
        return None
    size = float(np.hypot(m[0, 0], m[1, 0]))
    turn = float(np.degrees(np.arctan2(m[1, 0], m[0, 0])))
    if not SIZES[0] <= size <= SIZES[1] or abs(turn) > MAX_TURN:
        return None
    (lx, ly), (rx, ry) = (m[:, :2] @ np.float32([[ref_eyes[0], ref_eyes[1]], [ref_eyes[2], ref_eyes[3]]]).T).T + m[:, 2]
    return float(lx), float(ly), float(rx), float(ry)


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
    calm_path = os.path.join(DRAWABLES, "peri_calm.webp")
    # Poses are edits of the calm Peri, and so is a mood whose eyes can't be found (shut, asleep).
    for name in [n for n in MOODS + POSES if n != "calm"]:
        if name in POSES or found[name] is None:
            carried = follow(calm_path, calm, os.path.join(DRAWABLES, f"peri_{name}.webp"))
            if carried is None:
                print(f"{name}: no match, " + ("its own eyes" if found[name] else "calm's eyes"))
            found[name] = carried or found[name] or calm
    heads = dict(found)
    faces = 0
    # Each mood's frames: the still's eyes, carried over to the frame.
    for mood in MOODS:
        still = os.path.join(DRAWABLES, f"peri_{mood}.webp")
        lost = []
        for f in FRAMES:
            path = os.path.join(DRAWABLES, f"peri_{mood}{f}.webp")
            if not os.path.exists(path):
                continue
            head = follow(still, found[mood], path)
            if head is None:
                lost.append(f[1:])
                head = found[mood]
            heads[f"{mood}{f}"] = head
            seen = eyes(path) if f in GLANCES else None
            if trusted(seen, head):
                faces += 1
            found[f"{mood}{f}"] = seen if trusted(seen, head) else head
        print(f"{mood}: " + (f"no match for {', '.join(lost)} (the still's eyes)" if lost else "every frame matched"))

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
    print(f"{faces} glances use their own eyes for her face")
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
