# Peri's new poses for "Peri's diary" (diary_images.ts), cut out like the app's (assets/peri/cutout.py) so she stands on
# any picture: the pose's outline is found once and used for every mouth frame too, so her silhouette never shimmers
# while she talks, and everything is moved up or down so her feet sit where the app's cut-outs have them (SOLES in
# video/src/cast.tsx). The mouth frames are the steadied ones (heist_stabilize.py: the pose with only its mouth from
# the frame); the eye frames stay transparent overlays, moved the same way.
#   python video/scripts/peri_cut.py  ->  video/public/diary/peri/<pose>[_<frame>].png
import sys
from pathlib import Path

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(root / "assets" / "peri"))
import cutout  # noqa: E402  (the app's own cut-out)

pub = root / "video" / "public"
out = pub / "diary" / "peri"
POSES = ["peri_phone_check", "peri_hangup", "peri_stop", "peri_wait", "peri_key", "peri_lantern", "peri_detective"]
MOUTHS = ["slight", "half", "open", "round"]
EYES = ["blink", "blinkhalf"]
SIZE = cutout.SIZE
SOLES = 0.925


def shifted(image: Image.Image, dy: int) -> Image.Image:
    """The picture moved down by dy pixels (up if negative), on a transparent canvas."""
    canvas = Image.new("RGBA", image.size, (0, 0, 0, 0))
    canvas.paste(image, (0, dy))
    return canvas


out.mkdir(parents=True, exist_ok=True)
for name in POSES:
    base_path = pub / "diary" / f"{name}.png"
    base = cutout.cut(str(base_path))
    alpha = np.asarray(base)[..., 3].astype(np.float32) / 255
    rows = np.nonzero((alpha > 0.47).sum(axis=1) > 6)[0]
    dy = int(round(SOLES * SIZE - rows.max()))
    rgb_base = np.asarray(Image.open(base_path).convert("RGB").resize((SIZE, SIZE), Image.LANCZOS)).astype(np.int32)
    bg = cutout.background_colour(rgb_base)
    shifted(base, dy).save(out / f"{name}.png")
    for shape in MOUTHS:
        frame = pub / "heist" / "v" / f"{name}_{shape}.png"
        rgb = np.asarray(Image.open(frame).convert("RGB").resize((SIZE, SIZE), Image.LANCZOS)).astype(np.float32)
        # The same unmixing of the cream from the edges as the app's cut-out, with the pose's own outline.
        safe = np.maximum(alpha, 1e-3)[..., None]
        fg = np.clip((rgb - (1 - alpha[..., None]) * bg) / safe, 0, 255)
        fg = np.where(alpha[..., None] > 0.98, rgb, fg)
        shifted(Image.fromarray(np.dstack([fg, alpha * 255]).astype(np.uint8), "RGBA"), dy).save(out / f"{name}_{shape}.png")
    for shape in EYES:
        frame = Image.open(pub / "heist" / "v" / f"{name}_{shape}.png").convert("RGBA").resize((SIZE, SIZE), Image.LANCZOS)
        shifted(frame, dy).save(out / f"{name}_{shape}.png")
    print(f"{name}: feet moved {dy:+d} px")
