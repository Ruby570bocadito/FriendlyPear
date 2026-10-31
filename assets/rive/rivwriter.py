"""A tiny writer for Rive's runtime format (.riv, major 7): shapes, paints, keyframed animations and a state machine.

Object ids are artboard-local: the artboard is 0, every component after it counts up in file order. Animations
are numbered among themselves, and so are a layer's states and a state machine's inputs.
"""
import json, math, os, re, struct

SCHEMA = json.load(open(os.path.join(os.path.dirname(__file__), "schema.json")))
FIELD_ID = {"uint": 0, "bool": 0, "string": 1, "bytes": 1, "double": 2, "color": 3}


def _props(cls):
    out = {}
    while cls in SCHEMA:
        for name, d in SCHEMA[cls]["props"].items():
            out.setdefault(name, d)
        cls = SCHEMA[cls]["super"]
    return out


def _varuint(n):
    out = bytearray()
    while True:
        b = n & 0x7F; n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b); return bytes(out)


class Obj:
    def __init__(self, cls, **props):
        self.cls, self.props = cls, props


class Riv:
    def __init__(self, name, width, height):
        self.objects = [Obj("Backboard"), Obj("Artboard", name=name, width=float(width), height=float(height),
                                               clip=True, defaultStateMachineId=0)]
        self.components = 1  # the artboard is component 0
        self.animations = []

    # --- components ---------------------------------------------------------------------------------------------
    def add(self, cls, **props):
        self.objects.append(Obj(cls, **props))
        cid = self.components
        self.components += 1
        return cid

    def node(self, parent, x=0, y=0, name=None, **kw):
        return self.add("Node", name=name, parentId=parent, x=float(x), y=float(y), **kw)

    def shape(self, parent, x=0, y=0, name=None, **kw):
        return self.add("Shape", name=name, parentId=parent, x=float(x), y=float(y), **kw)

    def ellipse(self, shape, w, h, x=0, y=0):
        return self.add("Ellipse", parentId=shape, width=float(w), height=float(h), x=float(x), y=float(y))

    def rect(self, shape, w, h, radius=0, x=0, y=0):
        return self.add("Rectangle", parentId=shape, width=float(w), height=float(h), x=float(x), y=float(y),
                        cornerRadiusTL=float(radius), linkCornerRadius=True)

    def path(self, shape, points, closed=True):
        """points: (x, y) or (x, y, corner radius)."""
        pid = self.add("PointsPath", parentId=shape, isClosed=closed)
        for p in points:
            self.add("StraightVertex", parentId=pid, x=float(p[0]), y=float(p[1]),
                     radius=float(p[2]) if len(p) > 2 else None)
        return pid

    def svg(self, shape, d, scale=1.0, origin=(0, 0)):
        """An SVG or Android pathData (M L H V C S Z, absolute or relative) as paths of [shape], each point moved by
        -origin and multiplied by scale. Returns the path ids, one per subpath, in order."""
        return [self.curve(shape, nodes, closed) for nodes, closed in parse_svg(d, scale, origin)]

    def curve(self, shape, nodes, closed=True):
        """nodes: (x, y, in control or None, out control or None), each control an (x, y) point."""
        pid = self.add("PointsPath", parentId=shape, isClosed=closed)
        for x, y, cin, cout in nodes:
            cin, cout = cin or (x, y), cout or (x, y)
            self.add("CubicDetachedVertex", parentId=pid, x=float(x), y=float(y),
                     inRotation=math.atan2(cin[1] - y, cin[0] - x), inDistance=math.hypot(cin[0] - x, cin[1] - y),
                     outRotation=math.atan2(cout[1] - y, cout[0] - x), outDistance=math.hypot(cout[0] - x, cout[1] - y))
        return pid

    def fill(self, shape, color):
        f = self.add("Fill", parentId=shape)
        return f, self.add("SolidColor", parentId=f, colorValue=color)

    def radial(self, shape, radius, stops):
        """A fill fading out from the shape's centre: stops are (position 0..1, color)."""
        f = self.add("Fill", parentId=shape)
        g = self.add("RadialGradient", parentId=f, startX=0.0, startY=0.0, endX=float(radius), endY=0.0)
        for position, color in stops:
            self.add("GradientStop", parentId=g, colorValue=color, position=float(position))
        return f, g

    def stroke(self, shape, color, thickness, cap=0, join=0):
        s = self.add("Stroke", parentId=shape, thickness=float(thickness), cap=cap, join=join)
        return s, self.add("SolidColor", parentId=s, colorValue=color)

    def trim(self, stroke, end=0.0):
        return self.add("TrimPath", parentId=stroke, start=0.0, end=float(end), offset=0.0, modeValue=1)

    def cubic(self, x1, y1, x2, y2):
        return self.add("CubicEaseInterpolator", x1=float(x1), y1=float(y1), x2=float(x2), y2=float(y2))

    # --- animations ---------------------------------------------------------------------------------------------
    def animation(self, name, frames, loop=1, fps=60):
        """loop: 0 one shot, 1 loop, 2 ping-pong. Returns (animation id, keys) where keys(obj, prop, [frames])."""
        anim = {"obj": Obj("LinearAnimation", name=name, fps=fps, duration=frames, loopValue=loop), "keys": []}
        self.animations.append(anim)

        def keys(obj, prop, frames_, color=False):
            """frames_: (frame, value) or (frame, value, interpolator id or 'hold' or 'linear')."""
            anim["keys"].append((obj, prop, frames_, color))
        return len(self.animations) - 1, keys

    # --- writing ------------------------------------------------------------------------------------------------
    def save(self, path, machine):
        objs = list(self.objects)
        for anim in self.animations:
            objs.append(anim["obj"])
            grouped = {}
            for obj, prop, frames_, color in anim["keys"]:
                grouped.setdefault(obj, []).append((prop, frames_, color))
            for obj, props in grouped.items():
                objs.append(Obj("KeyedObject", objectId=obj))
                for prop, frames_, color in props:
                    key = PROPERTY_KEYS[prop]
                    objs.append(Obj("KeyedProperty", propertyKey=key))
                    for f in frames_:
                        frame, value = f[0], f[1]
                        how = f[2] if len(f) > 2 else "linear"
                        kw = {"frame": frame, "value": value if color else float(value)}
                        if how == "hold":
                            kw["interpolationType"] = 0
                        elif how == "linear":
                            kw["interpolationType"] = 1
                        else:
                            kw["interpolationType"] = 2; kw["interpolatorId"] = how
                        objs.append(Obj("KeyFrameColor" if color else "KeyFrameDouble", **kw))
        objs.extend(machine)

        body, used = bytearray(), {}
        for o in objs:
            body += _varuint(SCHEMA[o.cls]["typeKey"])
            fields = _props(o.cls)
            for name, value in o.props.items():
                if value is None:
                    continue
                d = fields[name]
                used[d["key"]] = FIELD_ID[d["type"]]
                body += _varuint(d["key"])
                t = d["type"]
                if t == "uint":
                    body += _varuint(int(value))
                elif t == "bool":
                    body += bytes([1 if value else 0])
                elif t == "string":
                    b = value.encode("utf8"); body += _varuint(len(b)) + b
                elif t == "double":
                    body += struct.pack("<f", float(value))
                elif t == "color":
                    body += struct.pack("<I", value & 0xFFFFFFFF)
            body += _varuint(0)

        head = bytearray(b"RIVE") + _varuint(7) + _varuint(0) + _varuint(0)
        keys = sorted(used)
        for k in keys:
            head += _varuint(k)
        head += _varuint(0)
        for i in range(0, len(keys), 4):  # the reader takes 4 fields per uint32
            word = 0
            for j, k in enumerate(keys[i:i + 4]):
                word |= used[k] << (j * 2)
            head += struct.pack("<I", word)
        open(path, "wb").write(bytes(head + body))


def parse_svg(d, scale, origin):
    """pathData as subpaths of (x, y, in control, out control) nodes, and whether each is closed."""
    tokens = re.findall(r"[MmLlHhVvCcSsZz]|[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?", d)
    at, start, last_c2 = (0.0, 0.0), (0.0, 0.0), None
    subpaths, segments, cmd, i = [], [], None, 0

    def num():
        nonlocal i
        i += 1
        return float(tokens[i - 1])

    def finish(closed):
        if segments:
            subpaths.append((segments[:], closed))
        segments.clear()

    while i < len(tokens):
        if re.match(r"[A-Za-z]", tokens[i]):
            cmd = tokens[i]; i += 1
            if cmd in "Zz":
                if math.dist(at, start) > 1e-6:
                    segments.append((at, at, start, start))
                finish(True)
                at, last_c2 = start, None
                continue
        rel = cmd.islower()
        ox, oy = at if rel else (0.0, 0.0)
        c = cmd.upper()
        if c == "M":
            finish(False)
            at = start = (ox + num(), oy + num()); last_c2 = None
            cmd = "l" if rel else "L"  # further pairs are lines
        elif c == "L":
            end = (ox + num(), oy + num()); segments.append((at, at, end, end)); at, last_c2 = end, None
        elif c == "H":
            end = ((at[0] if rel else 0.0) + num(), at[1]); segments.append((at, at, end, end)); at, last_c2 = end, None
        elif c == "V":
            end = (at[0], (at[1] if rel else 0.0) + num()); segments.append((at, at, end, end)); at, last_c2 = end, None
        elif c == "C":
            c1 = (ox + num(), oy + num()); c2 = (ox + num(), oy + num()); end = (ox + num(), oy + num())
            segments.append((at, c1, c2, end)); at, last_c2 = end, c2
        elif c == "S":
            c1 = (2 * at[0] - last_c2[0], 2 * at[1] - last_c2[1]) if last_c2 else at
            c2 = (ox + num(), oy + num()); end = (ox + num(), oy + num())
            segments.append((at, c1, c2, end)); at, last_c2 = end, c2
    finish(False)

    out = []
    for segs, closed in subpaths:
        tr = lambda p: ((p[0] - origin[0]) * scale, (p[1] - origin[1]) * scale)
        nodes = []
        for j, (a, c1, c2, b) in enumerate(segs):
            cin = segs[j - 1][2] if j > 0 else (segs[-1][2] if closed and math.dist(segs[-1][3], a) < 1e-6 else None)
            nodes.append([*tr(a), tr(cin) if cin else None, tr(c1)])
        if not closed:
            b = segs[-1][3]
            nodes.append([*tr(b), tr(segs[-1][2]), None])
        out.append(([tuple(n) for n in nodes], closed))
    return out


PROPERTY_KEYS = {"x": 13, "y": 14, "rotation": 15, "scaleX": 16, "scaleY": 17, "opacity": 18, "color": 37,
                 "trimEnd": 115, "trimStart": 114, "trimOffset": 116, "width": 20, "height": 21}


class Machine:
    """A state machine with one layer: inputs, then states with their transitions."""

    def __init__(self, name):
        self.out = [Obj("StateMachine", name=name)]
        self.inputs = 0
        self.layer = []

    def number(self, name, value=0.0):
        self.out.append(Obj("StateMachineNumber", name=name, value=float(value)))
        self.inputs += 1
        return self.inputs - 1

    def trigger(self, name):
        self.out.append(Obj("StateMachineTrigger", name=name))
        self.inputs += 1
        return self.inputs - 1

    def objects(self, states, *more_layers):
        """states: list of (kind, animation id or None, [(to state index, [conditions], duration ms[, exit ms])]).
        kind: 'entry', 'any', 'exit' or 'anim'. A condition is (input id, op, value) for numbers or (input id,) for
        a trigger. Each of more_layers is another such list, played at the same time on top."""
        out = list(self.out)
        for n, layer in enumerate((states,) + more_layers):
            out.append(Obj("StateMachineLayer", name=f"Layer {n + 1}"))
            out.extend(self._layer(layer))
        return out

    @staticmethod
    def _layer(states):
        out = []
        for kind, anim, transitions in states:
            cls = {"entry": "EntryState", "any": "AnyState", "exit": "ExitState", "anim": "AnimationState"}[kind]
            out.append(Obj(cls, animationId=anim) if kind == "anim" else Obj(cls))
            for to, conditions, duration, *exit in transitions:
                # An exit time (ms) waits for the animation to get that far, as when a one-shot hands over to a loop.
                out.append(Obj("StateTransition", stateToId=to, duration=duration or None,
                               flags=4 if exit else None, exitTime=exit[0] if exit else None))
                for c in conditions:
                    if len(c) == 1:
                        out.append(Obj("TransitionTriggerCondition", inputId=c[0]))
                    else:
                        out.append(Obj("TransitionNumberCondition", inputId=c[0], opValue=c[1] or None,
                                       value=float(c[2])))
        return out
