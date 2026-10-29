"""
The image agent's pictures (assets/peri/sprites/, docs/PERI_SPRITES.md) cut out for the app, each at the size it's
drawn at:

    python assets/peri/sprites_cut.py [fx] [scene] [move] [mini]     needs Pillow and numpy (cutout.py)

fx     clay effects (hearts, notes, a drop, ?, !, a bulb, confetti, a puff of dust...): trimmed to the object, 256 px at
       most, for Sparkles                                                   → peri_fx_<name>.webp
scene  Peri's illustrations for the sections (diary, learn, family, settings...): 512 px  → peri_scene_<name>.webp
move   her movements (a jump, dance steps, a bow, a stretch, a turn...): 1024 px squares framed like her other
       pictures, so they swap with them in PeriFace                          → peri_move_<name>.webp, peri_turn_<name>.webp
mini   Mini Peri, only her head (peeking, waving, asleep...): 512 px          → peri_mini_<name>.webp
"""
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SPRITES = os.path.join(HERE, "sprites")
sys.path.insert(0, HERE)
from cutout import OUT, cut  # noqa: E402

# The white ones (a puff of dust, a snowflake, a sparkle) can't be told from the cream: Sparkles draws those itself.
FX = ["heart_coral", "heart_pink", "heart_broken", "music_note", "sweat_drop", "tear", "question", "exclamation", "bulb",
      "confetti_strip", "confetti_dot"]
# The soap bubble is see-through: its edge goes with the cream, so it isn't used.
SCENES = ["diary", "learn", "family", "settings", "checkup", "trophy", "cheer", "relax", "night", "morning"]
# Her movements with her face on (they're short: no blinking or talking in them); only those she does, and the
# section pictures and effects in use: the rest of the lotes wait for their place.
MOVES = ["jump_crouch", "jump_air", "jump_land", "dance1", "dance2", "dance3", "dance4", "hips", "bow1", "bow2", "stretch_up"]
TURNS = ["side_left", "back", "side_right"]
MINIS = ["noface", "leaf_left", "leaf_right", "peek", "wave", "umbrella", "sleep"]
FX_MAX = 256
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


def trimmed(image: Image.Image) -> Image.Image:
    """The object alone: cropped to what isn't see-through, with a little room for its soft edge."""
    alpha = np.asarray(image)[:, :, 3]
    ys, xs = np.nonzero(alpha > 8)
    pad = 6
    box = (max(0, xs.min() - pad), max(0, ys.min() - pad), min(image.width, xs.max() + pad + 1), min(image.height, ys.max() + pad + 1))
    return image.crop(box)


def main() -> None:
    groups = set(sys.argv[1:]) or {"fx", "scene", "move", "mini"}
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
        print(f"move: {len(MOVES) + len(TURNS)}")
    if "mini" in groups:
        for name in MINIS:
            save(cut(os.path.join(SPRITES, f"mini_{name}.webp")).resize((MINI_SIZE, MINI_SIZE), Image.LANCZOS), f"peri_mini_{name}.webp")
        print(f"mini: {len(MINIS)}")


if __name__ == "__main__":
    main()
