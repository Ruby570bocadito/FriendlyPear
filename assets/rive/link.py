"""rive_link.riv: pairing this phone with the family's server. A phone on the left, Peri's shield on the right, dots
between them. Input "state": 0 waiting (the dots faint), 1 connecting (the dots run from the phone to the shield),
2 connected (the line turns green, the shield lights up and hops, hearts rise), 3 it failed (the line breaks, amber).
What comes first in the file is drawn on top."""
import sys
from rivwriter import Riv, Machine
from tabs import path_data

GREEN, GREEN_DARK, AMBER, LINE, CORAL = 0xFF7CC242, 0xFF25541A, 0xFFE59A00, 0xFFD8CCB2, 0xFFFF8A7A
DOTS = 7

r = Riv("Link", 320, 110)
ease = r.cubic(0.42, 0, 0.58, 1)
out = r.cubic(0.2, 0.8, 0.3, 1)
pop = r.cubic(0.34, 1.56, 0.64, 1)
root = r.node(0, 0, 0, name="Link")

# Two hearts above the line, on top of everything.
hearts = []
for i, x in enumerate((140, 180)):
    h = r.shape(root, name=f"Heart{i}", x=x, y=55, opacity=0.0, scaleX=0.0, scaleY=0.0)
    r.svg(h, "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 "
             "14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z", 0.9, (12, 12))
    r.fill(h, CORAL)
    hearts.append(h)
# The broken link's mark, in the middle.
cross = r.shape(root, name="Cross", x=160, y=55, opacity=0.0, scaleX=0.0, scaleY=0.0)
r.path(cross, [(-8, -8), (8, 8)], closed=False)
r.path(cross, [(8, -8), (-8, 8)], closed=False)
r.stroke(cross, AMBER, 5, cap=1)

# The phone: a rounded body with its screen and a little pear dot.
phone = r.node(root, name="Phone", x=44, y=55)
screen = r.shape(phone)
r.rect(screen, 28, 42, radius=4, y=-2)
r.fill(screen, 0xFFE1F0CF)
body = r.shape(phone)
r.rect(body, 38, 62, radius=9)
r.fill(body, GREEN_DARK)
# Peri's shield, from the app's own icon.
shield = r.node(root, name="Shield", x=276, y=55)
s = r.shape(shield)
r.svg(s, path_data("ic_shield"), 2.6, (12, 12))
_, shield_colour = r.fill(s, GREEN_DARK)

dots = []
for i in range(DOTS):
    d = r.shape(root, name=f"Dot{i}", x=84 + i * (152 / (DOTS - 1)), y=55)
    r.ellipse(d, 10, 10)
    _, colour = r.fill(d, LINE)
    dots.append((d, colour))


def everything(k, dot_colour, dot_opacity, hearts_on=0, cross_on=0):
    for d, c in dots:
        k(c, "color", [(0, dot_colour)], color=True)
        k(d, "opacity", [(0, dot_opacity)]); k(d, "scaleX", [(0, 1)]); k(d, "scaleY", [(0, 1)]); k(d, "y", [(0, 55)])
    k(cross, "scaleX", [(0, cross_on)]); k(cross, "scaleY", [(0, cross_on)])
    k(shield_colour, "color", [(0, GREEN_DARK)], color=True)
    k(cross, "opacity", [(0, cross_on)])
    for h in hearts:
        k(h, "opacity", [(0, hearts_on)])


# 0 waiting: faint dots; the phone breathes.
_, k = r.animation("waiting", 120)
everything(k, LINE, 0.6)
k(phone, "scaleX", [(0, 1, ease), (60, 1.04, ease), (120, 1)]); k(phone, "scaleY", [(0, 1, ease), (60, 1.04, ease), (120, 1)])

# 1 connecting: a wave runs along the dots, from the phone to the shield.
_, k = r.animation("connecting", 60)
for d, c in dots:
    k(c, "color", [(0, GREEN)], color=True)
    k(d, "y", [(0, 55)])  # back in line after a failed try
for i, (d, _) in enumerate(dots):
    t = i * 6
    k(d, "opacity", [(0, 0.35, "hold"), (t, 0.35, ease), (t + 8, 1, ease), (t + 20, 0.35, "hold"), (60, 0.35)])
    k(d, "scaleX", [(0, 0.8, "hold"), (t, 0.8, ease), (t + 8, 1.4, ease), (t + 20, 0.8, "hold"), (60, 0.8)])
    k(d, "scaleY", [(0, 0.8, "hold"), (t, 0.8, ease), (t + 8, 1.4, ease), (t + 20, 0.8, "hold"), (60, 0.8)])
k(cross, "scaleX", [(0, 0)]); k(cross, "scaleY", [(0, 0)])
k(shield_colour, "color", [(0, GREEN_DARK)], color=True)
for h in hearts:
    k(h, "opacity", [(0, 0)])
k(phone, "scaleX", [(0, 1)]); k(phone, "scaleY", [(0, 1)])

# 2 connected: the dots fill in green one after another, the shield lights up, two hearts rise and fade.
_, k = r.animation("connected", 100, loop=0)
for i, (d, c) in enumerate(dots):
    t = i * 3
    k(c, "color", [(0, GREEN)], color=True)
    k(d, "y", [(0, 55)])
    k(d, "opacity", [(0, 0.35, "hold"), (t, 0.35, out), (t + 8, 1)])
    k(d, "scaleX", [(0, 0.8, "hold"), (t, 0.8, pop), (t + 14, 1.15)]); k(d, "scaleY", [(0, 0.8, "hold"), (t, 0.8, pop), (t + 14, 1.15)])
k(shield_colour, "color", [(0, GREEN_DARK, "hold"), (20, GREEN_DARK, "linear"), (30, GREEN)], color=True)
k(shield, "scaleX", [(0, 1, "hold"), (20, 1, pop), (28, 1.12, ease), (40, 1)]); k(shield, "scaleY", [(0, 1, "hold"), (20, 1, pop), (28, 1.12, ease), (40, 1)])
for n, h in enumerate(hearts):
    t = 30 + n * 10
    k(h, "opacity", [(0, 0, "hold"), (t, 1, "linear"), (t + 40, 1, "linear"), (t + 60, 0)])
    k(h, "scaleX", [(0, 0, "hold"), (t, 0, pop), (t + 14, 1)]); k(h, "scaleY", [(0, 0, "hold"), (t, 0, pop), (t + 14, 1)])
    k(h, "y", [(0, 55, "hold"), (t, 55, out), (t + 60, 18)])
k(cross, "scaleX", [(0, 0)]); k(cross, "scaleY", [(0, 0)]); k(cross, "opacity", [(0, 0)])
k(phone, "scaleX", [(0, 1)]); k(phone, "scaleY", [(0, 1)])

# 3 failed: the dots in the middle drop away, the rest turn amber, a cross pops where the link broke.
_, k = r.animation("failed", 60, loop=0)
for i, (d, c) in enumerate(dots):
    k(c, "color", [(0, AMBER)], color=True)
    middle = abs(i - (DOTS - 1) / 2) < 1.5
    k(d, "opacity", [(0, 1, out), (24, 0 if middle else 0.8)])
    k(d, "y", [(0, 55, out), (24, 80 if middle else 55)])
    k(d, "scaleX", [(0, 1)]); k(d, "scaleY", [(0, 1)])
k(cross, "opacity", [(0, 0, "hold"), (14, 1)])
k(cross, "scaleX", [(0, 0, "hold"), (14, 0, pop), (32, 1)]); k(cross, "scaleY", [(0, 0, "hold"), (14, 0, pop), (32, 1)])
k(cross, "rotation", [(14, -0.6, out), (32, 0)])
k(shield_colour, "color", [(0, GREEN_DARK)], color=True)
for h in hearts:
    k(h, "opacity", [(0, 0)])
k(phone, "scaleX", [(0, 1)]); k(phone, "scaleY", [(0, 1)])

m = Machine("Link")
state = m.number("state", 0)
r.save(sys.argv[1], m.objects([
    ("entry", None, [(3, [], 0)]),
    ("any", None, [(3 + n, [(state, 0, n)], 200) for n in range(4)]),
    ("exit", None, []),
] + [("anim", n, []) for n in range(4)]))
