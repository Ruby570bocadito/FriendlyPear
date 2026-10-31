"""rive_scan.riv: Peri's magnifying glass over a picture. Input "state": 0 nothing shown, 1 looking (the glass
circles the picture), 2 fine (green tick), 3 in doubt (amber, three dots), 4 a trap (red "!"). What comes first in
the file is drawn on top."""
import math, sys
from rivwriter import Riv, Machine

GREEN, AMBER, RED, WHITE, INK = 0xFF2E8B2A, 0xFFE59A00, 0xFFC0261C, 0xFFFFFFFF, 0xFF25541A

r = Riv("Scan", 240, 240)
ease = r.cubic(0.42, 0, 0.58, 1)
out = r.cubic(0.2, 0.8, 0.3, 1)
pop = r.cubic(0.34, 1.56, 0.64, 1)
root = r.node(0, 120, 120, name="Scan")



# Badges (on top): each a disc with its sign.
ok = r.node(root, name="Ok", scaleX=0.0, scaleY=0.0)
tick = r.shape(ok, name="Tick")
r.path(tick, [(-26, 2), (-7, 22), (28, -18)], closed=False)
r.stroke(tick, WHITE, 14, cap=1, join=1)
disc = r.shape(ok); r.ellipse(disc, 110, 110); r.fill(disc, GREEN); r.stroke(disc, WHITE, 6)

doubt = r.node(root, name="Doubt", scaleX=0.0, scaleY=0.0)
for x in (-24, 0, 24):
    d = r.shape(doubt); r.ellipse(d, 15, 15, x=x); r.fill(d, WHITE)
disc = r.shape(doubt); r.ellipse(disc, 110, 110); r.fill(disc, AMBER); r.stroke(disc, WHITE, 6)

trap = r.node(root, name="Trap", scaleX=0.0, scaleY=0.0)
bang = r.shape(trap); r.rect(bang, 14, 44, radius=7, y=-10); r.ellipse(bang, 15, 15, y=28); r.fill(bang, WHITE)
disc = r.shape(trap); r.ellipse(disc, 110, 110); r.fill(disc, RED); r.stroke(disc, WHITE, 6)

# The glass: a lens with a handle, on its orbit.
glass = r.node(root, name="Glass", opacity=0.0)
lens = r.shape(glass, name="Lens")
r.ellipse(lens, 62, 62)
r.fill(lens, 0x55DDF1FF)
r.stroke(lens, INK, 9)
shine = r.shape(glass, name="Shine", rotation=-0.7)
r.rect(shine, 8, 22, radius=4, x=-14, y=-8)
r.fill(shine, 0xCCFFFFFF)
handle = r.shape(glass, name="Handle", rotation=-0.785)
r.rect(handle, 14, 40, radius=7, x=0, y=46)
r.fill(handle, INK)

ring = r.shape(root, name="Ring", opacity=0.0)
r.ellipse(ring, 180, 180)
ring_stroke, _ = r.stroke(ring, 0xAAFFFFFF, 5, cap=1)
r.trim(ring_stroke, 0.3)

zero = lambda k, *objs: [k(o, p, [(0, v)]) for o in objs for p, v in (("scaleX", 0), ("scaleY", 0))]

# 0 nothing.
_, k = r.animation("hidden", 10)
k(glass, "opacity", [(0, 0)]); k(ring, "opacity", [(0, 0)]); zero(k, ok, doubt, trap)

# 1 looking: the glass circles, the ring turns.
_, k = r.animation("looking", 120)
pts = [(f, 46 * math.cos(2 * math.pi * f / 120), 46 * math.sin(2 * math.pi * f / 120)) for f in range(0, 121, 10)]
k(glass, "x", [(f, x) for f, x, _ in pts]); k(glass, "y", [(f, y) for f, _, y in pts])
k(glass, "opacity", [(0, 1)]); k(ring, "opacity", [(0, 1)])
k(ring, "rotation", [(0, 0), (120, 2 * math.pi)])
k(glass, "scaleX", [(0, 1, ease), (60, 1.12, ease), (120, 1)]); k(glass, "scaleY", [(0, 1, ease), (60, 1.12, ease), (120, 1)])
zero(k, ok, doubt, trap)

# 2-4 the verdict: the glass goes, the badge pops.
for name, b in (("fine", ok), ("doubt", doubt), ("trap", trap)):
    _, k = r.animation(name, 50, loop=0)
    k(glass, "opacity", [(0, 1, out), (12, 0)]); k(ring, "opacity", [(0, 1, out), (12, 0)])
    k(b, "scaleX", [(0, 0, "hold"), (6, 0, pop), (30, 1)]); k(b, "scaleY", [(0, 0, "hold"), (6, 0, pop), (30, 1)])
    zero(k, *[o for o in (ok, doubt, trap) if o != b])
    if b == trap:
        k(trap, "rotation", [(30, 0), (34, 0.12), (38, -0.12), (42, 0.06), (46, 0)])

m = Machine("Scan")
state = m.number("state", 0)
r.save(sys.argv[1], m.objects([
    ("entry", None, [(3, [], 0)]),
    ("any", None, [(3 + n, [(state, 0, n)], 0) for n in range(5)]),
    ("exit", None, []),
] + [("anim", n, []) for n in range(5)]))
