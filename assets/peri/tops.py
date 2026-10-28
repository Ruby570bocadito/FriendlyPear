"""
Peri's head without its leaf and stem, for when she wears a hat: over a leaf, a hat looks stuck on. Muse Image redraws
each pose bare-headed (`generate.ts tops`, kept in assets/peri/peri_bare_<name>.webp); this script cuts it out, lines it
up with the pose by her face, and keeps only the top of her head where something changed: from just left of the leaf to
just right of the stem, down to just under them, its sides and bottom fading out. The app draws that piece over the
pose, and over each of the pose's frames moved as far as her head moved, in place of that part of the picture
(PeriTops.kt, generated: do not edit). Also writes the bare calm Peri for the family's web page (backend/public/bare.webp).

    python assets/peri/tops.py [--sheet sheet.png]
"""
import os
import re
import sys

import cv2
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from anchors import DRAWABLES, FRAMES, KOTLIN, grey  # noqa: E402
from cutout import cut  # noqa: E402

TOPS_KT = os.path.join(os.path.dirname(KOTLIN), "PeriTops.kt")
WEB = os.path.join(HERE, "..", "..", "backend", "public", "bare.webp")
# Moods and poses that can wear a hat: asleep she has her nightcap, and a detective's or a party hat is a hat already.
MOODS = ["calm", "listening", "thinking", "worried", "protective", "calling", "waiting", "happy"]
POSES = ["calm_wave1", "calm_wave2", "calm_idea", "calm_sorry", "calm_thumbsup", "calm_laugh", "calm_love", "calm_dizzy",
         "calm_yawn", "calm_letter", "calm_hero", "calm_medal", "calm_peekaboo", "calm_clap", "calm_surprised",
         "calm_glasses", "calm_scarf", "calm_bye"]
MARGIN = 24  # px kept around what changed
FADE = 16  # px of soft edge at the sides and the bottom
CHANGE = 90  # a colour difference (sum of the three channels) that counts as changed
REACH = 60  # px the redrawn picture may have moved


def anchors() -> dict[str, list[float]]:
    """Her eyes in each picture, as the app has them (PeriAnchors.kt)."""
    text = open(KOTLIN, encoding="utf-8").read()
    found = re.finditer(r"R\.drawable\.peri_(\w+) to floatArrayOf\(([-\d.]+)f, ([-\d.]+)f, ([-\d.]+)f, ([-\d.]+)f\)", text)
    return {m.group(1): [float(v) for v in m.group(2, 3, 4, 5)] for m in found}


def rgba(image: Image.Image) -> np.ndarray:
    return np.asarray(image.convert("RGBA")).astype(np.float32)


def aligned(bare: np.ndarray, pose: np.ndarray, eyes: list[float]) -> np.ndarray:
    """The redrawn picture moved to lie on the pose, matched on her face (the leaf, which changed, left out)."""
    lx, ly, rx, ry = eyes
    d = ((rx - lx) ** 2 + (ry - ly) ** 2) ** 0.5
    cx, cy = (lx + rx) / 2, (ly + ry) / 2
    top, bottom = int(cy - 0.35 * d), int(cy + 1.0 * d)
    left, right = max(0, int(cx - 1.3 * d)), min(1024, int(cx + 1.3 * d))

    def on_grey(a: np.ndarray) -> np.ndarray:
        k = a[..., 3:4] / 255
        return cv2.cvtColor((a[..., :3] * k + 128 * (1 - k)).astype(np.uint8), cv2.COLOR_RGB2GRAY)

    face = on_grey(pose)[top:bottom, left:right]
    y0, x0 = max(0, top - REACH), max(0, left - REACH)
    area = on_grey(bare)[y0: bottom + REACH, x0: right + REACH]
    _, score, _, (bx, by) = cv2.minMaxLoc(cv2.matchTemplate(area, face, cv2.TM_CCOEFF_NORMED))
    dx, dy = bx + x0 - left, by + y0 - top
    print(f"  moved {dx:+d},{dy:+d} (match {score:.2f})")
    shift = np.float32([[1, 0, -dx], [0, 1, -dy]])
    return cv2.warpAffine(bare, shift, (1024, 1024), flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))


def piece(name: str, eyes: list[float]):
    """The bare top for one pose: the picture to draw, where (left, top, width, height) and what it replaces."""
    pose = rgba(Image.open(os.path.join(DRAWABLES, f"peri_{name}.webp")))
    bare = aligned(rgba(cut(os.path.join(HERE, f"peri_bare_{name}.webp"))), pose, eyes)
    lx, ly, rx, ry = eyes
    d = ((rx - lx) ** 2 + (ry - ly) ** 2) ** 0.5
    eye_y = min(ly, ry)
    a0, a1 = pose[..., 3] / 255, bare[..., 3] / 255
    colour = np.abs(pose[..., :3] - bare[..., :3]).sum(axis=2)
    changed = ((a0 > 0.5) & (a1 < 0.25)) | ((a0 < 0.25) & (a1 > 0.5)) | ((a0 > 0.5) & (a1 > 0.5) & (colour > CHANGE))
    changed[int(eye_y - 0.45 * d):] = False
    changed = cv2.morphologyEx(changed.astype(np.uint8), cv2.MORPH_OPEN, np.ones((5, 5), np.uint8))
    count, labels, stats, _ = cv2.connectedComponentsWithStats(changed, connectivity=8)
    keep = [i for i in range(1, count) if stats[i, cv2.CC_STAT_AREA] >= 150]
    if not keep:
        raise SystemExit(f"{name}: nothing changed on top; is peri_bare_{name}.webp bare-headed?")
    x0 = min(stats[i, cv2.CC_STAT_LEFT] for i in keep)
    x1 = max(stats[i, cv2.CC_STAT_LEFT] + stats[i, cv2.CC_STAT_WIDTH] for i in keep)
    y1 = max(stats[i, cv2.CC_STAT_TOP] + stats[i, cv2.CC_STAT_HEIGHT] for i in keep)
    line = int(min(y1 + 8, eye_y - 0.45 * d))
    left, right = max(0, x0 - MARGIN), min(1024, x1 + MARGIN)
    bottom = min(1024, line + FADE)
    out = bare[:bottom, left:right].copy()
    # Soft sides and bottom: there the pose shows through, so no seam where the redraw differs a little.
    ramp = np.linspace(0, 1, FADE, endpoint=False)
    fade = np.ones(out.shape[:2], np.float32)
    fade[:, :FADE] *= ramp[None, :]
    fade[:, -FADE:] *= ramp[::-1][None, :]
    fade[-FADE:, :] *= ramp[::-1][:, None]
    out[..., 3] *= fade
    picture = Image.fromarray(out.clip(0, 255).astype(np.uint8), "RGBA")
    clip = (left + FADE, 0, right - FADE, line)
    return picture, (left, 0, right - left, bottom), clip


def main() -> None:
    eyes = anchors()
    tops = {}
    for name in MOODS + POSES:
        print(name)
        picture, box, clip = piece(name, eyes[name])
        picture.save(os.path.join(DRAWABLES, f"peri_top_{name}.webp"), "WEBP", quality=90, alpha_quality=100, method=6)
        tops[name] = (name, box, clip)
        if name == "calm":
            pose = Image.open(os.path.join(DRAWABLES, "peri_calm.webp")).convert("RGBA")
            cleared = np.asarray(pose).copy()
            cleared[clip[1]: clip[3], clip[0]: clip[2]] = 0
            web = Image.fromarray(cleared, "RGBA")
            web.alpha_composite(picture, (box[0], box[1]))
            web.save(WEB, "WEBP", quality=88, alpha_quality=100, method=6)
    # A mood's frames: their own bare head where Muse redrew it (a step or a gesture turns her head a little, and the
    # still's piece would leave a bit of leaf), else their still's piece, moved as far as her head moved in that frame.
    for mood in MOODS:
        for f in FRAMES:
            frame = f"{mood}{f}"
            if frame not in eyes:
                continue
            if os.path.exists(os.path.join(HERE, f"peri_bare_{frame}.webp")):
                print(frame)
                picture, box, clip = piece(frame, eyes[frame])
                picture.save(os.path.join(DRAWABLES, f"peri_top_{frame}.webp"), "WEBP", quality=90, alpha_quality=100, method=6)
                tops[frame] = (frame, box, clip)
                continue
            dx, dy = eyes[frame][0] - eyes[mood][0], eyes[frame][1] - eyes[mood][1]
            _, (x, y, w, h), (c0, c1, c2, c3) = tops[mood]
            tops[frame] = (mood, (x + dx, y + dy, w, h), (c0 + dx, c1 + dy, c2 + dx, c3 + dy))

    lines = [
        "package app.friendlypear.ui.peri",
        "",
        "import app.friendlypear.R",
        "",
        "/**",
        " * Generated by assets/peri/tops.py: Peri's bare head (no leaf, no stem) for under a hat, for each picture that can",
        " * wear one: the piece to draw ([Top.box]: left, top, width, height, in the picture's 1024 px square) and the part",
        " * of the picture it takes the place of ([Top.clip]: left, top, right, bottom).",
        " */",
        "internal object PeriTops {",
        "    class Top(val picture: Int, val box: FloatArray, val clip: FloatArray)",
        "",
        "    val TOPS: Map<Int, Top> = mapOf(",
    ]
    for name, (owner, box, clip) in tops.items():
        b = ", ".join(f"{v:.0f}f" for v in box)
        c = ", ".join(f"{v:.0f}f" for v in clip)
        lines.append(f"        R.drawable.peri_{name} to Top(R.drawable.peri_top_{owner}, floatArrayOf({b}), floatArrayOf({c})),")
    lines += ["    )", "}", ""]
    open(TOPS_KT, "w", encoding="utf-8", newline="\n").write("\n".join(lines))
    print(f"wrote {os.path.relpath(TOPS_KT, HERE)} ({len(tops)} pictures)")

    if "--sheet" in sys.argv:
        names = list(tops)
        cell, cols = 256, 10
        sheet = Image.new("RGB", (cols * cell, ((len(names) + cols - 1) // cols) * cell), (225, 238, 250))
        for i, name in enumerate(names):
            owner, box, clip = tops[name]
            pose = np.asarray(Image.open(os.path.join(DRAWABLES, f"peri_{name}.webp")).convert("RGBA")).copy()
            c = [int(round(v)) for v in clip]
            pose[max(0, c[1]): max(0, c[3]), max(0, c[0]): max(0, c[2])] = 0
            im = Image.fromarray(pose, "RGBA")
            im.alpha_composite(Image.open(os.path.join(DRAWABLES, f"peri_top_{owner}.webp")).convert("RGBA"), (int(round(box[0])), int(round(box[1]))))
            bg = Image.new("RGBA", im.size, (225, 238, 250, 255))
            bg.alpha_composite(im)
            ImageDraw.Draw(bg).text((8, 8), name, fill=(0, 0, 0))
            sheet.paste(bg.convert("RGB").resize((cell, cell)), ((i % cols) * cell, (i // cols) * cell))
        sheet.save(sys.argv[sys.argv.index("--sheet") + 1])


if __name__ == "__main__":
    main()
