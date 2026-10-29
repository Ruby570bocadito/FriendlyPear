"""
The image agent's pictures (assets/peri/sprites/, docs/PERI_SPRITES.md) cut out for the app, each at the size it's
drawn at:

    python assets/peri/sprites_cut.py [fx] [scene] [move] [mini] [ui]     needs Pillow and numpy (cutout.py)

fx     clay effects (hearts, notes, a drop, ?, !, a bulb, confetti, a puff of dust...): trimmed to the object, 256 px at
       most, for Sparkles                                                   → peri_fx_<name>.webp
scene  Peri's illustrations for the sections (diary, learn, family, settings...): 512 px  → peri_scene_<name>.webp
move   her movements (dance steps, a bow, a stretch, a turn; her jump in 9 frames and her entrance from the sky in 6):
       1024 px squares framed like her other pictures, so they swap with them in PeriFace
                                                    → peri_move_<name>.webp, peri_turn_<name>.webp, peri_anim_<name>.webp
mini   Mini Peri, only her head (her leaf in the wind, waving, under her umbrella, asleep): 512 px, and where Peri's
       face goes on each, found by her cheeks          → peri_mini_<name>.webp, android/.../ui/peri/MiniHeadData.kt
ui     the interface pictures (assets/ui/): Peri for each setup step, the empty family, no connection, the milestones
       and their badges, the quiz, 512 px, and the checkup's, 256 px          → peri_ui_<name>.webp, peri_check_<name>.webp
       and the launcher icon and the launch screen, framed so no mask cuts them → peri_launcher(_bg).webp, peri_splash.webp

The heads have no face: Peri's eye and mouth pieces (layers.py) go on them. The blush on her cheeks is on the calm Peri and
on every head, so the two cheeks give the shift, turn and size from her calm face to each head's.
"""
import os
import sys

import math

import cv2
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SPRITES = os.path.join(HERE, "sprites")
sys.path.insert(0, HERE)
from cutout import OUT, cut  # noqa: E402

KOTLIN = os.path.join(HERE, "..", "..", "android", "app", "src", "main", "java", "app", "friendlypear", "ui", "peri", "MiniHeadData.kt")

# The white ones (a puff of dust, a snowflake, a sparkle) can't be told from the cream: Sparkles draws those itself.
FX = ["heart_coral", "heart_pink", "heart_broken", "music_note", "sweat_drop", "tear", "question", "exclamation", "bulb",
      "confetti_strip", "confetti_dot"]
# The soap bubble is see-through: its edge goes with the cream, so it isn't used.
SCENES = ["diary", "learn", "family", "settings", "checkup", "trophy", "cheer", "night", "morning"]
# Her movements with her face on (they're short: no blinking or talking in them); only those she does, and the
# section pictures and effects in use: the rest of the lotes wait for their place.
MOVES = ["dance1", "dance2", "dance3", "dance4", "hips", "bow1", "bow2", "stretch_up"]
# Her jump and her entrance frame by frame (lote 16), on the ground line by ground.py; the ones in the air are drawn
# higher by PeriFace (sprites/anim_offsets.json), since Muse keeps her in the middle.
# More jumps and entrances (lote 17): to celebrate, with a turn, a star; out of the ground, down with her umbrella.
ANIMS = ([f"jump_{i:02d}" for i in range(1, 10)] + [f"drop_{i:02d}" for i in range(1, 7)]
         + [f"cheer_{i:02d}" for i in range(1, 5)] + [f"spin_{i:02d}" for i in range(1, 5)] + ["star_01"]
         + [f"sprout_{i:02d}" for i in range(1, 4)] + [f"float_{i:02d}" for i in range(1, 4)])
# A turn in 8 views (lotes 4 and 20): her face away, her outfit can't follow, so she turns without it.
TURNS = ["side_left", "back", "side_right", "threequarter_right", "threequarter_back_right", "threequarter_back_left", "threequarter_left"]
# Peeking from behind a wall, her cheeks barely show, so her face can't be placed: not used.
MINIS = ["noface", "leaf_left", "leaf_right", "wave", "umbrella", "sleep"]
# The interface pictures (lotes 14 and 15). The question mark is white on cream: Peri's own bubble says it instead.
UI = ["perm_sms", "perm_calls", "perm_notifications", "perm_battery", "perm_usage", "perm_location", "perm_overlay",
      "empty_diary", "empty_family", "offline", "unlock_guide", "badge_bronze", "badge_silver", "badge_gold", "tool_game",
      "welcome_shield", "shield_first", "shield_10", "shield_100", "streak_3", "streak_7", "streak_30"]
# The checkup's pictures (lote 29), small beside each check.
CHECKS = ["lock", "update", "adb", "screen", "dns", "alerts", "calls"]
CHECK_SIZE = 256
# Things nearly as light as the cream (a silver badge, the quiz's cards), cut out whole.
UI_SOLID = {"badge_bronze", "badge_silver", "badge_gold", "tool_game"}
# A white cloud and a white envelope beside Peri: only what's almost exactly the cream goes.
UI_WHITE = {"offline", "welcome_shield"}
# Peri on a green base with her trophy: the base isn't a floor shadow, it stays.
UI_STANDING = {"shield_100"}
WHITE_THRESHOLD = 22
UI_DIR = os.path.join(HERE, "..", "ui")
FX_MAX = 256
UI_SIZE = 512
# The adaptive icon is 108 dp, and every launcher's mask leaves at least a 66 dp circle; the launch screen shows its
# icon in a 192 dp circle of 288 dp. The pictures are framed to fit those circles whole.
LAUNCHER_SIZE = 432
LAUNCHER_REACH = 32 / 108
SPLASH_SIZE = 864
SPLASH_REACH = 92 / 288
LIGHT_THRESHOLD = 40
SCENE_SIZE = 512
MINI_SIZE = 512


def save(image: Image.Image, name: str) -> None:
    image.save(os.path.join(OUT, name), "WEBP", quality=90, alpha_quality=100, method=6)


def solid(path: str) -> Image.Image:
    """An effect cut out whole: light ones (a pink heart, a drop) are nearly as light as the cream, so the background
    is taken more strictly, and any notch or hole the fill still bit into the object is closed again."""
    import cv2

    image = cut(path, threshold=LIGHT_THRESHOLD)
    rgba = np.asarray(image).copy()
    alpha = rgba[:, :, 3]
    closed = cv2.morphologyEx(alpha, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9)))
    # Holes inside the object: whatever the outside can't reach.
    outside = np.zeros((alpha.shape[0] + 2, alpha.shape[1] + 2), np.uint8)
    mask = (closed < 128).astype(np.uint8)
    filled = mask.copy()
    cv2.floodFill(filled, outside, (0, 0), 2)
    holes = (filled == 1)
    closed[holes] = 255
    rgba[:, :, 3] = np.maximum(alpha, closed)
    return Image.fromarray(rgba, "RGBA")


def cheeks(rgba: np.ndarray):
    """Her two cheeks on a 1024 px picture: the two biggest pink blobs at about the same height in its lower part (an
    umbrella or a heart above her is pink too), left one first."""
    hsv = cv2.cvtColor(np.ascontiguousarray(rgba[:, :, :3]), cv2.COLOR_RGB2HSV)
    pink = ((hsv[:, :, 0] <= 10) | (hsv[:, :, 0] >= 165)) & (hsv[:, :, 1] > 60) & (hsv[:, :, 2] > 150) & (rgba[:, :, 3] > 200)
    pink[: rgba.shape[0] * 2 // 5] = False
    pink = cv2.morphologyEx(pink.astype(np.uint8), cv2.MORPH_OPEN, np.ones((5, 5), np.uint8))
    n, _, stats, centres = cv2.connectedComponentsWithStats(pink)
    blobs = sorted(range(1, n), key=lambda i: -stats[i, cv2.CC_STAT_AREA])[:4]
    pairs = [(a, b) for a in blobs for b in blobs if a < b and abs(centres[a][1] - centres[b][1]) < 40]
    a, b = max(pairs, key=lambda ab: stats[ab[0], cv2.CC_STAT_AREA] + stats[ab[1], cv2.CC_STAT_AREA])
    return sorted([tuple(centres[a]), tuple(centres[b])])


def place(calm, head):
    """Where Peri's calm face goes on a head: its cheeks' middle (as a share of the picture), turn (°) and size (picture
    shares per px of the calm 1024 px picture)."""
    (l0, r0), (l1, r1) = calm, head
    turn = math.degrees(math.atan2(r1[1] - l1[1], r1[0] - l1[0]) - math.atan2(r0[1] - l0[1], r0[0] - l0[0]))
    size = math.hypot(r1[0] - l1[0], r1[1] - l1[1]) / math.hypot(r0[0] - l0[0], r0[1] - l0[1]) / 1024
    return (l1[0] + r1[0]) / 2 / 1024, (l1[1] + r1[1]) / 2 / 1024, turn, size


def trimmed(image: Image.Image) -> Image.Image:
    """The object alone: cropped to what isn't see-through, with a little room for its soft edge."""
    alpha = np.asarray(image)[:, :, 3]
    ys, xs = np.nonzero(alpha > 8)
    pad = 6
    box = (max(0, xs.min() - pad), max(0, ys.min() - pad), min(image.width, xs.max() + pad + 1), min(image.height, ys.max() + pad + 1))
    return image.crop(box)


def framed(image: Image.Image, size: int, reach: float) -> Image.Image:
    """[image] on a [size] px square, its smallest enclosing circle in the middle with a radius of [reach] of the square."""
    piece = trimmed(image)
    ys, xs = np.nonzero(np.asarray(piece)[:, :, 3] > 24)
    (cx, cy), far = cv2.minEnclosingCircle(np.column_stack([xs, ys]).astype(np.float32))
    scale = reach * size / far
    piece = piece.resize((round(piece.width * scale), round(piece.height * scale)), Image.LANCZOS)
    out = Image.new("RGBA", (size, size))
    out.paste(piece, (round(size / 2 - cx * scale), round(size / 2 - cy * scale)), piece)
    return out


def main() -> None:
    groups = set(sys.argv[1:]) or {"fx", "scene", "move", "mini", "ui"}
    if "fx" in groups:
        for name in FX:
            piece = trimmed(solid(os.path.join(SPRITES, f"fx_{name}.webp")))
            piece.thumbnail((FX_MAX, FX_MAX), Image.LANCZOS)
            save(piece, f"peri_fx_{name}.webp")
        print(f"fx: {len(FX)}")
    if "scene" in groups:
        for name in SCENES:
            save(cut(os.path.join(SPRITES, f"scene_{name}.webp")).resize((SCENE_SIZE, SCENE_SIZE), Image.LANCZOS), f"peri_scene_{name}.webp")
        print(f"scene: {len(SCENES)}")
    if "move" in groups:
        for name in MOVES:
            save(cut(os.path.join(SPRITES, f"move_{name}.webp")), f"peri_move_{name}.webp")
        for name in TURNS:
            save(cut(os.path.join(SPRITES, f"turn_{name}.webp")), f"peri_turn_{name}.webp")
        for name in ANIMS:
            save(cut(os.path.join(SPRITES, f"anim_{name}.webp")), f"peri_anim_{name}.webp")
        print(f"move: {len(MOVES) + len(TURNS) + len(ANIMS)}")
    if "mini" in groups:
        calm = cheeks(np.asarray(Image.open(os.path.join(OUT, "peri_calm.webp")).convert("RGBA")))
        places = {}
        for name in MINIS:
            head = cut(os.path.join(SPRITES, f"mini_{name}.webp"))
            places[name] = place(calm, cheeks(np.asarray(head)))
            save(head.resize((MINI_SIZE, MINI_SIZE), Image.LANCZOS), f"peri_mini_{name}.webp")
        entries = "".join(
            f'        "{n}" to (R.drawable.peri_mini_{n} to floatArrayOf({x:.4f}f, {y:.4f}f, {t:.2f}f, {k:.6f}f)),\n' for n, (x, y, t, k) in places.items()
        )
        with open(KOTLIN, "w", encoding="utf-8", newline="\n") as f:
            f.write(
                "// Generated by assets/peri/sprites_cut.py: do not edit.\n"
                "package app.friendlypear.ui.peri\n\n"
                "import app.friendlypear.R\n\n"
                "/**\n"
                " * Mini Peri's heads (lote 5) and where Peri's face goes on each, found by her cheeks: their middle (x, y as shares of\n"
                " * the picture), turn (°) and size (picture shares per px of the calm 1024 px picture), from the calm cheeks' middle.\n"
                " */\n"
                "internal object MiniHeadData {\n"
                f"    val CALM_CHEEKS = floatArrayOf({(calm[0][0] + calm[1][0]) / 2:.1f}f, {(calm[0][1] + calm[1][1]) / 2:.1f}f)\n"
                "    val HEADS = mapOf(\n" + entries + "    )\n"
                "}\n"
            )
        print(f"mini: {len(MINIS)}")
    if "ui" in groups:
        for name in UI:
            path = os.path.join(UI_DIR, f"ui_{name}.webp")
            piece = (solid(path) if name in UI_SOLID
                     else cut(path, remove_floor=False, threshold=WHITE_THRESHOLD) if name in UI_STANDING
                     else cut(path, threshold=WHITE_THRESHOLD) if name in UI_WHITE
                     else cut(path))
            save(piece.resize((UI_SIZE, UI_SIZE), Image.LANCZOS), f"peri_ui_{name}.webp")
        for name in CHECKS:
            save(cut(os.path.join(UI_DIR, f"check_{name}.webp")).resize((CHECK_SIZE, CHECK_SIZE), Image.LANCZOS), f"peri_check_{name}.webp")
        save(framed(cut(os.path.join(UI_DIR, "icon_peri_shield.webp")), LAUNCHER_SIZE, LAUNCHER_REACH), "peri_launcher.webp")
        Image.open(os.path.join(UI_DIR, "icon_bg.webp")).convert("RGB").resize((LAUNCHER_SIZE, LAUNCHER_SIZE), Image.LANCZOS).save(
            os.path.join(OUT, "peri_launcher_bg.webp"), "WEBP", quality=90, method=6)
        save(framed(cut(os.path.join(UI_DIR, "splash_peri.webp")), SPLASH_SIZE, SPLASH_REACH), "peri_splash.webp")
        print(f"ui: {len(UI) + len(CHECKS) + 3}")


if __name__ == "__main__":
    main()
