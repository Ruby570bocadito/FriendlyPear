# Keeps the heist video's faces steady and lets people blink while they talk. Muse Image redraws the whole picture
# when it changes a mouth or the eyes (sometimes a little bigger or shifted, sometimes with new details such as the
# stickers' codes), so for every pose:
#  1. each frame is aligned to the pose (rotation, scale and shift from features on the face, with RANSAC) and its
#     colour nudged to the pose's;
#  2. the mouth is where the mouth frames change and the eye frames don't, the eyes the other way round, so details
#     redrawn in every frame (codes, hands, knitting) are left out;
#  3. a mouth frame becomes the pose with only its lips and jaw taken from the frame (on the crook, his moustache and
#     everything above it stay the pose's, so they never wobble); an eye frame (blink, glance) becomes just its eyes
#     on a transparent picture, laid over whatever the mouth is doing.
# The model's own frames are kept as <name>_raw.png. Pass a .jpg path to also get a sheet of every pose with the
# mouth (green), the eyes (blue) and what stays still (red), to check them.
#   python video/scripts/heist_stabilize.py [regions.jpg]
import json
import sys
from pathlib import Path

import cv2
import numpy as np

pub = Path(__file__).resolve().parent.parent / "public"
heist = pub / "heist"
MOUTHS = {"talk", "slight", "half", "open", "wide", "round", "smile"}
# Where the mouth and eyes are, as fractions of the picture (x0, y0, x1, y1), for the poses the search gets wrong:
# the crook's moustache is redrawn in every frame and Rosa's bun more than her mouth. "not" is left out of the mouth.
# "moustache" is the line along the bottom of the crook's moustache, (x, y) points from left to right: everything
# above it (moustache, nose, cheeks) stays as the pose has it, so only his lips and jaw move.
MARKED = {
    "crook_plan": {
        "mouth": (0.31, 0.345, 0.58, 0.48), "eyes": (0.32, 0.25, 0.53, 0.34),
        "moustache": [(0.33, 0.383), (0.36, 0.384), (0.38, 0.386), (0.40, 0.388), (0.43, 0.391), (0.46, 0.391), (0.48, 0.387), (0.50, 0.386), (0.52, 0.384), (0.54, 0.372), (0.57, 0.36)],
    },
    "crook_desk": {
        "mouth": (0.33, 0.345, 0.63, 0.49), "eyes": (0.38, 0.27, 0.60, 0.355),
        "moustache": [(0.335, 0.392), (0.36, 0.396), (0.38, 0.397), (0.40, 0.403), (0.43, 0.407), (0.47, 0.407), (0.50, 0.406), (0.53, 0.406), (0.55, 0.402), (0.575, 0.398), (0.60, 0.393), (0.62, 0.385)],
    },
    "crook_phone": {
        "mouth": (0.32, 0.335, 0.62, 0.48), "eyes": (0.36, 0.255, 0.57, 0.35),
        "moustache": [(0.33, 0.399), (0.36, 0.402), (0.38, 0.398), (0.40, 0.401), (0.43, 0.405), (0.46, 0.405), (0.485, 0.403), (0.51, 0.399), (0.535, 0.396), (0.555, 0.388), (0.575, 0.381), (0.60, 0.377), (0.61, 0.37)],
    },
    "crook_stickers": {
        "mouth": (0.28, 0.34, 0.60, 0.485), "eyes": (0.32, 0.255, 0.53, 0.345), "not": (0.525, 0.405, 1, 1),
        "moustache": [(0.295, 0.38), (0.33, 0.384), (0.36, 0.389), (0.40, 0.392), (0.44, 0.392), (0.47, 0.391), (0.50, 0.389), (0.52, 0.38), (0.54, 0.372), (0.57, 0.365)],
    },
    "crook_shock": {
        "mouth": (0.33, 0.345, 0.62, 0.49), "eyes": (0.34, 0.23, 0.56, 0.345),
        "moustache": [(0.36, 0.404), (0.39, 0.40), (0.41, 0.39), (0.43, 0.384), (0.46, 0.384), (0.49, 0.383), (0.51, 0.379), (0.54, 0.378), (0.56, 0.382), (0.59, 0.385), (0.60, 0.38)],
    },
    "crook_angry": {
        "mouth": (0.33, 0.37, 0.64, 0.50), "eyes": (0.39, 0.28, 0.60, 0.365),
        "moustache": [(0.35, 0.424), (0.38, 0.424), (0.40, 0.416), (0.42, 0.409), (0.45, 0.408), (0.49, 0.408), (0.51, 0.41), (0.54, 0.412), (0.57, 0.411), (0.60, 0.408), (0.62, 0.405)],
    },
    "crook_quit": {
        "mouth": (0.38, 0.635, 0.58, 0.72), "eyes": (0.40, 0.53, 0.60, 0.625),
        "moustache": [(0.40, 0.676), (0.43, 0.675), (0.45, 0.676), (0.49, 0.677), (0.52, 0.675), (0.535, 0.671), (0.56, 0.668), (0.575, 0.665)],
    },
    "rosa_morning": {"mouth": (0.46, 0.36, 0.58, 0.43), "eyes": (0.44, 0.295, 0.58, 0.36)},
    # Rosa somewhere new in every round; where she doesn't speak, only her eyes move.
    "rosa_reading": {"eyes": (0.44, 0.285, 0.57, 0.365)},
    "rosa_yes": {"mouth": (0.46, 0.365, 0.58, 0.43), "eyes": (0.44, 0.285, 0.57, 0.36)},
    "rosa_kitchen": {"eyes": (0.40, 0.315, 0.58, 0.415)},
    "rosa_qr": {"eyes": (0.39, 0.315, 0.57, 0.415)},
    "rosa_garden_call": {"mouth": (0.44, 0.30, 0.58, 0.36), "eyes": (0.43, 0.228, 0.60, 0.295)},
    "rosa_fallen": {"mouth": (0.37, 0.485, 0.48, 0.545), "eyes": (0.35, 0.405, 0.48, 0.475)},
    # "Peri's diary" (diary_images.ts).
    "rosa_fallen_night": {"mouth": (0.37, 0.485, 0.48, 0.545), "eyes": (0.35, 0.405, 0.48, 0.475)},
    "rosa_kitchen_hangup": {"eyes": (0.38, 0.33, 0.58, 0.40)},
    "rosa_qr_away": {"eyes": (0.48, 0.355, 0.66, 0.42)},
    "rosa_garden_smile": {"eyes": (0.44, 0.255, 0.60, 0.31)},
    "peri_desk": {"mouth": (0.40, 0.56, 0.51, 0.645), "eyes": (0.30, 0.46, 0.54, 0.585)},
}


def raw_of(frame_file):
    raw = frame_file.with_name(frame_file.stem + "_raw.png")
    if not raw.exists():
        frame_file.replace(raw)
    return raw


def read(path, size=None):
    im = cv2.imread(str(path), cv2.IMREAD_COLOR)
    if size is not None and (im.shape[1], im.shape[0]) != size:
        im = cv2.resize(im, size, interpolation=cv2.INTER_LANCZOS4)
    return im


def disc(size):
    return cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (size, size))


def diff(a, b):
    return cv2.GaussianBlur(cv2.absdiff(a, b).mean(axis=2).astype(np.float32), (0, 0), 3)


def box_mask(shape, x0, y0, x1, y1):
    mask = np.zeros(shape, np.uint8)
    mask[int(max(0, y0)) : int(max(0, y1)), int(max(0, x0)) : int(max(0, x1))] = 255
    return mask


def align(pose, frame, mask):
    """The frame moved onto the pose (and its scale and shift), from features inside the mask; None if too few."""
    orb = cv2.ORB_create(5000)
    k1, d1 = orb.detectAndCompute(cv2.cvtColor(pose, cv2.COLOR_BGR2GRAY), mask)
    k2, d2 = orb.detectAndCompute(cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY), mask)
    if d1 is None or d2 is None:
        return None
    pairs = cv2.BFMatcher(cv2.NORM_HAMMING).knnMatch(d2, d1, k=2)
    good = [p[0] for p in pairs if len(p) == 2 and p[0].distance < 0.75 * p[1].distance]
    if len(good) < 15:
        return None
    src = np.float32([k2[m.queryIdx].pt for m in good])
    dst = np.float32([k1[m.trainIdx].pt for m in good])
    m, _ = cv2.estimateAffinePartial2D(src, dst, method=cv2.RANSAC, ransacReprojThreshold=2.5)
    if m is None:
        return None
    warped = cv2.warpAffine(frame, m, (pose.shape[1], pose.shape[0]), flags=cv2.INTER_LANCZOS4, borderMode=cv2.BORDER_REPLICATE)
    return warped, (float(np.hypot(m[0, 0], m[1, 0])), float(m[0, 2]), float(m[1, 2]))


def match_colour(frame, pose, area):
    """The frame with its colour cast removed: the typical difference to the pose where both show the same thing."""
    same = (area > 0) & (cv2.absdiff(frame, pose).max(axis=2) < 40)
    if same.sum() < 500:
        return frame
    cast = np.median(frame[same].astype(np.int16) - pose[same].astype(np.int16), axis=0)
    return np.clip(frame.astype(np.int16) - np.round(cast).astype(np.int16), 0, 255).astype(np.uint8)


def spots(score, threshold):
    """Places where the score is high: (total score, x, y, w, h, label), biggest total first, plus the label map."""
    on = cv2.morphologyEx((score > threshold).astype(np.uint8), cv2.MORPH_OPEN, disc(5))
    n, labels, stats, _ = cv2.connectedComponentsWithStats(on)
    totals = np.bincount(labels.ravel(), weights=score.ravel(), minlength=n)
    found = [(totals[i], *stats[i, :4], i) for i in range(1, n) if stats[i, 4] > 80]
    return sorted(found, reverse=True), labels


def above(points, shape):
    """Everything above a line given as (x, y) fractions from left to right, kept level beyond its ends."""
    h, w = shape
    line = np.interp(np.arange(w), [x * w for x, _ in points], [y * h for _, y in points])
    return cv2.dilate((np.arange(h)[:, None] <= line[None, :]).astype(np.uint8), disc(5)) > 0


def own_change(d, threshold, window, seeds, grow=15, soften=6):
    """A soft mask of what one frame changed inside the window, filled in, only where it touches the seeds."""
    on = cv2.morphologyEx(((d > threshold) & (window > 0)).astype(np.uint8), cv2.MORPH_CLOSE, disc(9))
    n, labels, stats, _ = cv2.connectedComponentsWithStats(on)
    keep = np.zeros(on.shape, np.uint8)
    for i in range(1, n):
        part = labels == i
        if stats[i, 4] >= 60 and (part & seeds).any():
            cv2.fillConvexPoly(keep, cv2.convexHull(cv2.findNonZero(part.astype(np.uint8))), 1)
    keep = cv2.dilate(keep, disc(grow)).astype(np.float32)
    soft_window = cv2.GaussianBlur(window.astype(np.float32) / 255, (0, 0), 10)
    return (cv2.GaussianBlur(keep, (0, 0), soften) * soft_window)[..., None]


def jaw_change(d, allowed):
    """A soft mask of the crook's lips and jaw as one frame moves them: what changed below the moustache, as one shape."""
    on = cv2.morphologyEx(((d > 20) & allowed).astype(np.uint8), cv2.MORPH_CLOSE, disc(7))
    n, labels, stats, _ = cv2.connectedComponentsWithStats(on)
    parts = [cv2.findNonZero((labels == i).astype(np.uint8)) for i in range(1, n) if stats[i, 4] >= 80]
    keep = np.zeros(on.shape, np.uint8)
    if parts:
        cv2.fillConvexPoly(keep, cv2.convexHull(np.vstack(parts)), 1)
    keep = cv2.dilate(keep, disc(9)) * allowed
    return (cv2.GaussianBlur(keep.astype(np.float32), (0, 0), 3) * allowed)[..., None]


# Every frame, by pose: (its file, whether it's a mouth).
by_pose = {}
prompts = json.loads((heist / "prompts.json").read_text())
for name, entry in prompts.items():
    if name.endswith("_talk") or name.endswith("_blink"):
        by_pose.setdefault(pub / entry["from"], []).append((heist / f"{name}.png", name.endswith("_talk")))
for meta in sorted((heist / "v").glob("*.json")):
    entry = json.loads(meta.read_text())
    if meta.with_suffix(".png").exists():
        by_pose.setdefault(pub / entry["from"], []).append((meta.with_suffix(".png"), meta.stem.rsplit("_", 1)[1] in MOUTHS))

sheet = []
for pose_file, frames in by_pose.items():
    pose = read(pose_file)
    size = (pose.shape[1], pose.shape[0])
    raws = [read(raw_of(f), size) for f, _ in frames]

    # A first alignment on everything the frames change, enough to find the mouth and eyes.
    rough = np.zeros(pose.shape[:2], np.float32)
    for raw in raws:
        rough = np.maximum(rough, cv2.GaussianBlur(cv2.absdiff(pose, raw).mean(axis=2).astype(np.float32), (0, 0), 4))
    ys, xs = np.nonzero(rough > 22)
    x0, x1, y0, y1 = np.percentile(xs, 1), np.percentile(xs, 99), np.percentile(ys, 1), np.percentile(ys, 99)
    area = box_mask(pose.shape[:2], x0 - (x1 - x0) / 4, y0 - (y1 - y0) / 4, x1 + (x1 - x0) / 4, y1 + (y1 - y0) / 4)
    first = [align(pose, raw, area) or (raw, (1.0, 0.0, 0.0)) for raw in raws]
    h, w = pose.shape[:2]
    marked = MARKED.get(pose_file.stem)
    if marked:
        at = lambda b: (b[0] * w, b[1] * h, b[2] * w, b[3] * h)
        mx0, my0, mx1, my1 = at(marked.get("mouth", marked["eyes"]))
        mouth_window = box_mask((h, w), mx0, my0, mx1, my1) if "mouth" in marked else np.zeros((h, w), np.uint8)
        if "not" in marked:
            mouth_window[box_mask((h, w), *at(marked["not"])) > 0] = 0
        eye_window = box_mask((h, w), *at(marked["eyes"]))
        mouth_seed, eye_seed = mouth_window > 0, eye_window > 0
        mx, my, mw, mh = mx0, my0, mx1 - mx0, my1 - my0
        reach = max(mw, mh) / 2
    else:
        diffs = [diff(pose, frame) for frame, _ in first]
        mouth_d = [d for d, (_, mouth) in zip(diffs, frames) if mouth]
        eye_d = [d for d, (_, mouth) in zip(diffs, frames) if not mouth]
        m_med, e_med = np.median(mouth_d, axis=0), np.median(eye_d, axis=0)
        e_two = np.sort(np.stack(eye_d), axis=0)[-min(2, len(eye_d))]
        # The mouth: what most mouth frames change and the eye frames don't.
        found, labels = spots(m_med - e_med, 14)
        _, mx, my, mw, mh, label = found[0]
        mouth_seed = cv2.dilate((labels == label).astype(np.uint8), disc(21)) > 0
        reach = max(mw, mh)
        mouth_window = box_mask((h, w), mx - reach, my - 0.8 * reach, mx + mw + reach, my + mh + 1.6 * reach)
        # The eyes: what blinks and glances change and mouth frames don't, above the mouth and near it.
        found, labels = spots(e_two - m_med, 12)
        near = [s for s in found if s[2] + s[4] / 2 < my and abs(s[1] + s[3] / 2 - (mx + mw / 2)) < 4 * max(mw, 60) and my - s[2] < 6 * max(mh, 50)][:2]
        eye_seed = np.isin(labels, [s[5] for s in near])
        if near:
            ex0, ey0 = min(s[1] for s in near), min(s[2] for s in near)
            ex1, ey1 = max(s[1] + s[3] for s in near), max(s[2] + s[4] for s in near)
            pad = max(ex1 - ex0, ey1 - ey0) * 0.5
            eye_window = box_mask((h, w), ex0 - pad, ey0 - pad, ex1 + pad, min(ey1 + pad, my))
            mouth_window[: int(ey1 + (my - ey1) / 3)] = 0  # the mouth never reaches the eyes
        else:
            eye_window = np.zeros((h, w), np.uint8)
        eye_seed = cv2.dilate(eye_seed.astype(np.uint8), disc(21)) > 0

    # A second alignment on the face alone (around the eyes and mouth), so the head lines up even if the rest moved.
    face = cv2.dilate(np.maximum(mouth_window, eye_window), disc(int(reach) | 1))
    aligned = []
    for (frame_file, mouth), raw, (rough_frame, rough_fit) in zip(frames, raws, first):
        frame, fit = align(pose, raw, face) or (rough_frame, rough_fit)
        aligned.append((match_colour(frame, pose, mouth_window if mouth else eye_window), fit))
    still = above(marked["moustache"], (h, w)) if marked and "moustache" in marked else None
    if still is not None:
        allowed = (mouth_window > 0) & ~still

    for (frame_file, mouth), (frame, (scale, dx, dy)) in zip(frames, aligned):
        d = diff(pose, frame)
        if mouth:
            m = jaw_change(d, allowed) if still is not None else own_change(d, 16, mouth_window, mouth_seed, grow=11, soften=4)
            cv2.imwrite(str(frame_file), (pose * (1 - m) + frame * m).clip(0, 255).astype(np.uint8))
        else:
            m = own_change(d, 12, eye_window, eye_seed)
            cv2.imwrite(str(frame_file), np.dstack([frame, (m[..., 0] * 255).clip(0, 255).astype(np.uint8)]))
        print(f"{frame_file.stem:26} scale {scale:5.3f}  shift {dx:6.1f},{dy:6.1f}  kept {(m > 0.5).sum():7d} px")

    if len(sys.argv) > 1:
        view = pose.copy()
        if still is not None:
            view[still & (mouth_window > 0)] = (0.5 * view[still & (mouth_window > 0)] + [0, 0, 128]).astype(np.uint8)
        for seed, colour in ((mouth_seed, (0, 220, 0)), (eye_seed, (255, 120, 0))):
            contours, _ = cv2.findContours(seed.astype(np.uint8), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            cv2.drawContours(view, contours, -1, colour, 3)
        for window, colour in ((mouth_window, (0, 220, 0)), (eye_window, (255, 120, 0))):
            contours, _ = cv2.findContours(window, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            cv2.drawContours(view, contours, -1, colour, 2)
        cx, cy, half = mx + mw / 2, my, 2.6 * reach + 120
        crop = view[int(max(0, cy - half)) : int(cy + half), int(max(0, cx - half)) : int(cx + half)]
        tile = cv2.resize(crop, (360, 360))
        cv2.putText(tile, pose_file.stem, (8, 24), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        sheet.append(tile)

if sheet:
    while len(sheet) % 4:
        sheet.append(np.zeros_like(sheet[0]))
    cv2.imwrite(sys.argv[1], np.vstack([np.hstack(sheet[i : i + 4]) for i in range(0, len(sheet), 4)]), [cv2.IMWRITE_JPEG_QUALITY, 85])
