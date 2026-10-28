"""
Stress tests on two emulators (Mamá on 5556, Ana on 5554, both with the debug build): an SMS flood, no network, low
memory and a killed process, big letters, fast tab switching. Prints what crashed or froze (FATAL EXCEPTION / ANR) and
how much memory the app uses, and saves a screenshot with big letters. Then run Android's monkey for random taps:

    python eval/stress.py [out_dir]
    adb -s emulator-5554 shell monkey -p app.friendlypear --throttle 70 --pct-syskeys 0 -v 2500
"""
import os
import re
import subprocess
import sys
import time

ADB = os.environ.get("ADB", "adb")
OUT = sys.argv[1] if len(sys.argv) > 1 else "."
MAMA, ANA = "emulator-5556", "emulator-5554"
PKG = "app.friendlypear"


def adb(phone, *args, timeout=60):
    try:
        return subprocess.run([ADB, "-s", phone, *args], capture_output=True, timeout=timeout).stdout.decode("utf-8", "replace")
    except subprocess.TimeoutExpired:
        return "TIMEOUT"


def problems(phone):
    log = adb(phone, "logcat", "-d", "-b", "crash,main,system")
    fatal = [l for l in log.splitlines() if "FATAL EXCEPTION" in l or ("ANR in" in l and PKG in l)]
    trace = []
    if fatal:
        lines = log.splitlines()
        i = next(k for k, l in enumerate(lines) if "FATAL EXCEPTION" in l or "ANR in" in l)
        trace = lines[i: i + 12]
    return fatal, trace


def memory(phone):
    out = adb(phone, "shell", "dumpsys", "meminfo", PKG)
    m = re.search(r"TOTAL PSS:\s+(\d+)", out) or re.search(r"TOTAL\s+(\d+)", out)
    return int(m.group(1)) // 1024 if m else None


def step(name):
    print(f"\n== {name}", flush=True)


for phone in (MAMA, ANA):
    adb(phone, "logcat", "-c")
    adb(phone, "shell", "am", "start", "-n", f"{PKG}/.ui.peri.PeriActivity")
time.sleep(6)
print("memory at start (MB):", memory(MAMA), memory(ANA))

step("SMS flood: 30 texts in a row to Mamá, scams and ordinary ones")
texts = [
    "Correos: su paquete esta retenido, pague 1,99 EUR aqui http://correos-envio.top/p",
    "Hola mama, soy Ana, este es mi numero nuevo, me haces un bizum urgente?",
    "Tu codigo de verificacion es 482913. No lo compartas con nadie.",
    "Quedamos manana a las 5 para el cafe?",
    "Felicidades! Has ganado un iPhone, reclamalo en premio-ya.xyz",
]
for i in range(30):
    adb(MAMA, "emu", "sms", "send", f"6{(i % 7) + 10}000{i:03d}", texts[i % len(texts)])
    time.sleep(0.4)
time.sleep(25)
print("problems:", problems(MAMA))
print("memory after flood (MB):", memory(MAMA))
adb(MAMA, "exec-out", "screencap", "-p")

step("No network on Ana's phone for 40 s (family polls must fail quietly)")
adb(ANA, "shell", "svc", "wifi", "disable")
adb(ANA, "shell", "svc", "data", "disable")
adb(ANA, "shell", "am", "start", "-n", f"{PKG}/.ui.peri.PeriActivity")
time.sleep(40)
adb(ANA, "shell", "svc", "wifi", "enable")
adb(ANA, "shell", "svc", "data", "enable")
time.sleep(8)
print("problems:", problems(ANA))

step("Low memory: trim levels, then the process killed in the background and reopened")
for level in ("RUNNING_LOW", "RUNNING_CRITICAL", "COMPLETE"):
    adb(ANA, "shell", "am", "send-trim-memory", PKG, level)
    time.sleep(1)
adb(ANA, "shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(2)
adb(ANA, "shell", "am", "kill", PKG)
time.sleep(2)
adb(ANA, "shell", "am", "start", "-n", f"{PKG}/.ui.peri.PeriActivity")
time.sleep(8)
print("problems:", problems(ANA))

step("Big letters (font scale 1.3)")
adb(ANA, "shell", "settings", "put", "system", "font_scale", "1.3")
adb(ANA, "shell", "am", "force-stop", PKG)
adb(ANA, "shell", "am", "start", "-n", f"{PKG}/.ui.peri.PeriActivity")
time.sleep(9)
open(f"{OUT}/stress_bigfont.png", "wb").write(subprocess.run([ADB, "-s", ANA, "exec-out", "screencap", "-p"], capture_output=True).stdout)
adb(ANA, "shell", "settings", "put", "system", "font_scale", "1.0")
print("problems:", problems(ANA))

step("Fast tab switching: 60 taps on the tab bar")
adb(ANA, "shell", "am", "force-stop", PKG)
adb(ANA, "shell", "am", "start", "-n", f"{PKG}/.ui.peri.PeriActivity")
time.sleep(8)
xs = [170, 415, 660, 910]
cmd = "; ".join(f"input tap {xs[i % 4]} 2250" for i in range(60))
adb(ANA, "shell", cmd, timeout=120)
time.sleep(4)
print("problems:", problems(ANA))
print("memory after (MB):", memory(ANA))
