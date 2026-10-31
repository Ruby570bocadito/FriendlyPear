"""Makes every .riv the app uses, into android/app/src/main/res/raw: python assets/rive/build.py

Each file is written from code (shield.py, stars.py, scan.py, ring.py, pulse.py, burst.py, write.py,
link.py, tabs.py, icons.py) with rivwriter.py, Rive's runtime format
read from rive-runtime's generated headers (schema.json, made by schema.py; MIT License). rivdump.py prints a .riv
back as objects, to check one. To change an animation, change its script and run this again.
"""
import os, subprocess, sys

here = os.path.dirname(os.path.abspath(__file__))
raw = os.path.join(here, "..", "..", "android", "app", "src", "main", "res", "raw")
os.makedirs(raw, exist_ok=True)
for name in ("shield", "stars", "scan", "ring", "pulse", "burst", "write", "link"):
    subprocess.run([sys.executable, os.path.join(here, f"{name}.py"), os.path.join(raw, f"rive_{name}.riv")], check=True)
    print("rive_%s.riv" % name)
# The app's own vector icons, alive: the tab bar's four, then the microphone, the shields chip's shield and arrows.
for name, made in (("tabs", "rive_tab_*.riv"), ("icons", "rive_voice.riv, rive_glint.riv, rive_send.riv, rive_next.riv")):
    subprocess.run([sys.executable, os.path.join(here, f"{name}.py"), raw], check=True)
    print(made)
