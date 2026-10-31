"""rive_write.riv: the report being written. A pencil writes line after line in handwriting, the lines fade, and it
starts again, for as long as the model takes. No input: it plays as it appears. What comes first in the file is drawn
on top."""
import sys
from rivwriter import Riv, Machine

INK, SUN, SUN_LIP, CORAL, WOOD, LEAD = 0xB33F4D39, 0xFFFFC83D, 0xFFDDA52A, 0xFFFF8A7A, 0xFFF6D7A7, 0xFF3F4D39
SHARES = [0.62, 1.0, 0.94, 1.0, 0.7, 0.86]
LEFT, WIDTH, TOP, GAP, PER = 20, 280, 26, 30, 26  # PER: frames per line

r = Riv("Write", 320, 200)
ease = r.cubic(0.42, 0, 0.58, 1)
root = r.node(0, 0, 0, name="Page")

# The pencil (on top): its tip at the node, leaning back to the right.
pencil = r.node(root, name="Pencil", x=LEFT, y=TOP)
lean = r.node(pencil, name="Lean", rotation=-0.75)
tip = r.shape(lean, name="Lead")
r.path(tip, [(0, 0), (6, -4), (6, 4)])
r.fill(tip, LEAD)
wood = r.shape(lean, name="Wood")
r.path(wood, [(4, -5.5), (16, -7, 2), (16, 7, 2), (4, 5.5)])
r.fill(wood, WOOD)
body = r.shape(lean, name="Body")
r.rect(body, 46, 14, radius=3, x=16 + 23)
r.fill(body, SUN)
r.stroke(body, SUN_LIP, 2)
eraser = r.shape(lean, name="Eraser")
r.rect(eraser, 10, 14, radius=4, x=16 + 46 + 4)
r.fill(eraser, CORAL)

# The lines, a little wavy like handwriting, each drawn by trimming its stroke.
lines = []
for i, share in enumerate(SHARES):
    y = TOP + i * GAP
    length = WIDTH * share
    steps = max(4, int(length // 22))
    pts = [(LEFT + length * j / steps, y + (1.4 if j % 2 else -1.4) * (0 < j < steps), 10) for j in range(steps + 1)]
    line = r.shape(root, name=f"Line{i}")
    r.path(line, pts, closed=False)
    stroke, _ = r.stroke(line, INK, 3, cap=1, join=1)
    lines.append((line, r.trim(stroke, 0), y, length))

total = PER * len(lines)
frames = total + 50
_, k = r.animation("writing", frames)
xs, ys = [], []
for i, (line, trim, y, length) in enumerate(lines):
    t0, t1 = i * PER, i * PER + PER - 4
    # One key per frame: the first line starts at frame 0 itself.
    k(trim, "trimEnd", ([(0, 0, "hold")] if t0 else []) + [(t0, 0, "linear"), (t1, 1, "hold"), (frames, 1)])
    k(line, "opacity", [(0, 1, "hold"), (total + 22, 1, "linear"), (total + 40, 0, "hold"), (frames, 0)])
    xs += [(t0, LEFT, "linear"), (t1, LEFT + length, ease)]
    ys += [(t0, y, "linear"), (t1, y, ease)]
# Back to the start of the next line between lines; at the end it rests, then goes back to the top as the lines fade.
last_y, last_length = lines[-1][2], lines[-1][3]
xs += [(total + 20, LEFT + last_length, ease), (total + 40, LEFT, "hold")]
ys += [(total + 20, last_y, ease), (total + 40, TOP, "hold")]
k(pencil, "x", xs + [(frames, LEFT)])
k(pencil, "y", ys + [(frames, TOP)])
k(lean, "rotation", [(f, -0.75 + (0.06 if (f // 6) % 2 else -0.06), "linear") for f in range(0, total, 6)] + [(total, -0.75)])

m = Machine("Write")
r.save(sys.argv[1], m.objects([
    ("entry", None, [(3, [], 0)]), ("any", None, []), ("exit", None, []), ("anim", 0, []),
]))
