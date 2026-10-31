"""Two of the app's own vector icons (res/drawable), alive. Usage: icons.py <dir>. What comes first in the file is
drawn on top.

rive_voice.riv: "Talk to Peri"'s microphone, white on the green button. Input "on": 1 while Peri listens (the
microphone gives way to five sound bars bouncing), 0 the microphone.
rive_glint.riv: the shield of the shields chip, forest green, that glints now and then. No input.
rive_send.riv: the chat's send arrow, white: it flies off to the right and comes back in from the left each time
"sent" changes between 1 and 2 (flipped on every message; 0 at rest).
rive_next.riv: the welcome's "Next" arrow, white: it nudges to the right every two seconds. No input."""
import math, os, sys
from rivwriter import Riv, Machine
from tabs import S, path_data

WHITE, GREEN_DARK, SUN = 0xFFFFFFFF, 0xFF25541A, 0xFFFFC83D


def voice(out):
    r = Riv("Voice", 64, 64)
    ease = r.cubic(0.42, 0, 0.58, 1)
    root = r.node(0, 32, 32, name="Voice")
    bars = []
    for i in range(5):
        bar = r.node(root, name=f"Bar{i}", x=(i - 2) * 11, scaleY=0.0)
        s = r.shape(bar)
        r.rect(s, 7, 46, radius=3.5)
        r.fill(s, WHITE)
        bars.append(bar)
    mic = r.shape(root, name="Mic")
    r.svg(mic, path_data("ic_mic"), S, (12, 12))
    r.fill(mic, WHITE)

    _, k = r.animation("mic", 12, loop=0)
    k(mic, "opacity", [(0, 1)]); k(mic, "scaleX", [(0, 1)]); k(mic, "scaleY", [(0, 1)])
    for bar in bars:
        k(bar, "scaleY", [(0, 0)])

    # Listening: the bars rise and fall, each its own heights and beat, like a voice; each loop ends where it began.
    _, k = r.animation("listening", 48)
    k(mic, "opacity", [(0, 0)]); k(mic, "scaleX", [(0, 0.6)]); k(mic, "scaleY", [(0, 0.6)])
    heights = [
        [0.3, 0.45, 0.25, 0.4, 0.3, 0.45, 0.3],
        [0.6, 0.3, 0.8, 0.4, 0.7, 0.35, 0.6],
        [1.0, 0.45, 0.85, 0.3, 0.95, 0.5, 1.0],
        [0.4, 0.75, 0.3, 0.7, 0.45, 0.8, 0.4],
        [0.45, 0.25, 0.5, 0.3, 0.4, 0.25, 0.45],
    ]
    for bar, hs in zip(bars, heights):
        k(bar, "scaleY", [(f * 8, h, ease) for f, h in enumerate(hs)])
    m = Machine("Voice")
    on = m.number("on", 0)
    r.save(os.path.join(out, "rive_voice.riv"), m.objects([
        ("entry", None, [(3, [], 0)]),
        ("any", None, [(3, [(on, 0, 0)], 150), (4, [(on, 0, 1)], 150)]),
        ("exit", None, []),
        ("anim", 0, []), ("anim", 1, []),
    ]))


def glint(out):
    r = Riv("Glint", 64, 64)
    pop = r.cubic(0.34, 1.56, 0.64, 1)
    ease = r.cubic(0.42, 0, 0.58, 1)
    root = r.node(0, 32, 33, name="Glint")
    star = r.shape(root, name="Star", x=18, y=-18, scaleX=0.0, scaleY=0.0)
    pts = []
    for i in range(8):
        a = -math.pi / 2 + i * math.pi / 4
        rad = 14 if i % 2 == 0 else 3.6
        pts.append((rad * math.cos(a), rad * math.sin(a)))
    r.path(star, pts)
    r.fill(star, SUN)
    shield = r.shape(root, name="Shield")
    r.svg(shield, path_data("ic_shield"), S * 0.85, (12, 12))
    r.fill(shield, GREEN_DARK)
    # Every four seconds: the shield nods, and a star twinkles at its shoulder.
    _, k = r.animation("glint", 240)
    k(star, "scaleX", [(0, 0, "hold"), (150, 0, pop), (166, 1, ease), (190, 0, "hold"), (240, 0)])
    k(star, "scaleY", [(0, 0, "hold"), (150, 0, pop), (166, 1, ease), (190, 0, "hold"), (240, 0)])
    k(star, "rotation", [(150, 0), (190, math.pi / 2)])
    k(shield, "rotation", [(0, 0, "hold"), (146, 0, ease), (154, -0.12, ease), (164, 0.06, ease), (174, 0)])
    k(shield, "scaleX", [(0, 1, "hold"), (146, 1, ease), (156, 1.08, ease), (172, 1)])
    k(shield, "scaleY", [(0, 1, "hold"), (146, 1, ease), (156, 1.08, ease), (172, 1)])
    m = Machine("Glint")
    r.save(os.path.join(out, "rive_glint.riv"), m.objects([
        ("entry", None, [(3, [], 0)]), ("any", None, []), ("exit", None, []), ("anim", 0, []),
    ]))


def send(out):
    r = Riv("Send", 64, 64)
    away = r.cubic(0.55, 0, 0.9, 0.4)
    back = r.cubic(0.2, 0.8, 0.3, 1)
    root = r.node(0, 32, 32, name="Send")
    arrow = r.shape(root, name="Arrow")
    r.svg(arrow, path_data("ic_chevron"), S, (12, 12))
    r.fill(arrow, WHITE)
    _, k = r.animation("rest", 10, loop=0)
    k(arrow, "x", [(0, 0)]); k(arrow, "opacity", [(0, 1)])
    _, k = r.animation("fly", 34, loop=0)
    k(arrow, "x", [(0, 0, away), (12, 44, "hold"), (13, -40, back), (34, 0)])
    k(arrow, "opacity", [(0, 1, "linear"), (10, 0, "hold"), (14, 0, "linear"), (24, 1)])
    m = Machine("Send")
    sent = m.number("sent", 0)
    # Two states for the same flight, one for each value: flipping the number always plays it again.
    r.save(os.path.join(out, "rive_send.riv"), m.objects([
        ("entry", None, [(3, [], 0)]),
        ("any", None, [(4, [(sent, 0, 1)], 0), (5, [(sent, 0, 2)], 0)]),
        ("exit", None, []),
        ("anim", 0, []), ("anim", 1, []), ("anim", 1, []),
    ]))


def nudge(out):
    r = Riv("Next", 64, 64)
    ease = r.cubic(0.42, 0, 0.58, 1)
    root = r.node(0, 32, 32, name="Next")
    arrow = r.shape(root, name="Arrow")
    r.svg(arrow, path_data("ic_chevron"), S, (12, 12))
    r.fill(arrow, WHITE)
    _, k = r.animation("nudge", 120)
    k(arrow, "x", [(0, 0, "hold"), (84, 0, ease), (94, 9, ease), (102, 0, ease), (110, 6, ease), (120, 0)])
    m = Machine("Next")
    r.save(os.path.join(out, "rive_next.riv"), m.objects([
        ("entry", None, [(3, [], 0)]), ("any", None, []), ("exit", None, []), ("anim", 0, []),
    ]))


if __name__ == "__main__":
    voice(sys.argv[1])
    glint(sys.argv[1])
    send(sys.argv[1])
    nudge(sys.argv[1])
