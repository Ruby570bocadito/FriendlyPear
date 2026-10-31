# Peri for the heist video, straight from the app: her moods with their talking, blinking and glancing frames, her
# entrance and jumps frame by frame, her moments, her effects, and her wardrobe. The pictures are the app's own
# cut-outs (android/app/src/main/res/drawable-nodpi, 1024 px squares); each outfit is laid on her the way the app does
# it (ui/peri/Outfit.kt): placed from where her eyes are in that very frame (ui/peri/PeriAnchors.kt), sized in eye
# distances and turned with her head. Hats that need her bare head (straw hat, beanie) are left out: only her still
# pose has a bare-headed picture, and she talks in these.
#   python video/scripts/peri_app.py
import math
import re
import shutil
from pathlib import Path

from PIL import Image

root = Path(__file__).resolve().parents[2]
drawables = root / "android/app/src/main/res/drawable-nodpi"
anchors_kt = root / "android/app/src/main/java/app/friendlypear/ui/peri/PeriAnchors.kt"
out = root / "video/public/peri7"

MOODS = ["calm", "happy", "listening", "thinking", "worried", "protective", "calling", "waiting"]
FRAMES = ["", "_talk", "_talko", "_talkwide", "_blink", "_blinkhalf", "_glanceleft", "_glanceright", "_wink", "_gesturea", "_gestureb"]
ANIMS = [f"anim_drop_0{i}" for i in range(1, 7)] + [f"anim_cheer_0{i}" for i in range(1, 5)] + [f"anim_jump_0{i}" for i in range(1, 10)]
MOMENTS = ["calm_hero", "calm_medal", "calm_party", "calm_love", "calm_clap"]
# For "Peri's diary": waking up on her hill, each trap's card from the app's lessons, and the album of her pictures.
DIARY = ["sleeping", "calm_yawn", "move_stretch_up", "anim_star_01"] + [f"trap_{t}_1" for t in ("family", "callback", "code", "link", "official", "payment", "prize", "parcel", "romance", "remote", "bill")] + [
    f"calm_{m}" for m in ("tea", "letter", "glasses", "scarf", "idea", "thumbsup", "wave1", "flower", "butterfly", "hum", "detective", "peekaboo", "surprised", "bye", "sorry", "dizzy", "laugh", "phone", "wink")
] + ["happy_wink", "sleeping_blink"]
EFFECTS = ["fx_star", "fx_star_green", "fx_heart_pink", "fx_heart_coral", "fx_music_note", "fx_confetti_dot", "fx_confetti_strip"]

# From Outfit.Item: slot, width in eye distances, the picture's point that goes there (ax, ay), and where that is (dx, dy
# eye distances from between her eyes, turned with her).
WEAR = {
    "crown": ("head", 1.25, 0.5, 0.92, 0.0, -1.08),
    "flowers": ("head", 1.55, 0.5, 0.75, 0.0, -0.98),
    "bow": ("head", 0.72, 0.5, 0.5, 0.06, -1.22),
    "sunglasses": ("eyes", 1.85, 0.5, 0.5, 0.0, 0.0),
    "hearts": ("eyes", 1.93, 0.5, 0.5, 0.0, 0.0),
    "glasses": ("eyes", 1.91, 0.5, 0.5, 0.0, 0.0),
    "bowtie": ("neck", 0.9, 0.5, 0.5, 0.0, 0.78),
    "tie": ("neck", 0.46, 0.5, 0.04, 0.0, 0.6),
    "bandana": ("neck", 0.85, 0.5, 0.05, 0.0, 0.66),
}
# The outfits she shows off, each worn on her happy talking frames.
OUTFITS = {
    "crown": ["crown", "bowtie"],
    "flowers": ["flowers", "hearts"],
    "bow": ["bow", "sunglasses", "tie"],
    "glasses": ["glasses", "bandana"],
}


def anchors():
    """PeriAnchors.EYES and HEADS: picture name -> (left x, y, right x, y)."""
    text = anchors_kt.read_text(encoding="utf-8")
    maps = {}
    for name, body in re.findall(r"val (EYES|HEADS): Map<Int, FloatArray> = mapOf\((.*?)\n    \)", text, re.S):
        maps[name] = {m[0]: tuple(float(v) for v in m[1:]) for m in re.findall(r"R\.drawable\.(\w+) to floatArrayOf\(([\d.]+)f, ([\d.]+)f, ([\d.]+)f, ([\d.]+)f\)", body)}
    return maps["EYES"], maps["HEADS"]


def dress(picture, eyes_at, head_at, items):
    """The picture with the items on, as OutfitLayer draws them."""
    canvas = picture.copy()
    for item in items:
        slot, width, ax, ay, dx, dy = WEAR[item]
        lx, ly, rx, ry = head_at if slot == "head" else eyes_at
        d = math.hypot(rx - lx, ry - ly)
        angle = math.atan2(ry - ly, rx - lx)
        cx, cy = (lx + rx) / 2, (ly + ry) / 2
        wear = Image.open(drawables / f"peri_wear_{item}.webp").convert("RGBA")
        w = width * d
        h = w * wear.height / wear.width
        wear = wear.resize((max(1, round(w)), max(1, round(h))), Image.LANCZOS)
        px = cx + math.cos(angle) * dx * d - math.sin(angle) * dy * d
        py = cy + math.sin(angle) * dx * d + math.cos(angle) * dy * d
        # Turn it about its anchor point, then put that point at (px, py).
        pivot = (ax * wear.width, ay * wear.height)
        turned = wear.rotate(-math.degrees(angle), resample=Image.BICUBIC, expand=True, center=pivot)
        # Where the anchor point ends up in the turned, expanded picture.
        cos_a, sin_a = math.cos(angle), math.sin(angle)
        corners = [(-pivot[0], -pivot[1]), (wear.width - pivot[0], -pivot[1]), (-pivot[0], wear.height - pivot[1]), (wear.width - pivot[0], wear.height - pivot[1])]
        turned_corners = [(x * cos_a - y * sin_a, x * sin_a + y * cos_a) for x, y in corners]
        min_x, min_y = min(c[0] for c in turned_corners), min(c[1] for c in turned_corners)
        canvas.alpha_composite(turned, (round(px + min_x), round(py + min_y)))
    return canvas


out.mkdir(parents=True, exist_ok=True)
names = [f"{mood}{frame}" for mood in MOODS for frame in FRAMES] + ANIMS + MOMENTS + EFFECTS + DIARY
for name in names:
    source = drawables / f"peri_{name}.webp"
    if source.exists():
        shutil.copyfile(source, out / f"{name}.webp")
    else:
        print("missing", source.name)

eyes, heads = anchors()
for outfit, items in OUTFITS.items():
    for frame in ["", "_talk", "_talko", "_talkwide", "_blink", "_blinkhalf"]:
        name = f"peri_happy{frame}"
        picture = Image.open(drawables / f"{name}.webp").convert("RGBA")
        face = eyes.get(name, eyes["peri_happy"])
        dress(picture, face, heads.get(name, face), items).save(out / f"dressed_{outfit}{frame}.webp", quality=92)
print(len(names), "pictures and", len(OUTFITS), "outfits in", out)
