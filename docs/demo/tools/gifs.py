"""
The README's feature GIFs, recorded for real on the emulators with the debug build and Peri's server running (the
same two phones as eval/features.py: Mamá on 5556 in English, Pablo on 5560). Each clip is the phone's own screen
recording, made into a small looping GIF with ffmpeg in docs/media/features/.

    python docs/demo/tools/gifs.py [name ...]
"""
import os
import subprocess
import sys
import threading
import time

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
OUT = os.path.join(ROOT, "docs", "media", "features")
WANTED = set(sys.argv[1:])
os.makedirs(OUT, exist_ok=True)
# The helpers (adb, sms, hear, tap_text…) come from the feature test, without running its tests.
sys.argv = [sys.argv[0], OUT, "__none__"]
exec(open(os.path.join(ROOT, "eval", "features.py"), encoding="utf-8").read().split("\nsetup()\n")[0])
FFMPEG = os.environ.get("FFMPEG", "ffmpeg")
WIDTH = 300


def animations(phone, on):
    for k in ANIMATIONS:
        adb(phone, "shell", "settings", "put", "global", k, "1" if on else "0")


def record(name, phone, seconds, act, skip=0.0, speed=1.0):
    """Records [seconds] of [phone] while [act] runs, and makes docs/media/features/[name].gif from it."""
    if WANTED and name not in WANTED:
        return
    print(f"== {name}", flush=True)
    remote = f"/sdcard/{name}.mp4"
    rec = threading.Thread(target=adb, args=(phone, "shell", "screenrecord", "--size", "540x1200", "--bit-rate", "3000000",
                                             "--time-limit", str(seconds), remote), kwargs={"timeout": seconds + 30})
    rec.start()
    time.sleep(1)
    act()
    rec.join()
    time.sleep(1)
    mp4 = os.path.join(OUT, f"{name}.mp4")
    subprocess.run([ADB, "-s", phone, "pull", remote, mp4], capture_output=True, env=ENV)
    gif = os.path.join(OUT, f"{name}.gif")
    # One palette for the clip, few colours and a light dither: small files that still look like clay.
    # Long waits for the other phone are played faster, so the GIF stays short.
    vf = (f"setpts=PTS/{speed},fps=9,scale={WIDTH}:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=96:stats_mode=diff[p];"
          "[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle")
    subprocess.run([FFMPEG, "-y", "-loglevel", "error", "-ss", str(skip), "-i", mp4, "-vf", vf, gif], check=True)
    os.remove(mp4)
    print(f"   {os.path.getsize(gif) // 1024} KB", flush=True)


def ready(phone=MAMA, moving=True):
    """
    Peri's home, fresh and calm, with no old card in the way. Android can only read a still screen, so the app starts
    with animations off; a clip that taps buttons keeps them off (Peri keeps her poses but doesn't move), the others
    turn them back on and Peri comes alive.
    """
    animations(phone, False)
    fresh(phone)
    for _ in range(3):
        if not tap_text(phone, "Hide|Ocultar|Lovely|Qué bien", tries=1):
            break
    if moving:
        animations(phone, True)
    time.sleep(2)


def new_number():
    sms(MAMA, "447700900123", "Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today for a bill? x")
    time.sleep(16)


ready()
record("new-number", MAMA, 18, new_number)


def stranger_call():
    adb(MAMA, "emu", "gsm", "call", STRANGER)
    time.sleep(7)
    adb(MAMA, "emu", "gsm", "accept", STRANGER)
    time.sleep(4)
    adb(MAMA, "emu", "gsm", "cancel", STRANGER)
    time.sleep(6)
    hear(MAMA, "the bank")
    time.sleep(6)
    hear(MAMA, "they wanted the code they texted me")
    time.sleep(8)


ready()
record("stranger-call", MAMA, 32, stranger_call)


def code_during_call():
    adb(MAMA, "emu", "gsm", "call", "911000222")
    time.sleep(3)
    adb(MAMA, "emu", "gsm", "accept", "911000222")
    adb(MAMA, "shell", "am", "start", "-n", f"{PKG}/.ui.peri.PeriActivity")
    time.sleep(3)
    sms(MAMA, "22395", "Your bank verification code is 482913. Never share it with anyone.")
    time.sleep(10)
    adb(MAMA, "emu", "gsm", "cancel", "911000222")


ready()
record("code-during-call", MAMA, 17, code_during_call)


def safety_pause():
    adb(MAMA, "emu", "gsm", "call", STRANGER)
    time.sleep(3)
    adb(MAMA, "emu", "gsm", "accept", STRANGER)
    adb(MAMA, "shell", "am", "start", "-n", f"{PKG}/.ui.peri.PeriActivity")
    time.sleep(2)
    debug(MAMA, "opened", "com.bbva.bbvacontigo")
    time.sleep(13)
    adb(MAMA, "emu", "gsm", "cancel", STRANGER)


ready()
record("safety-pause", MAMA, 20, safety_pause)


def website():
    debug(MAMA, "web", "virus-alert.top|Your phone has 5 viruses!|Tap now to clean it or your bank data will be stolen")
    time.sleep(14)


ready()
record("website-notice", MAMA, 15, website)


def remote_app():
    debug(MAMA, "installed", "com.anydesk.anydeskandroid")
    time.sleep(13)


ready()
record("remote-app", MAMA, 14, remote_app)


def first_aid():
    hear(MAMA, "I gave them the code")
    time.sleep(8)
    adb(MAMA, "shell", "input", "swipe", "540", "1700", "540", "900", "800")
    time.sleep(4)


ready()
record("first-aid", MAMA, 15, first_aid)


def chat():
    tap_text(MAMA, "Write to Peri", tries=2)
    time.sleep(3)
    tap_class(MAMA, "android.widget.EditText")
    adb(MAMA, "shell", "input", "text", "I%sgot%sthis:%sYour%sparcel%sis%sheld,%spay%s1.99%sat%shttp://correos-envio.top")
    time.sleep(1)
    tap_text(MAMA, "Send", tries=2)
    time.sleep(10)


ready(moving=False)
record("chat", MAMA, 18, chat)


def block_and_report():
    # A number never used before: the clip blocks it, and a blocked sender's texts are silenced from then on.
    sms(MAMA, f"6112{int(time.time()) % 100000:05d}", "Correos: su paquete esta retenido, pague 1,99 EUR aqui http://correos-envio.top/p")
    time.sleep(9)
    tap_text(MAMA, "See the trap", tries=2)
    time.sleep(5)
    adb(MAMA, "shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(2)
    tap_text(MAMA, "Block this number", tries=2)
    time.sleep(5)


ready(moving=False)
record("trap-card", MAMA, 32, block_and_report)


def game():
    tap_text(MAMA, "Scam or not", tries=2)
    time.sleep(3)
    for _ in range(3):
        tap_text(MAMA, "It's a trap", tries=2)
        time.sleep(4)
        tap_text(MAMA, "Next", tries=1)
        time.sleep(2)


ready(moving=False)
record("scam-or-not", MAMA, 24, game)


def checkup():
    tap_text(MAMA, "Check my phone", tries=2)
    time.sleep(4)
    adb(MAMA, "shell", "input", "swipe", "540", "1800", "540", "700", "900")
    time.sleep(3)
    adb(MAMA, "shell", "input", "swipe", "540", "1800", "540", "700", "900")
    time.sleep(3)


ready(moving=False)
record("checkup", MAMA, 16, checkup)


def play():
    for x, y in ((540, 330), (540, 520), (540, 660)):
        adb(MAMA, "shell", "input", "tap", str(x), str(y))
        time.sleep(3)
    adb(MAMA, "shell", "input", "swipe", "540", "520", "250", "900", "700")
    time.sleep(3)
    for _ in range(5):
        adb(MAMA, "shell", "input", "tap", "540", "520")
        time.sleep(0.25)
    time.sleep(4)


ready()
record("playing", MAMA, 20, play)


def mini_peri():
    adb(MAMA, "shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(2)
    adb(MAMA, "emu", "gsm", "call", STRANGER)
    time.sleep(4)
    # She comes out beside a stranger's call once it's picked up, and goes when it ends.
    adb(MAMA, "emu", "gsm", "accept", STRANGER)
    time.sleep(9)
    adb(MAMA, "emu", "gsm", "cancel", STRANGER)
    time.sleep(3)


ready()
record("mini-peri", MAMA, 22, mini_peri)


# Two phones: Pablo answers "was it you?" on his phone, and Mamá hangs up a stranger's call on Pablo's.
def was_it_you():
    sms(MAMA, "447700900456", "Hi Grandma it's Pablo, new number, my old phone fell in the sea. Text me here")
    wait_for(lambda: has(screen(PABLO), "escrito tú|Was it you"), timeout=80, every=4)
    time.sleep(2)
    # Any older question still open shows first: answer them all, then Mamá's Peri hears back within a minute.
    for _ in range(4):
        if not tap_text(PABLO, "No he sido yo|No, it wasn", tries=2):
            break
        time.sleep(3)
    # Mamá's Peri polls every 10 s and says who answered once a minute has passed without a "yes".
    time.sleep(80)


ready(PABLO, moving=False)
ready(MAMA)
record("was-it-you", MAMA, 150, was_it_you, speed=4.0)


def hang_up_theirs():
    adb(PABLO, "emu", "gsm", "call", STRANGER)
    time.sleep(3)
    adb(PABLO, "emu", "gsm", "accept", STRANGER)
    wait_for(lambda: tap_text(MAMA, "Hang up their call", tries=1), timeout=70, every=4)
    time.sleep(10)
    hang_up(PABLO)


ready(PABLO)
ready(MAMA, moving=False)
record("hang-up-theirs", MAMA, 75, hang_up_theirs, speed=3.0)

for p in PHONES:
    animations(p, True)
print("done")
