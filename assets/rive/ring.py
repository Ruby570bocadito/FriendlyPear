"""rive_ring.riv: the checkup's ring around Peri. Input "percent" (0, 10, ... 100): the ring fills to it, coral while
something is left to fix, green with a burst of sparks when everything is in order. What comes first in the file is
drawn on top."""
import math, sys
from rivwriter import Riv, Machine

GREEN, CORAL, TRACK = 0xFF7CC242, 0xFFFF8A7A, 0xFFEDE4D0

r = Riv("Ring", 240, 240)
out = r.cubic(0.2, 0.8, 0.3, 1)
root = r.node(0, 120, 120, name="Ring")

sparks = []
for i in range(10):
    a = i * math.pi / 5
    s = r.shape(root, name=f"Spark{i}", opacity=0.0)
    r.ellipse(s, 10, 10)
    r.fill(s, 0xFFFFC83D if i % 2 else GREEN)
    sparks.append((s, math.cos(a), math.sin(a)))

fill = r.shape(root, name="Fill")  # an ellipse starts at 12 o'clock
r.ellipse(fill, 176, 176)
fill_stroke, fill_color = r.stroke(fill, CORAL, 14, cap=1)
trim = r.trim(fill_stroke, 0)

track = r.shape(root, name="Track")
r.ellipse(track, 176, 176)
r.stroke(track, TRACK, 14)

for p in range(0, 101, 10):
    done = p == 100
    _, k = r.animation(f"p{p}", 90, loop=0)
    k(trim, "trimEnd", [(0, 0, out), (50, max(p / 100, 0.001))])
    k(fill_color, "color", [(0, CORAL, "hold"), (44, CORAL if not done else CORAL, "linear"), (54, GREEN if done else CORAL)], color=True)
    k(fill, "opacity", [(0, 1 if p else 0)])
    for s, cx, cy in sparks:
        if done:
            k(s, "x", [(0, cx * 88, "hold"), (50, cx * 88, out), (85, cx * 112)])
            k(s, "y", [(0, cy * 88, "hold"), (50, cy * 88, out), (85, cy * 112)])
            k(s, "opacity", [(0, 0, "hold"), (50, 1, "linear"), (70, 1, "linear"), (88, 0)])
        else:
            k(s, "opacity", [(0, 0)])

m = Machine("Ring")
percent = m.number("percent", 0)
r.save(sys.argv[1], m.objects([
    ("entry", None, [(3, [], 0)]),
    ("any", None, [(3 + i, [(percent, 0, i * 10)], 0) for i in range(11)]),
    ("exit", None, []),
] + [("anim", i, []) for i in range(11)]))
