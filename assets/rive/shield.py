"""rive_shield.riv (what comes first in the file is drawn on top): Peri's shield. Input "level": 0 watching (breathes), 1 danger (red, "!", shakes), 2 safe (green, a
check draws itself, sparks fly out)."""
import math, sys
from rivwriter import Riv, Machine

GREEN, GREEN_DARK, RED, WHITE = 0xFF7CC242, 0xFF25541A, 0xFFC0261C, 0xFFFFFFFF
GLOW_GREEN, GLOW_RED = 0x557CC242, 0x55E5483C

r = Riv("Shield", 240, 240)
ease = r.cubic(0.42, 0, 0.58, 1)
out = r.cubic(0.2, 0.8, 0.3, 1)
pop = r.cubic(0.34, 1.56, 0.64, 1)

root = r.node(0, 120, 124, name="Shield")
bang = r.shape(root, name="Bang", opacity=0.0)
r.rect(bang, 16, 52, radius=8, y=-14)
r.ellipse(bang, 18, 18, y=34)
r.fill(bang, WHITE)

check = r.shape(root, name="Check")
r.path(check, [(-30, 2), (-8, 26), (34, -22)], closed=False)
check_stroke, _ = r.stroke(check, WHITE, 16, cap=1, join=1)
check_trim = r.trim(check_stroke, 0)

inner = r.shape(root, name="Inner", opacity=0.25)
r.path(inner, [(0, -66, 14), (52, -48, 10), (49, 4, 30), (0, 64, 20), (-49, 4, 30), (-52, -48, 10)])
r.fill(inner, WHITE)

body = r.shape(root, name="Body")
outline = [(0, -86, 18), (70, -62, 14), (66, 8, 40), (0, 86, 26), (-66, 8, 40), (-70, -62, 14)]
r.path(body, outline)
_, body_color = r.fill(body, GREEN_DARK)
r.stroke(body, WHITE, 8, join=1)

sparks = []
for i in range(8):
    a = i * math.pi / 4
    s = r.shape(root, name=f"Spark{i}", opacity=0.0)
    r.ellipse(s, 14, 14)
    r.fill(s, 0xFFFFC94A if i % 2 else GREEN)
    sparks.append((s, math.cos(a), math.sin(a)))

glow = r.shape(root, name="Glow")
r.ellipse(glow, 200, 200)
_, glow_color = r.fill(glow, GLOW_GREEN)

# 0 watching: breathes gently.
_, k = r.animation("watching", 120)
k(body, "scaleX", [(0, 1, ease), (60, 1.04, ease), (120, 1)]); k(body, "scaleY", [(0, 1, ease), (60, 1.04, ease), (120, 1)])
k(glow, "scaleX", [(0, 0.9, ease), (60, 1.05, ease), (120, 0.9)]); k(glow, "scaleY", [(0, 0.9, ease), (60, 1.05, ease), (120, 0.9)])
k(glow, "opacity", [(0, 0.6, ease), (60, 1, ease), (120, 0.6)])
k(body_color, "color", [(0, GREEN_DARK)], color=True); k(glow_color, "color", [(0, GLOW_GREEN)], color=True)
k(check_trim, "trimEnd", [(0, 0)]); k(bang, "opacity", [(0, 0)]); k(root, "rotation", [(0, 0)])

# 1 danger: red, an exclamation mark, a quick shake and a pulse.
_, k = r.animation("danger", 60)
k(body_color, "color", [(0, RED)], color=True); k(glow_color, "color", [(0, GLOW_RED)], color=True)
k(bang, "opacity", [(0, 1)]); k(check_trim, "trimEnd", [(0, 0)])
k(root, "rotation", [(0, 0), (4, 0.09), (8, -0.09), (12, 0.06), (16, -0.06), (20, 0), (60, 0)])
k(body, "scaleX", [(0, 1, ease), (30, 1.08, ease), (60, 1)]); k(body, "scaleY", [(0, 1, ease), (30, 1.08, ease), (60, 1)])
k(glow, "scaleX", [(0, 0.95, ease), (30, 1.15, ease), (60, 0.95)]); k(glow, "scaleY", [(0, 0.95, ease), (30, 1.15, ease), (60, 0.95)])
k(glow, "opacity", [(0, 1)])

# 2 safe: it pops back green, the check draws itself, sparks fly out.
_, k = r.animation("safe", 80, loop=0)
k(body_color, "color", [(0, RED, out), (14, GREEN)], color=True); k(glow_color, "color", [(0, GLOW_GREEN)], color=True)
k(bang, "opacity", [(0, 0)]); k(root, "rotation", [(0, 0)])
k(body, "scaleX", [(0, 0.85, pop), (22, 1)]); k(body, "scaleY", [(0, 0.85, pop), (22, 1)])
k(check_trim, "trimEnd", [(0, 0, "hold"), (10, 0, out), (34, 1)])
k(glow, "scaleX", [(0, 0.8, out), (40, 1.1)]); k(glow, "scaleY", [(0, 0.8, out), (40, 1.1)]); k(glow, "opacity", [(0, 1)])
for s, cx, cy in sparks:
    k(s, "x", [(0, cx * 50, "hold"), (14, cx * 50, out), (60, cx * 112)])
    k(s, "y", [(0, cy * 50, "hold"), (14, cy * 50, out), (60, cy * 112)])
    k(s, "opacity", [(0, 0, "hold"), (14, 1, "linear"), (44, 1, "linear"), (70, 0)])
    k(s, "scaleX", [(14, 1.2, "linear"), (70, 0.4)]); k(s, "scaleY", [(14, 1.2, "linear"), (70, 0.4)])

m = Machine("Shield")
level = m.number("level", 0)
# Layer states: 0 entry, 1 any, 2 exit, 3 watching, 4 danger, 5 safe.
r.save(sys.argv[1], m.objects([
    ("entry", None, [(3, [], 0)]),
    ("any", None, [(3, [(level, 0, 0)], 250), (4, [(level, 0, 1)], 150), (5, [(level, 0, 2)], 120)]),
    ("exit", None, []),
    ("anim", 0, []), ("anim", 1, []), ("anim", 2, []),
]))
