"""rive_burst.riv: a celebration behind Peri. Sun rays fade in and turn slowly for as long as it shows; confetti bursts
out once at the start. No input: it plays as it appears. Two layers, at the same time: the rays, and the confetti.
What comes first in the file is drawn on top."""
import math, random, sys
from rivwriter import Riv, Machine

SUN, GREEN, CORAL, SKY, LILAC = 0xFFFFC83D, 0xFF7CC242, 0xFFFF8A7A, 0xFF6EC6FF, 0xFFA98BFF
COLORS = [SUN, GREEN, CORAL, SKY, LILAC]

r = Riv("Burst", 240, 240)
ease = r.cubic(0.42, 0, 0.58, 1)
out = r.cubic(0.15, 0.85, 0.35, 1)
root = r.node(0, 120, 120, name="Burst")

rng = random.Random(7)
confetti = []
for i in range(20):
    a = i * 2 * math.pi / 20 + rng.uniform(-0.15, 0.15)
    reach = rng.uniform(72, 112)
    piece = r.shape(root, name=f"Confetti{i}", opacity=0.0, scaleX=0.0, scaleY=0.0)
    if i % 3 == 0:
        r.ellipse(piece, 9, 9)
    else:
        r.rect(piece, 7, 13, radius=2)
    r.fill(piece, COLORS[i % len(COLORS)])
    confetti.append((piece, a, reach, rng.uniform(-6, 6)))

rays = r.shape(root, name="Rays", opacity=0.0)
for i in range(12):
    a, d = i * math.pi / 6, math.radians(7)
    r.path(rays, [(0, 0), (118 * math.cos(a - d), 118 * math.sin(a - d)), (118 * math.cos(a + d), 118 * math.sin(a + d))])
r.radial(rays, 118, [(0, 0x99FFC83D), (0.35, 0x66FFC83D), (1, 0x00FFC83D)])
glow = r.shape(root, name="Glow", opacity=0.0)
r.ellipse(glow, 150, 150)
r.radial(glow, 75, [(0, 0xCCFFF1C9), (1, 0x00FFF1C9)])

# Layer 1: the rays fade in, then turn a twelfth of a turn per loop (the same picture again) and breathe.
_, k = r.animation("rays_in", 30, loop=0)
k(rays, "opacity", [(0, 0, out), (30, 1)]); k(glow, "opacity", [(0, 0, out), (30, 1)])
k(rays, "scaleX", [(0, 0.6, out), (30, 0.95)]); k(rays, "scaleY", [(0, 0.6, out), (30, 0.95)])
_, k = r.animation("rays", 480)
k(rays, "opacity", [(0, 1)]); k(glow, "opacity", [(0, 1)])
k(rays, "rotation", [(0, 0), (480, math.pi / 6)])
k(rays, "scaleX", [(0, 0.95, ease), (240, 1.05, ease), (480, 0.95)]); k(rays, "scaleY", [(0, 0.95, ease), (240, 1.05, ease), (480, 0.95)])

# Layer 2: the confetti, once: out from behind Peri, then drifting down as it fades.
_, k = r.animation("confetti", 110, loop=0)
for piece, a, reach, spin in confetti:
    x, y = reach * math.cos(a), reach * math.sin(a)
    k(piece, "x", [(0, 0, out), (40, x, "linear"), (110, x * 1.08)])
    k(piece, "y", [(0, 0, out), (40, y, "linear"), (110, y + 26)])
    k(piece, "scaleX", [(0, 0, out), (14, 1)]); k(piece, "scaleY", [(0, 0, out), (14, 1)])
    k(piece, "rotation", [(0, 0), (110, spin)])
    k(piece, "opacity", [(0, 1, "hold"), (70, 1, "linear"), (110, 0)])

m = Machine("Burst")
# Each layer: 0 entry, 1 any, 2 exit, then its animations.
r.save(sys.argv[1], m.objects(
    [("entry", None, [(3, [], 0)]), ("any", None, []), ("exit", None, []),
     ("anim", 0, [(4, [], 0, 500)]), ("anim", 1, [])],
    [("entry", None, [(3, [], 0)]), ("any", None, []), ("exit", None, []), ("anim", 2, [])],
))
