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

import math
import re

import cv2
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SPRITES = os.path.join(HERE, "sprites")
DRAWABLES = os.path.join(HERE, "..", "..", "android", "app", "src", "main", "res", "drawable-nodpi")
KOTLIN = os.path.join(HERE, "..", "..", "android", "app", "src", "main", "java", "app", "friendlypear", "ui", "peri", "PeriLayerData.kt")
ANCHORS = os.path.join(os.path.dirname(KOTLIN), "PeriAnchors.kt")
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
# In front of her face: differing this much from the calm body (summed over RGB), in pieces of this many px (1024 px).
FRONT_DIFF = 90
FRONT_LEAST = 250

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
# Her mouths for talking: small, so their skin around them never covers a hand she holds near her chest; the big ones
# (a laugh, a yawn, a surprised "oh!") cover what they need.
TALKING = {"smile", "talk", "wide", "o", "m", "ee", "u", "f", "l"}
# The calm body, the one the pieces are cut for.
CALM_BODY = "sprites/body_calm_noface.webp"
# Moods drawn by layers: those with a faceless still (Muse's filter turned down "no face" for the worried and happy ones,
# which keep their whole pictures), but thinking, whose eye is seen through her magnifying glass, and sleeping, whose
# shut eyes are softer than any piece (and asleep she neither talks nor looks around). Every faceless body of these moods is used: sprites/body_<frame>_noface.webp is
# peri_<frame> without its face, in the same framing (measured: within 3 px), so that picture's eyes (PeriAnchors) say
# where her face goes on it.
MOODS = ["calm", "listening", "protective", "calling", "waiting"]
# Each mood's face: eyes and mouth, as near as the pieces get to its picture.
MOOD_FACE = {
    "calm": ("open", "smile"), "listening": ("open", "smile"), "protective": ("brave", "smile"),
    "calling": ("open", "smile"), "waiting": ("open", "smile"),
}
# A mood's frames: what changes on its face (eyes, mouth; None keeps the mood's) and where the eyes look. Steps and
# gestures have their own body; the rest are the still body with another face.
FRAME_FACE = {
    "": (None, None, 0), "_blinkhalf": ("half", None, 0), "_blink": ("closed", None, 0), "_talk": (None, "talk", 0),
    "_talkwide": (None, "wide", 0), "_talko": (None, "o", 0), "_wink": ("wink_right", None, 0),
    "_glanceleft": (None, None, -1), "_glanceright": (None, None, 1), "_gesturea": (None, "talk", 0), "_gestureb": (None, "wide", 0),
    "_stepleft": (None, None, 0), "_stepright": (None, None, 0), "_steplefthalf": (None, None, 0), "_steprighthalf": (None, None, 0),
}
# The calm Peri's poses, each with the face of her picture.
POSE_FACE = {
    "wave1": ("happy", "smile"), "wave2": ("happy", "smile"), "butterfly": ("up", "smile"), "bye": ("happy", "talk"),
    "clap": ("happy", "laugh"), "flower": ("happy", "smile"), "hero": ("brave", "grin"), "idea": ("wide", "o"),
    "letter": ("down", "smile"), "love": ("love", "kiss"), "medal": ("happy", "grin"), "party": ("stars", "laugh"),
    "phone": ("down", "smile"), "scarf": ("happy", "smile"), "sorry": ("worried", "worried"), "tea": ("happy", "smile"),
    "thumbsup": ("happy", "grin"),
}


def placements():
    """Where her face goes on each picture, from PeriAnchors: the turn, size and shift taking the calm eyes to its eyes."""
    text = open(ANCHORS, encoding="utf-8").read().split("val HEADS")[0]
    eyes = {n: [float(v) for v in vs] for n, *vs in re.findall(r"R\.drawable\.(\w+) to floatArrayOf\(([\d.]+)f, ([\d.]+)f, ([\d.]+)f, ([\d.]+)f\)", text)}
    calm = eyes["peri_calm"]

    def matrix(name):
        e = eyes[name]
        turn = math.atan2(e[3] - e[1], e[2] - e[0]) - math.atan2(calm[3] - calm[1], calm[2] - calm[0])
        size = math.hypot(e[2] - e[0], e[3] - e[1]) / math.hypot(calm[2] - calm[0], calm[3] - calm[1])
        a, b = size * math.cos(turn), size * math.sin(turn)
        c0, c1 = ((calm[0] + calm[2]) / 2, (calm[1] + calm[3]) / 2), ((e[0] + e[2]) / 2, (e[1] + e[3]) / 2)
        return np.float32([[a, -b, c1[0] - a * c0[0] + b * c0[1]], [b, a, c1[1] - b * c0[0] - a * c0[1]]])

    return matrix


def front(body: np.ndarray, calm: np.ndarray, face: np.ndarray, m: np.ndarray, objects_only: bool):
    """What's in front of her face on [body] (both 1024 px RGBA): where it differs a lot from the calm body moved onto
    it, inside the area her eyes and mouth cover ([face], calm's). Another mood's body is lit a little differently, so
    there only what isn't her green clay or her cheeks counts ([objects_only]: a watch, a letter). The body's own pixels there,
    soft-edged, and their box; None if nothing is in front."""
    size = (SIZE, SIZE)
    calm_here = cv2.warpAffine(calm, m, size, flags=cv2.INTER_LINEAR)
    face_here = cv2.warpAffine(face, m, size, flags=cv2.INTER_LINEAR)
    blur = lambda img: cv2.GaussianBlur(img[..., :3].astype(np.float32), (0, 0), 2.0)
    differs = (np.abs(blur(body) - blur(calm_here)).sum(axis=2) > FRONT_DIFF) & (body[..., 3] > 128) & (face_here > 0.05)
    if objects_only:
        hsv = cv2.cvtColor(body[..., :3], cv2.COLOR_RGB2HSV)
        clay = (hsv[..., 0] >= 30) & (hsv[..., 0] <= 80) & (hsv[..., 1] > 40)
        blush = ((hsv[..., 0] <= 12) | (hsv[..., 0] >= 165)) & (hsv[..., 1] < 150)
        differs &= ~clay & ~blush
    mask = clean(differs, FRONT_LEAST)
    if mask.sum() < FRONT_LEAST:
        return None
    mask = cv2.GaussianBlur(cv2.dilate(mask, np.ones((5, 5), np.uint8)).astype(np.float32), (0, 0), 1.5)
    ys, xs = np.nonzero(mask > 0.02)
    left, top, right, bottom = int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1
    rgba = body.copy()
    rgba[..., 3] = (body[..., 3].astype(np.float32) * np.clip(mask, 0, 1)).astype(np.uint8)
    return Image.fromarray(rgba[top:bottom, left:right], "RGBA"), (left, top, right, bottom)


def bodies_and_frames():
    """The faceless bodies of the layered moods, and every frame of the app drawn by layers: frame name to (body, eyes,
    mouth, look)."""
    made = {f[len("body_"):-len("_noface.webp")] for f in os.listdir(SPRITES) if f.startswith("body_") and f.endswith("_noface.webp")}
    bodies = {b: f"sprites/body_{b}_noface.webp" for b in sorted(made) if b.split("_")[0] in MOODS and os.path.exists(os.path.join(DRAWABLES, f"peri_{b}.webp"))}
    frames = {}
    for mood in MOODS:
        if mood not in bodies:
            continue
        eyes, mouth = MOOD_FACE[mood]
        for suffix, (e, m, look) in FRAME_FACE.items():
            body = mood + suffix if mood + suffix in bodies else mood
            if (body == mood and suffix.startswith(("_step", "_gesture"))) or not os.path.exists(os.path.join(DRAWABLES, f"peri_{mood}{suffix}.webp")):
                continue
            frames[f"{mood}{suffix}"] = (body, e or eyes, m or mouth, look)
    for pose, (e, m) in POSE_FACE.items():
        if f"calm_{pose}" in bodies:
            frames[f"calm_{pose}"] = (f"calm_{pose}", e, m, 0)
    return bodies, frames


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
    bodies, frames = bodies_and_frames()
    base, inside = load("peri_calm.webp"), face_inside()
    count = lambda names: sum(changes(base, name, inside).astype(np.int32) for name in names)
    eye_count, mouth_count = count(eyes.values()), count(mouths.values())
    eye_share, mouth_share = eye_count / len(eyes), mouth_count / len(mouths)
    eye_area = clean((eye_count > 0) & (eye_share > mouth_share), 300)
    mouth_area = clean((mouth_count > 0) & (mouth_share >= eye_share), 300)
    talk_area = clean((count(v for k, v in mouths.items() if k in TALKING) > 0) & (mouth_share >= eye_share), 300)
    gone = changes(base, CALM_BODY, inside)
    distance = lambda area: cv2.distanceTransform(1 - area, cv2.DIST_L2, 5)
    to_eyes, to_mouth = distance(eye_area), distance(mouth_area)
    eye_area |= gone & (to_eyes <= to_mouth)
    mouth_area |= gone & (to_mouth < to_eyes)
    talk_area |= gone & (to_mouth < to_eyes)
    to_eyes, to_mouth = distance(eye_area), distance(mouth_area)
    eye_alpha = soft(eye_area)
    give_way = np.clip((to_eyes - to_mouth) / (2 * BAND) + 0.5, 0.0, 1.0)
    mouth_alpha = soft(mouth_area) * give_way
    talk_alpha = soft(talk_area) * give_way
    eye_rect, mouth_rect = box(eye_alpha), box(mouth_alpha)
    for old in glob.glob(os.path.join(DRAWABLES, "peri_layer_*.webp")):
        os.remove(old)
    for state, name in eyes.items():
        piece(name, eye_alpha, eye_rect, f"peri_layer_eyes_{state}.webp")
    for state, name in mouths.items():
        piece(name, talk_alpha if state in TALKING else mouth_alpha, mouth_rect, f"peri_layer_mouth_{state}.webp")
    # Her face's area on the calm body (1024 px), to know what a body holds in front of it.
    face = np.zeros((SIZE, SIZE), np.float32)
    for alpha in (eye_alpha, mouth_alpha):
        face = np.maximum(face, cv2.resize(alpha, (SIZE, SIZE), interpolation=cv2.INTER_AREA))
    calm_rgba = np.asarray(cut(os.path.join(HERE, CALM_BODY)))
    matrix = placements()
    fronts = {}
    for state, name in bodies.items():
        picture = cut(os.path.join(HERE, name))
        picture.save(os.path.join(DRAWABLES, f"peri_layer_body_{state}.webp"), "WEBP", quality=90, method=6)
        if state == "calm":
            continue
        held = front(np.asarray(picture), calm_rgba, face, matrix(f"peri_{state}"), objects_only=not state.startswith("calm_"))
        if held:
            held[0].save(os.path.join(DRAWABLES, f"peri_layer_front_{state}.webp"), "WEBP", quality=92, method=6)
            fronts[state] = held[1]

    def entries(table, kind):
        return "".join(f'        "{s}" to R.drawable.peri_layer_{kind}_{s},\n' for s in table)

    box_1024 = lambda r: "floatArrayOf(" + ", ".join(f"{v * SCALE:.0f}f" for v in r) + ")"
    body_entries = "".join(f'        "{s}" to (R.drawable.peri_layer_body_{s} to R.drawable.peri_{s}),\n' for s in bodies)
    look = lambda v: f", look = {v}f" if v else ""
    front_entries = "".join(f'        "{s}" to (R.drawable.peri_layer_front_{s} to floatArrayOf({l}f, {t}f, {r}f, {b}f)),\n' for s, (l, t, r, b) in fronts.items())
    frame_entries = "".join(f'        R.drawable.peri_{n} to Frame("{b}", "{e}", "{m}"{look(v)}),\n' for n, (b, e, m, v) in frames.items())
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
            "    /** A frame by layers: its body, its eyes and mouth, and where the eyes look (-1 left, 1 right). */\n"
            "    class Frame(val body: String, val eyes: String, val mouth: String, val look: Float = 0f)\n\n"
            "    /** Each body, and the picture it was made from: that picture's eyes ([PeriAnchors]) place her face on it. */\n"
            "    val BODIES = mapOf(\n" + body_entries + "    )\n"
            "    /** What a body holds in front of her face (a cup, a letter, her hands), drawn over her eyes and mouth: where it goes on the body. */\n"
            "    val FRONTS = mapOf(\n" + front_entries + "    )\n"
            "    /** The app's frames drawn by layers; the others keep their whole picture. */\n"
            "    val FRAMES = mapOf(\n" + frame_entries + "    )\n"
            "    val EYES = mapOf(\n" + entries(eyes, "eyes") + "    )\n"
            "    val MOUTHS = mapOf(\n" + entries(mouths, "mouth") + "    )\n"
            "}\n"
        )
    print(f"eyes {len(eyes)} at {eye_rect}, mouths {len(mouths)} at {mouth_rect}, bodies {len(bodies)}, frames {len(frames)}, in front: {', '.join(fronts)}")


if __name__ == "__main__":
    main()
