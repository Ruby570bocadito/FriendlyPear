"""Joins the eight concept designs into one labelled sheet: python assets/peri/concepts_sheet.py"""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
NAMES = [("clay", "1 · Clay 3D"), ("watercolor", "2 · Watercolor"), ("sticker", "3 · Kawaii sticker"), ("grandma", "4 · Grandma's friend"),
         ("hero", "5 · Little hero"), ("geometric", "6 · Geometric"), ("plush", "7 · Plush toy"), ("anime", "8 · Anime")]
S, PAD, LABEL = 480, 24, 56
W, H = 4 * S + 5 * PAD, 2 * (S + LABEL) + 3 * PAD + 90
sheet = Image.new("RGB", (W, H), "#FFF8EC")
d = ImageDraw.Draw(sheet)
try:
    title, font = ImageFont.truetype("arialbd.ttf", 46), ImageFont.truetype("arialbd.ttf", 30)
except OSError:
    title = font = ImageFont.load_default()
d.text((PAD, 24), "Peri · 8 new designs (Muse Image)", fill="#25541A", font=title)
for i, (name, label) in enumerate(NAMES):
    im = Image.open(os.path.join(HERE, "concepts", f"concept_{name}.webp")).convert("RGB").resize((S, S), Image.LANCZOS)
    x = PAD + (i % 4) * (S + PAD)
    y = 90 + PAD + (i // 4) * (S + LABEL + PAD)
    sheet.paste(im, (x, y))
    d.text((x + 8, y + S + 10), label, fill="#1F2A1A", font=font)
out = os.path.join(HERE, "concepts", "peri_concepts.webp")
sheet.save(out, quality=88)
print(out)
