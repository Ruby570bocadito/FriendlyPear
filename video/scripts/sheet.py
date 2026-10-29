"""Contact sheet of some pictures, each with its name: python video/scripts/sheet.py <sheet.jpg> <picture.webp>..."""
import os
import sys
from PIL import Image, ImageDraw, ImageFont

out, paths = sys.argv[1], sys.argv[2:]
S, PAD, LABEL, COLS = 300, 14, 30, 5
rows = (len(paths) + COLS - 1) // COLS
sheet = Image.new("RGB", (COLS * S + (COLS + 1) * PAD, rows * (S + LABEL + PAD) + PAD), "#9A9488")
d = ImageDraw.Draw(sheet)
try:
    font = ImageFont.truetype("arialbd.ttf", 18)
except OSError:
    font = ImageFont.load_default()
for i, path in enumerate(paths):
    x = PAD + (i % COLS) * (S + PAD)
    y = PAD + (i // COLS) * (S + LABEL + PAD)
    if os.path.exists(path):
        im = Image.open(path).convert("RGB")
        im.thumbnail((S, S), Image.LANCZOS)
        sheet.paste(im, (x + (S - im.width) // 2, y + (S - im.height) // 2))
    d.text((x + 4, y + S + 6), os.path.splitext(os.path.basename(path))[0], fill="#FFFFFF", font=font)
os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
sheet.save(out, quality=85)
print(out)
