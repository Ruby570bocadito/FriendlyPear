"""rive_tab_*.riv: the tab bar's four icons, drawn from the app's own vector icons (res/drawable/ic_*.xml), each with a
little moment when its tab is picked. Input "on": 1 picked (its section's colour), 0 not (grey). Usage: tabs.py <dir>.
What comes first in the file is drawn on top."""
import math, os, re, sys
from rivwriter import Riv, Machine

INK_SOFT, SUN, SUN_LIP, CORAL = 0xFF3F4D39, 0xFFFFC83D, 0xFFDDA52A, 0xFFFF8A7A
DRAWABLE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "android", "app", "src", "main", "res", "drawable")
S = 2.4  # 24 viewport units to a 64 x 64 artboard, a little margin around


def path_data(icon):
    return re.search(r'pathData="([^"]+)"', open(os.path.join(DRAWABLE, f"{icon}.xml"), encoding="utf8").read()).group(1)


def subpaths(d):
    """pathData split where each subpath starts (an M after a z), to give each its own shape."""
    return [p if p[0] in "Mm" else "M" + p for p in re.split(r"(?<=[zZ])(?=[Mm])", d)]


def machine():
    """Two animations: 0 "off", 1 "on"; picked when "on" is 1."""
    m = Machine("Tab")
    on = m.number("on", 0)
    return m.objects([
        ("entry", None, [(3, [], 0)]),
        ("any", None, [(3, [(on, 0, 0)], 160), (4, [(on, 0, 1)], 0)]),
        ("exit", None, []),
        ("anim", 0, []), ("anim", 1, []),
    ])


def colours(k, paints, ink, on):
    for p in paints:
        k(p, "color", [(0, INK_SOFT, "linear"), (8, ink)] if on else [(0, INK_SOFT)], color=True)


def sparkles(r, root, at, count, colour):
    out = []
    for i in range(count):
        a = -math.pi / 2 + (i - (count - 1) / 2) * 0.8
        s = r.shape(root, name=f"Sparkle{i}", opacity=0.0, x=at[0], y=at[1])
        r.ellipse(s, 6, 6)
        r.fill(s, colour)
        out.append((s, math.cos(a), math.sin(a)))
    return out


def burst(k, dots, at, start, reach=14):
    for s, cx, cy in dots:
        k(s, "x", [(0, at[0], "hold"), (start, at[0], "linear"), (start + 22, at[0] + cx * reach)])
        k(s, "y", [(0, at[1], "hold"), (start, at[1], "linear"), (start + 22, at[1] + cy * reach)])
        k(s, "opacity", [(0, 0, "hold"), (start, 1, "linear"), (start + 14, 1, "linear"), (start + 26, 0)])


def hidden(k, dots):
    for s, _, _ in dots:
        k(s, "opacity", [(0, 0)])


def pear(out):
    """Peri's tab: the pear wobbles on its base, squashes and stretches, and a few sparkles fly off its leaf."""
    ink = 0xFF25541A
    r = Riv("Pear", 64, 64)
    out_ease = r.cubic(0.2, 0.8, 0.3, 1)
    root = r.node(0, 32, 32 + 9.5 * S, name="Base")  # it wobbles on its bottom
    dots = sparkles(r, root, (12, -14), 3, SUN)
    body = r.shape(root, name="Pear")
    r.svg(body, path_data("ic_pear"), S, (12, 21.5))
    _, paint = r.fill(body, INK_SOFT)
    _, k = r.animation("off", 10, loop=0)
    colours(k, [paint], ink, False); k(body, "rotation", [(0, 0)]); k(body, "scaleY", [(0, 1)]); k(body, "scaleX", [(0, 1)]); hidden(k, dots)
    _, k = r.animation("on", 60, loop=0)
    colours(k, [paint], ink, True)
    k(body, "rotation", [(0, 0), (8, 0.28), (18, -0.2), (28, 0.11), (38, -0.04), (48, 0)])
    k(body, "scaleY", [(0, 0.82, out_ease), (14, 1.08, out_ease), (26, 1)])
    k(body, "scaleX", [(0, 1.12, out_ease), (14, 0.95, out_ease), (26, 1)])
    burst(k, dots, (12, -14), 6)
    r.save(os.path.join(out, "rive_tab_peri.riv"), machine())


def diary(out):
    """The diary's tab: the book opens out from its spine, and a sparkle rises from its pages."""
    ink = 0xFF6B3E00
    r = Riv("Diary", 64, 64)
    pop = r.cubic(0.34, 1.56, 0.64, 1)
    root = r.node(0, 32, 32, name="Book")
    dots = sparkles(r, root, (0, -12), 3, SUN_LIP)
    book = r.shape(root, name="Pages")
    r.svg(book, path_data("ic_diary"), S, (12, 12.5))
    _, paint = r.fill(book, INK_SOFT)
    _, k = r.animation("off", 10, loop=0)
    colours(k, [paint], ink, False); k(book, "scaleX", [(0, 1)]); k(book, "y", [(0, 0)]); hidden(k, dots)
    _, k = r.animation("on", 60, loop=0)
    colours(k, [paint], ink, True)
    k(book, "scaleX", [(0, 0.15, pop), (22, 1)])
    k(book, "y", [(0, 0), (8, -3), (22, 0)])
    burst(k, dots, (0, -16), 14, 12)
    r.save(os.path.join(out, "rive_tab_diary.riv"), machine())


def learn(out):
    """Learn's tab: the bulb lights up, its rays pop out and stay while the tab is picked."""
    ink = 0xFF1E4A7A
    r = Riv("Learn", 64, 64)
    pop = r.cubic(0.34, 1.56, 0.64, 1)
    root = r.node(0, 32, 32, name="Bulb")
    # Around the glass (its centre 3 units above the icon's, scaled 0.86), inside the artboard.
    rays = r.shape(root, name="Rays", y=-3 * 0.86 * S, opacity=0.0)
    for i in range(5):
        a = math.pi + i * math.pi / 4
        r.path(rays, [(math.cos(a) * 17, math.sin(a) * 17), (math.cos(a) * 23, math.sin(a) * 23)], closed=False)
    r.stroke(rays, SUN_LIP, 2.6, cap=1)
    bulb = r.shape(root, name="Glass")
    r.svg(bulb, path_data("ic_learn"), S * 0.86, (12, 12))
    _, paint = r.fill(bulb, INK_SOFT)
    _, k = r.animation("off", 10, loop=0)
    colours(k, [paint], ink, False); k(rays, "opacity", [(0, 0)]); k(bulb, "scaleX", [(0, 1)]); k(bulb, "scaleY", [(0, 1)])
    _, k = r.animation("on", 60, loop=0)
    colours(k, [paint], ink, True)
    k(bulb, "scaleX", [(0, 1, "linear"), (6, 1.15, pop), (20, 1)]); k(bulb, "scaleY", [(0, 1, "linear"), (6, 1.15, pop), (20, 1)])
    k(rays, "opacity", [(0, 0, "hold"), (6, 1)])
    k(rays, "scaleX", [(0, 0.5, "hold"), (6, 0.5, pop), (26, 1)]); k(rays, "scaleY", [(0, 0.5, "hold"), (6, 0.5, pop), (26, 1)])
    r.save(os.path.join(out, "rive_tab_learn.riv"), machine())


def family(out):
    """The family's tab: the two hop one after the other, and a heart pops up between them."""
    ink = 0xFF4A2F99
    r = Riv("Family", 64, 64)
    out_ease = r.cubic(0.2, 0.8, 0.3, 1)
    pop = r.cubic(0.34, 1.56, 0.64, 1)
    root = r.node(0, 32, 34, name="Family")
    heart = r.shape(root, name="Heart", y=-11 * S, opacity=0.0, scaleX=0.0, scaleY=0.0)
    r.svg(heart, "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 "
                 "14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z", 0.5, (12, 12))
    r.fill(heart, CORAL)
    right_head, left_head, left_body, right_body = subpaths(path_data("ic_family"))
    paints, people = [], []
    for name, head, body in (("Left", left_head, left_body), ("Right", right_head, right_body)):
        person = r.node(root, name=name)
        for part in (head, body):
            s = r.shape(person)
            r.svg(s, part, S, (12, 12))
            paints.append(r.fill(s, INK_SOFT)[1])
        people.append(person)
    _, k = r.animation("off", 10, loop=0)
    colours(k, paints, ink, False)
    for p in people:
        k(p, "y", [(0, 0)])
    k(heart, "opacity", [(0, 0)])
    _, k = r.animation("on", 70, loop=0)
    colours(k, paints, ink, True)
    for n, p in enumerate(people):
        t = n * 8
        k(p, "y", [(0, 0, "hold"), (t, 0, out_ease), (t + 8, -7, "linear"), (t + 10, -7, out_ease), (t + 22, 0)])
    k(heart, "opacity", [(0, 0, "hold"), (12, 1, "linear"), (44, 1, "linear"), (62, 0)])
    k(heart, "scaleX", [(0, 0, "hold"), (12, 0, pop), (28, 1)]); k(heart, "scaleY", [(0, 0, "hold"), (12, 0, pop), (28, 1)])
    k(heart, "y", [(0, -11 * S, "hold"), (12, -8 * S, out_ease), (62, -12 * S)])
    r.save(os.path.join(out, "rive_tab_family.riv"), machine())


if __name__ == "__main__":
    for make in (pear, diary, learn, family):
        make(sys.argv[1])
