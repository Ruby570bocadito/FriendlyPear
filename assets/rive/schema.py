"""Reads rive-runtime's generated *_base.hpp headers into a schema: classes, type keys, properties and field types."""
import json, os, re, sys

ROOT = sys.argv[1]  # .../include/rive/generated
TYPES = {"CoreUintType": "uint", "CoreIdType": "uint", "CoreIntType": "uint", "CoreStringType": "string",
         "CoreBytesType": "bytes", "CoreDoubleType": "double", "CoreColorType": "color", "CoreBoolType": "bool"}

classes = {}
for dirpath, _, files in os.walk(ROOT):
    for f in files:
        if not f.endswith("_base.hpp"):
            continue
        src = open(os.path.join(dirpath, f), encoding="utf8").read()
        m = re.search(r"class (\w+)Base : public ([\w:]+)", src)
        tk = re.search(r"static const uint16_t typeKey = (\d+);", src)
        if not m or not tk:
            continue
        keys = dict((n, int(k)) for n, k in re.findall(r"static const uint16_t (\w+)PropertyKey = (\d+);", src))
        props = {}
        for case, typ in re.findall(r"case (\w+)PropertyKey:\s*\n(?:#.*\n)?\s*[\w>()\-.]+ = (\w+)::\w*[dD]eserialize", src):
            props[case] = {"key": keys[case], "type": TYPES[typ]}
        classes[m.group(1)] = {"typeKey": int(tk.group(1)), "super": m.group(2).split("::")[-1], "props": props}

json.dump(classes, open(sys.argv[2], "w"), indent=1)
print(len(classes), "classes")
