"""Dumps a .riv file as objects with named properties, using schema.json."""
import json, struct, sys

schema = json.load(open(sys.argv[1]))
by_key = {c["typeKey"]: name for name, c in schema.items()}


def all_props(name):
    out = {}
    while name in schema:
        for p, d in schema[name]["props"].items():
            out[d["key"]] = (p, d["type"])
        name = schema[name]["super"]
    return out


data = open(sys.argv[2], "rb").read()
pos = 0


def varuint():
    global pos
    r = s = 0
    while True:
        b = data[pos]; pos += 1
        r |= (b & 0x7F) << s; s += 7
        if b < 0x80:
            return r


assert data[:4] == b"RIVE"; pos = 4
major, minor, fid = varuint(), varuint(), varuint()
print("version", major, minor, "file", fid)
keys = []
while True:
    k = varuint()
    if k == 0:
        break
    keys.append(k)
toc = {}
bit = 8
for k in keys:
    if bit == 8:
        cur = struct.unpack_from("<I", data, pos)[0]; pos += 4; bit = 0
    toc[k] = (cur >> bit) & 3; bit += 2
print("toc", toc)
idx = 0
while pos < len(data):
    tk = varuint()
    name = by_key.get(tk, f"?{tk}")
    props = all_props(name)
    vals = []
    while True:
        k = varuint()
        if k == 0:
            break
        pname, ptype = props[k] if k in props else (f"?{k}", {0: "uint", 1: "string", 2: "double", 3: "color"}[toc[k]])
        if ptype in ("uint",):
            v = varuint()
        elif ptype == "bool":
            v = bool(data[pos]); pos += 1
        elif ptype in ("string", "bytes"):
            n = varuint(); v = data[pos:pos + n]; pos += n
            v = v.decode("utf8", "replace") if ptype == "string" else f"<{n} bytes>"
        elif ptype == "double":
            v = round(struct.unpack_from("<f", data, pos)[0], 3); pos += 4
        elif ptype == "color":
            v = hex(struct.unpack_from("<I", data, pos)[0]); pos += 4
        vals.append(f"{pname}={v}")
    print(idx, name, " ".join(vals))
    idx += 1
