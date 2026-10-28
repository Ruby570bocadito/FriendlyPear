"""Contact sheet of one lote of sprites, each with its name: python assets/peri/sprites_sheet.py <lote> name1 name2 ..."""
import os
import sys
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SPRITES = os.path.join(HERE, "sprites")
lote, names = sys.argv[1], sys.argv[2:]
S, PAD, LABEL, COLS = 320, 16, 34, 5
rows = (len(names) + COLS - 1) // COLS
sheet = Image.new("RGB", (COLS * S + (COLS + 1) * PAD, rows * (S + LABEL + PAD) + PAD), "#E8E0D0")
d = ImageDraw.Draw(sheet)
try:
    font = ImageFont.truetype("arialbd.ttf", 20)
except OSError:
    font = ImageFont.load_default()
for i, name in enumerate(names):
    x = PAD + (i % COLS) * (S + PAD)
    y = PAD + (i // COLS) * (S + LABEL + PAD)
    path = os.path.join(SPRITES, f"{name}.webp")
    if os.path.exists(path):
        sheet.paste(Image.open(path).convert("RGB").resize((S, S), Image.LANCZOS), (x, y))
    d.text((x + 4, y + S + 6), name, fill="#1F2A1A", font=font)
os.makedirs(os.path.join(SPRITES, "hojas"), exist_ok=True)
out = os.path.join(SPRITES, "hojas", f"{lote}.jpg")
sheet.save(out, quality=85)
print(out)
