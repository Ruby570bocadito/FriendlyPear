"""rive_stars.riv: the quiz's score. Input "score" (0 to 6): that many stars pop in gold, one after another, each
with a little burst; the rest stay as empty outlines. What comes first in the file is drawn on top."""
import math, sys
from rivwriter import Riv, Machine

SUN, SUN_LIP, EMPTY, WHITE = 0xFFFFC83D, 0xFFDDA52A, 0xFFD8CCB2, 0xFFFFFFFF
COUNT, GAP = 6, 64

r = Riv("Stars", GAP * COUNT, 96)
out = r.cubic(0.2, 0.8, 0.3, 1)
pop = r.cubic(0.34, 1.56, 0.64, 1)


def star(outer, inner, corner):
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rad = outer if i % 2 == 0 else inner
        pts.append((rad * math.cos(a), rad * math.sin(a), corner if i % 2 == 0 else corner / 2))
    return pts


stars = []
for i in range(COUNT):
    slot = r.node(0, GAP * i + GAP / 2, 50, name=f"Star{i}")
    burst = []
    for j in range(6):
        a = j * math.pi / 3 - math.pi / 2
        dot = r.shape(slot, name=f"Burst{i}_{j}", opacity=0.0)
        r.ellipse(dot, 7, 7)
        r.fill(dot, SUN if j % 2 else 0xFF7CC242)
        burst.append((dot, math.cos(a), math.sin(a)))
    gold = r.shape(slot, name=f"Gold{i}", scaleX=0.0, scaleY=0.0)
    r.path(gold, star(26, 12, 4))
    r.fill(gold, SUN)
    r.stroke(gold, SUN_LIP, 3, join=1)
    empty = r.shape(slot, name=f"Empty{i}")
    r.path(empty, star(24, 11, 4))
    r.fill(empty, 0xFFF4EEE1)
    r.stroke(empty, EMPTY, 3, join=1)
    stars.append((gold, burst))

for n in range(COUNT + 1):
    _, k = r.animation(f"score_{n}", 30 + 12 * COUNT + 40, loop=0)
    for i, (gold, burst) in enumerate(stars):
        if i < n:
            t = 10 + 12 * i
            k(gold, "scaleX", [(0, 0, "hold"), (t, 0, pop), (t + 18, 1)])
            k(gold, "scaleY", [(0, 0, "hold"), (t, 0, pop), (t + 18, 1)])
            k(gold, "rotation", [(0, -0.6, "hold"), (t, -0.6, out), (t + 18, 0)])
            for dot, cx, cy in burst:
                k(dot, "x", [(0, cx * 14, "hold"), (t + 4, cx * 14, out), (t + 30, cx * 40)])
                k(dot, "y", [(0, cy * 14, "hold"), (t + 4, cy * 14, out), (t + 30, cy * 40)])
                k(dot, "opacity", [(0, 0, "hold"), (t + 4, 1, "linear"), (t + 20, 1, "linear"), (t + 34, 0)])
        else:
            k(gold, "scaleX", [(0, 0)]); k(gold, "scaleY", [(0, 0)])
            for dot, _, _ in burst:
                k(dot, "opacity", [(0, 0)])

m = Machine("Stars")
score = m.number("score", 0)
# Layer states: 0 entry, 1 any, 2 exit, then score_0 .. score_6 as 3 .. 9.
states = [("entry", None, [(3, [], 0)]),
          ("any", None, [(3 + n, [(score, 0, n)], 0) for n in range(COUNT + 1)]),
          ("exit", None, [])]
states += [("anim", n, []) for n in range(COUNT + 1)]
r.save(sys.argv[1], m.objects(states))
