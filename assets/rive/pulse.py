"""rive_pulse.riv: the dot of the "Protecting you" chip. Input "on": 1 protecting (a green dot sending out sonar
waves), 0 something to set up (an amber dot blinking slowly). What comes first in the file is drawn on top."""
import sys
from rivwriter import Riv, Machine

GREEN, AMBER = 0xFF7CC242, 0xFFE59A00

r = Riv("Pulse", 48, 48)
ease = r.cubic(0.42, 0, 0.58, 1)
root = r.node(0, 24, 24, name="Pulse")

core = r.shape(root, name="Core")
r.ellipse(core, 14, 14)
_, core_color = r.fill(core, GREEN)

rings = []
for i in range(2):
    ring = r.shape(root, name=f"Ring{i}", opacity=0.0)
    r.ellipse(ring, 22, 22)
    r.fill(ring, GREEN)
    rings.append(ring)

# 1 protecting: two rings go out one after the other, the dot breathes.
_, k = r.animation("on", 120)
k(core_color, "color", [(0, GREEN)], color=True)
k(core, "opacity", [(0, 1)])
k(core, "scaleX", [(0, 1, ease), (60, 1.12, ease), (120, 1)]); k(core, "scaleY", [(0, 1, ease), (60, 1.12, ease), (120, 1)])
first, second = rings
k(first, "scaleX", [(0, 0.6), (90, 2.1, "hold"), (120, 2.1)]); k(first, "scaleY", [(0, 0.6), (90, 2.1, "hold"), (120, 2.1)])
k(first, "opacity", [(0, 0.6), (90, 0, "hold"), (120, 0)])
# The second, half a beat behind: it carries on across the loop's seam.
k(second, "scaleX", [(0, 1.6), (30, 2.1, "hold"), (60, 0.6), (120, 1.6)])
k(second, "scaleY", [(0, 1.6), (30, 2.1, "hold"), (60, 0.6), (120, 1.6)])
k(second, "opacity", [(0, 0.2), (30, 0, "hold"), (60, 0.6), (120, 0.2)])

# 0 something to set up: amber, blinking slowly, no rings.
_, k = r.animation("off", 120)
k(core_color, "color", [(0, AMBER)], color=True)
k(core, "opacity", [(0, 1, ease), (60, 0.35, ease), (120, 1)])
k(core, "scaleX", [(0, 1)]); k(core, "scaleY", [(0, 1)])
for ring in rings:
    k(ring, "opacity", [(0, 0)])

m = Machine("Pulse")
on = m.number("on", 1)
# Layer states: 0 entry, 1 any, 2 exit, 3 on, 4 off.
r.save(sys.argv[1], m.objects([
    ("entry", None, [(3, [], 0)]),
    ("any", None, [(3, [(on, 0, 1)], 300), (4, [(on, 0, 0)], 300)]),
    ("exit", None, []),
    ("anim", 0, []), ("anim", 1, []),
]))
