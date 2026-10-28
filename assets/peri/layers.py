"""
Peri by layers (docs/PERI_SPRITES.md): her body without a face, and her eyes and her mouth as pieces cut from
pictures in which only they change, so any body can wear any face and she can talk, blink and look around in any pose.

    python assets/peri/layers.py            needs Pillow, numpy and OpenCV

Reads  assets/peri/peri_calm*.webp      the face she already had (open, blinking, glancing, winking, three mouths)
       assets/peri/sprites/             Muse Image pictures by the image agent: the body without a face and new faces
Writes android/.../res/drawable-nodpi/peri_layer_body_<pose>.webp, peri_layer_eyes_<state>.webp, peri_layer_mouth_<state>.webp
       (and nothing else named peri_layer_*: they are all this script's)
       android/.../ui/peri/PeriLayerData.kt   generated: where the pieces go on the body, and which exist

Every face picture is an edit of peri_calm with the same framing (measured: within 2 px), so the pieces line up with
no alignment. Where the eyes and the mouth live is what the pictures change, well inside her outline (the edge differs
a little in every edit and belongs to the body): each changed pixel goes to the kind of picture that changes it more
often, because Muse redraws an eye now and then in a mouth picture and a cheek in an eye picture. Wherever the faceless
body differs, the nearer piece covers it whole. Each piece keeps its shape, a little wider, with a soft edge, so its
clay skin melts into the body's; between the two, the mouth gives way to the eyes across a short band.
"""
import glob
import os
import sys

import cv2
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SPRITES = os.path.join(HERE, "sprites")
DRAWABLES = os.path.join(HERE, "..", "..", "android", "app", "src", "main", "res", "drawable-nodpi")
KOTLIN = os.path.join(HERE, "..", "..", "android", "app", "src", "main", "java", "app", "friendlypear", "ui", "peri", "PeriLayerData.kt")
sys.path.insert(0, HERE)
from cutout import SIZE, cut  # noqa: E402

SOURCE = 1600  # Muse Image's pictures
SCALE = SIZE / SOURCE
STEP = 25  # 25 px of the 1600 px picture are exactly 16 of the 1024 px one
# The face, generously: rows and columns of the 1600 px picture.
FACE = (slice(470, 1140), slice(500, 1120))
GROW = 16  # px of skin around each feature
FEATHER = 7.0  # px, the soft edge
EDGE = 40  # px of her outline that always belong to the body
BAND = 6  # px, half the band where the mouth gives way to the eyes

# Her old glances are no pieces: to the right she turns her whole head, and to the left only the shine moves. She looks
# sideways by moving her eyes (LayeredPeri).
EYES = {
    "open": "peri_calm.webp",
    "half": "peri_calm_blinkhalf.webp",
    "closed": "peri_calm_blink.webp",
    "wink_right": "peri_calm_wink.webp",
    "happy": "sprites/face_eyes_happy.webp",
    "wide": "sprites/face_eyes_wide.webp",
    "laugh": "sprites/face_eyes_laugh.webp",
    "worried": "sprites/face_eyes_worried.webp",
    "love": "sprites/face_eyes_love.webp",
    "stars": "sprites/face_eyes_stars.webp",
    "up": "sprites/face_eyes_up.webp",
    "down": "sprites/face_eyes_down.webp",
    "wink_left": "sprites/face_eyes_wink_left.webp",
    "sleepy": "sprites/face_eyes_sleepy.webp",
    "brave": "sprites/face_eyes_brave.webp",
    "teary": "sprites/face_eyes_teary.webp",
}
MOUTHS = {
    "smile": "peri_calm.webp",
    "talk": "peri_calm_talk.webp",
    "wide": "peri_calm_talkwide.webp",
    "o": "peri_calm_talko.webp",
    "m": "sprites/face_mouth_m.webp",
    "ee": "sprites/face_mouth_ee.webp",
    "u": "sprites/face_mouth_u.webp",
    "laugh": "sprites/face_mouth_laugh.webp",
    "grin": "sprites/face_mouth_grin.webp",
    "surprised": "sprites/face_mouth_surprised.webp",
    "f": "sprites/face_mouth_f.webp",
    "l": "sprites/face_mouth_l.webp",
    "worried": "sprites/face_mouth_worried.webp",
    "sad": "sprites/face_mouth_sad.webp",
    "tongue": "sprites/face_mouth_tongue.webp",
    "kiss": "sprites/face_mouth_kiss.webp",
    "whistle": "sprites/face_mouth_whistle.webp",
    "smirk": "sprites/face_mouth_smirk.webp",
    "yawn": "sprites/face_mouth_yawn.webp",
}
# Each body without a face, and the picture it was made from (same framing, measured: within 3 px): that picture's eyes
# (PeriAnchors) say where her face goes on it. The calm body is the one the pieces are cut for.
BODIES = {
    "calm": ("sprites/body_calm_noface.webp", "peri_calm"),
    "wave1": ("sprites/body_calm_wave1_noface.webp", "peri_calm_wave1"),
    "wave2": ("sprites/body_calm_wave2_noface.webp", "peri_calm_wave2"),
    "gesturea": ("sprites/body_calm_gesturea_noface.webp", "peri_calm_gesturea"),
    "gestureb": ("sprites/body_calm_gestureb_noface.webp", "peri_calm_gestureb"),
    "thumbsup": ("sprites/body_calm_thumbsup_noface.webp", "peri_calm_thumbsup"),
    "love": ("sprites/body_calm_love_noface.webp", "peri_calm_love"),
}


def load(name: str) -> np.ndarray:
    """The picture a little blurred: clay grain and compression are not changes."""
    rgb = np.asarray(Image.open(os.path.join(HERE, name)).convert("RGB"), dtype=np.float32)
    return cv2.GaussianBlur(rgb, (0, 0), 1.5)


def clean(mask: np.ndarray, least: int) -> np.ndarray:
    """[mask] without specks: opened, and only its parts of [least] pixels or more."""
    mask = cv2.morphologyEx(mask.astype(np.uint8), cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5)))
    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask)
    big = np.zeros(count, dtype=np.uint8)
    big[1:] = stats[1:, cv2.CC_STAT_AREA] >= least
    return big[labels]


def face_inside() -> np.ndarray:
    """The face, well inside her outline."""
    outline = np.asarray(cut(os.path.join(HERE, "peri_calm.webp")))[:, :, 3]
    outline = (cv2.resize(outline, (SOURCE, SOURCE), interpolation=cv2.INTER_LINEAR) > 128).astype(np.uint8)
    inner = cv2.erode(outline, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * EDGE + 1, 2 * EDGE + 1)))
    face = np.zeros_like(inner)
    face[FACE] = 1
    return (inner & face).astype(bool)


def changes(base: np.ndarray, name: str, inside: np.ndarray) -> np.ndarray:
    """Where the picture [name] differs from [base], inside the face, without specks."""
    return clean((np.abs(load(name) - base).sum(axis=2) > 90) & inside, 200)


def soft(mask: np.ndarray) -> np.ndarray:
    grown = cv2.dilate(mask, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * GROW + 1, 2 * GROW + 1)))
    return np.clip(cv2.GaussianBlur(grown.astype(np.float32), (0, 0), FEATHER) * 1.15, 0.0, 1.0)


def box(alpha: np.ndarray):
    ys, xs = np.nonzero(alpha > 0.01)
    # On whole steps: a piece lands on whole pixels of the body and keeps exactly its scale.
    up = lambda v: min(SOURCE, -(-v // STEP) * STEP)
    return int(xs.min() // STEP * STEP), int(ys.min() // STEP * STEP), int(up(xs.max() + 1)), int(up(ys.max() + 1))


def piece(name: str, alpha: np.ndarray, rect, out: str):
    left, top, right, bottom = rect
    image = np.asarray(Image.open(os.path.join(HERE, name)).convert("RGB"))[top:bottom, left:right]
    a = (alpha[top:bottom, left:right] * 255).astype(np.uint8)
    rgba = Image.fromarray(np.dstack([image, a]), "RGBA")
    size = (round((right - left) * SCALE), round((bottom - top) * SCALE))
    rgba.resize(size, Image.LANCZOS).save(os.path.join(DRAWABLES, out), "WEBP", quality=92, method=6)


def main() -> None:
    exists = lambda name: os.path.exists(os.path.join(HERE, name))
    have = lambda table: {k: v for k, v in table.items() if exists(v)}
    eyes, mouths = have(EYES), have(MOUTHS)
    bodies = {k: v for k, v in BODIES.items() if exists(v[0])}
    base, inside = load("peri_calm.webp"), face_inside()
    count = lambda names: sum(changes(base, name, inside).astype(np.int32) for name in names)
    eye_count, mouth_count = count(eyes.values()), count(mouths.values())
    eye_share, mouth_share = eye_count / len(eyes), mouth_count / len(mouths)
    eye_area = clean((eye_count > 0) & (eye_share > mouth_share), 300)
    mouth_area = clean((mouth_count > 0) & (mouth_share >= eye_share), 300)
    gone = changes(base, BODIES["calm"][0], inside)
    distance = lambda area: cv2.distanceTransform(1 - area, cv2.DIST_L2, 5)
    to_eyes, to_mouth = distance(eye_area), distance(mouth_area)
    eye_area |= gone & (to_eyes <= to_mouth)
    mouth_area |= gone & (to_mouth < to_eyes)
    to_eyes, to_mouth = distance(eye_area), distance(mouth_area)
    eye_alpha = soft(eye_area)
    mouth_alpha = soft(mouth_area) * np.clip((to_eyes - to_mouth) / (2 * BAND) + 0.5, 0.0, 1.0)
    eye_rect, mouth_rect = box(eye_alpha), box(mouth_alpha)
    for old in glob.glob(os.path.join(DRAWABLES, "peri_layer_*.webp")):
        os.remove(old)
    for state, name in eyes.items():
        piece(name, eye_alpha, eye_rect, f"peri_layer_eyes_{state}.webp")
    for state, name in mouths.items():
        piece(name, mouth_alpha, mouth_rect, f"peri_layer_mouth_{state}.webp")
    for state, (name, _) in bodies.items():
        cut(os.path.join(HERE, name)).save(os.path.join(DRAWABLES, f"peri_layer_body_{state}.webp"), "WEBP", quality=92, method=6)

    def entries(table, kind):
        return "".join(f'        "{s}" to R.drawable.peri_layer_{kind}_{s},\n' for s in table)

    box_1024 = lambda r: "floatArrayOf(" + ", ".join(f"{v * SCALE:.0f}f" for v in r) + ")"
    body_entries = "".join(f'        "{s}" to (R.drawable.peri_layer_body_{s} to R.drawable.{frame}),\n' for s, (_, frame) in bodies.items())
    with open(KOTLIN, "w", encoding="utf-8", newline="\n") as f:
        f.write(
            "// Generated by assets/peri/layers.py: do not edit.\n"
            "package app.friendlypear.ui.peri\n\n"
            "import app.friendlypear.R\n\n"
            "/** Peri by layers: the pieces there are, and where her eyes and her mouth go (left, top, right, bottom on the calm body's 1024 px picture). */\n"
            "internal object PeriLayerData {\n"
            f"    const val SIZE = {SIZE}f\n"
            f"    val EYES_BOX = {box_1024(eye_rect)}\n"
            f"    val MOUTH_BOX = {box_1024(mouth_rect)}\n"
            "    /** Each body, and the picture it was made from: that picture's eyes ([PeriAnchors]) place her face on it. */\n"
            "    val BODIES = mapOf(\n" + body_entries + "    )\n"
            "    val EYES = mapOf(\n" + entries(eyes, "eyes") + "    )\n"
            "    val MOUTHS = mapOf(\n" + entries(mouths, "mouth") + "    )\n"
            "}\n"
        )
    print(f"eyes {len(eyes)} at {eye_rect}, mouths {len(mouths)} at {mouth_rect}, bodies {len(bodies)}")


if __name__ == "__main__":
    main()
