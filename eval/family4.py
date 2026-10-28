"""
A family of four phones under attack (debug build, all paired with the family's server, Ana and Pablo sharing Peri's
news): Mamá (5556) gets a stranger's call, then a "new number" text; Ana (5554) a phishing email; Pablo (5560) the same
trap by SMS; Papá (5558) has the app closed and acts from notifications. Saves screenshots (fam_*.png) and prints what
each Peri did and any crash.

    python eval/family4.py [out_dir]
"""
import os
import re
import subprocess
import sys
import time

ADB = os.environ.get("ADB", "adb")
APK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "android", "app", "build", "outputs", "apk", "debug", "app-debug.apk")
OUT = sys.argv[1] if len(sys.argv) > 1 else "."
PKG = "app.friendlypear"
MAMA, ANA, PAPA, PABLO = "emulator-5556", "emulator-5554", "emulator-5558", "emulator-5560"
ALL = {MAMA: "mama", ANA: "ana", PAPA: "papa", PABLO: "pablo"}
STRANGER = "699000222"
ENV = {**os.environ, "MSYS_NO_PATHCONV": "1"}


def adb(phone, *args, timeout=60):
    try:
        return subprocess.run([ADB, "-s", phone, *args], capture_output=True, timeout=timeout, env=ENV).stdout.decode("utf-8", "replace")
    except subprocess.TimeoutExpired:
        return "TIMEOUT"


def shot(phone, name):
    data = subprocess.run([ADB, "-s", phone, "exec-out", "screencap", "-p"], capture_output=True, timeout=30).stdout
    open(f"{OUT}/fam_{name}.png", "wb").write(data)


def log(phone, n=6):
    return [l.split(" FriendlyPear: ")[1] for l in adb(phone, "logcat", "-d").splitlines() if " FriendlyPear: " in l][-n:]


def tap_text(phone, label, tries=6):
    for _ in range(tries):
        adb(phone, "shell", "uiautomator", "dump", "/sdcard/ui.xml", timeout=40)
        xml = adb(phone, "exec-out", "cat", "/sdcard/ui.xml")
        m = re.search(r'(?:text|content-desc)="' + re.escape(label) + r'[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml, re.I)
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            adb(phone, "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
            return True
        time.sleep(2)
    return False


def share(phone):
    local = f"{OUT}/shared_news.xml"
    open(local, "w").write("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n    <boolean name=\"sharing\" value=\"true\" />\n</map>\n")
    adb(phone, "shell", "am", "force-stop", PKG)
    adb(phone, "push", local, "/data/local/tmp/shared_news.xml")
    adb(phone, "shell", "chmod", "644", "/data/local/tmp/shared_news.xml")
    adb(phone, "shell", "run-as", PKG, "cp", "/data/local/tmp/shared_news.xml", "shared_prefs/shared_news.xml")


print("== setup")
for phone in ALL:
    print(phone, adb(phone, "install", "-r", APK, timeout=180).strip().splitlines()[-1:])
for phone in (ANA, PABLO):
    share(phone)
for phone in ALL:
    adb(phone, "logcat", "-c")
    adb(phone, "shell", "am", "start", "-n", f"{PKG}/.ui.peri.PeriActivity")
time.sleep(8)
adb(PAPA, "shell", "input", "keyevent", "KEYCODE_HOME")
adb(PAPA, "shell", "cmd", "statusbar", "collapse")
time.sleep(3)

print("\n== 1. A stranger calls Mamá; Papá, with the app closed, asks her Peri to hang up")
adb(MAMA, "emu", "gsm", "call", STRANGER)
time.sleep(4)
adb(MAMA, "emu", "gsm", "accept", STRANGER)
time.sleep(80)
adb(PAPA, "shell", "cmd", "statusbar", "expand-notifications")
time.sleep(3)
shot(PAPA, "1_papa_notification")
adb(PAPA, "shell", "settings", "put", "global", "animator_duration_scale", "0")
print("papa tapped 'Hang up their call':", tap_text(PAPA, "Hang up their call"))
adb(PAPA, "shell", "settings", "put", "global", "animator_duration_scale", "1")
time.sleep(9)
shot(MAMA, "1_mama_countdown")
time.sleep(8)
state = re.findall(r"mCallState=(\d)", adb(MAMA, "shell", "dumpsys", "telephony.registry"))
print("mama call state now:", state[:1], "(0 = hung up)")
adb(MAMA, "emu", "gsm", "cancel", STRANGER)
print("mama:", *log(MAMA, 5), sep="\n  ")
adb(PAPA, "shell", "cmd", "statusbar", "collapse")

print("\n== 2. Mamá: 'new number, it's Pablo'; the family is asked, Pablo answers in his app")
adb(MAMA, "emu", "sms", "send", "633444555", "Hola mama, soy Pablo. Este es mi numero nuevo, se me rompio el movil. Me haces un bizum de 300 euros? Es urgente")
time.sleep(75)
shot(PAPA, "2_papa_home")
adb(PAPA, "shell", "cmd", "statusbar", "expand-notifications")
time.sleep(3)
shot(PAPA, "2_papa_question")
adb(PAPA, "shell", "cmd", "statusbar", "collapse")
shot(PABLO, "2_pablo_card")
adb(PABLO, "shell", "settings", "put", "global", "animator_duration_scale", "0")
print("pablo tapped 'No, it wasn't me':", tap_text(PABLO, "No, it wasn"))
adb(PABLO, "shell", "settings", "put", "global", "animator_duration_scale", "1")
time.sleep(75)
shot(MAMA, "2_mama_result")
print("mama:", *log(MAMA, 4), sep="\n  ")

print("\n== 3. Ana gets a phishing email and Pablo the same trap by SMS: the trap is going around")
# One shell string, quoted: the device's shell would read < > | itself.
adb(ANA, "shell", "am broadcast -a app.friendlypear.debug.HEAR --es email 'BBVA Seguridad <alertas@bbva-verifica.top>|Su cuenta ha sido bloqueada. Verifique sus datos hoy en http://bbva-verifica.top/acceso o se cerrara.'")
time.sleep(4)
adb(PABLO, "emu", "sms", "send", "644555666", "BBVA: acceso a su cuenta bloqueado. Verifique sus datos en http://bbva-verifica.top/acceso")
time.sleep(20)
shot(ANA, "3_ana_email")
shot(PABLO, "3_pablo_sms")
time.sleep(70)
adb(PAPA, "shell", "cmd", "statusbar", "expand-notifications")
time.sleep(3)
shot(PAPA, "3_papa_campaign")
adb(PAPA, "shell", "cmd", "statusbar", "collapse")
for phone, name in ALL.items():
    print(name, *log(phone, 3), sep="\n  ")
    crash = [l for l in adb(phone, "logcat", "-d", "-b", "crash").splitlines() if "FATAL" in l]
    print("  crashes:", len(crash))
