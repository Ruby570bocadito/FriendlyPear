# The fake parcel notice for "Look at a picture" (a made-up company, never a real one): python fake_letter_en.py out.jpg
import random, sys
from PIL import Image, ImageDraw, ImageFont
W, H = 900, 1200
im = Image.new("RGB", (W, H), "#fdfcf7")
d = ImageDraw.Draw(im)
def font(size, bold=False):
    for name in (["arialbd.ttf"] if bold else ["arial.ttf"]):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            pass
    return ImageFont.load_default()
d.rectangle([0, 0, W, 130], fill="#e8b400")
d.text((40, 38), "ParcelNow Deliveries", fill="#1a1a1a", font=font(48, True))
lines = [
    ("MISSED DELIVERY NOTICE", 40, True),
    ("", 20, False),
    ("Dear customer,", 30, False),
    ("Your parcel no. 4417-UK is held at", 30, False),
    ("our depot for unpaid customs fees.", 30, False),
    ("", 20, False),
    ("To get it TODAY, pay 1.99 GBP", 32, True),
    ("by scanning this QR code or at:", 30, False),
    ("http://parcelnow-fees.top/pay", 30, True),
    ("", 20, False),
    ("If you do not pay within 24 hours, the", 30, False),
    ("parcel will be returned to the sender.", 30, False),
]
y = 180
for text, size, bold in lines:
    d.text((60, y), text, fill="#222222", font=font(size, bold))
    y += size + 18
# A QR-looking code: three finder squares and random modules.
random.seed(7)
qx, qy, cell, n = 300, 820, 10, 29
d.rectangle([qx - 20, qy - 20, qx + n * cell + 20, qy + n * cell + 20], fill="white")
for r in range(n):
    for c in range(n):
        if random.random() < 0.5:
            d.rectangle([qx + c * cell, qy + r * cell, qx + (c + 1) * cell - 1, qy + (r + 1) * cell - 1], fill="black")
for fx, fy in [(0, 0), (n - 7, 0), (0, n - 7)]:
    x0, y0 = qx + fx * cell, qy + fy * cell
    d.rectangle([x0, y0, x0 + 7 * cell - 1, y0 + 7 * cell - 1], fill="black")
    d.rectangle([x0 + cell, y0 + cell, x0 + 6 * cell - 1, y0 + 6 * cell - 1], fill="white")
    d.rectangle([x0 + 2 * cell, y0 + 2 * cell, x0 + 5 * cell - 1, y0 + 5 * cell - 1], fill="black")
im.save(sys.argv[1], quality=88)
print("saved", im.size)
