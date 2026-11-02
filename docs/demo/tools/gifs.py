"""
The README's feature GIFs, recorded for real on the emulators with the debug build and Peri's server running (the
phones of eval/features.py: Mamá on 5556 in English, Pablo on 5560 in Spanish, plus Papá on 5558, a guardian with
FriendlyPear, in Spanish). Each clip is the phones' own screen recording, made into a small looping GIF with ffmpeg in
docs/media/features/; the family's stories show two phones side by side, with the waits cut out.

The server must sign its links with the address the emulators see, or Papá's Peri won't trust the request:

    cd backend && PUBLIC_BASE_URL=http://10.0.2.2:8711 bun src/main.ts
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
PAPA = os.environ.get("PAPA", "emulator-5558")
# Labels over each phone in the side-by-side clips (Windows' Segoe UI Semibold; any TTF will do).
FONT = os.environ.get("GIF_FONT", "C\\:/Windows/Fonts/seguisb.ttf")
PALETTE = "split[a][b];[a]palettegen=max_colors=96:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle"


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


def record_pair(name, left, right, seconds, act, labels):
    """
    Records two phones at once while [act] runs and puts them side by side, each with its label on top. [act] gets a
    clock (seconds since the recording started) and returns the stretches to keep, or None for all of it: the
    minute a family's Peri waits for an answer is cut, never played fast.
    """
    if WANTED and name not in WANTED:
        return
    print(f"== {name}", flush=True)
    threads = [threading.Thread(target=adb, args=(p, "shell", "screenrecord", "--size", "540x1200", "--bit-rate", "3000000",
                                                  "--time-limit", str(seconds), f"/sdcard/{name}.mp4"), kwargs={"timeout": seconds + 30})
               for p in (left, right)]
    for t in threads:
        t.start()
    started = time.time() + 0.7
    time.sleep(1)
    keep = act(lambda: time.time() - started)
    for t in threads:
        t.join()
    time.sleep(1)
    mp4s = []
    for i, p in enumerate((left, right)):
        mp4s.append(os.path.join(OUT, f"{name}-{i}.mp4"))
        subprocess.run([ADB, "-s", p, "pull", f"/sdcard/{name}.mp4", mp4s[-1]], capture_output=True, env=ENV)
    cut = ""
    if keep:
        spans = "+".join(f"between(t,{max(0.0, a):.1f},{b:.1f})" for a, b in keep)
        cut = f"select='{spans}',setpts=N/8/TB,"
    half = WIDTH - 30
    sides = []
    for i, label in enumerate(labels):
        gap = ",pad=iw+14:ih:0:0:color=0xFFF8EC" if i == 0 else ""
        sides.append(f"[{i}:v]fps=8,{cut}scale={half}:-1:flags=lanczos,pad=iw:ih+34:0:34:color=0xFFF8EC,"
                     f"drawtext=fontfile='{FONT}':text='{label}':x=(w-text_w)/2:y=8:fontsize=17:fontcolor=0x24461E{gap}[s{i}]")
    graph = ";".join(sides) + f";[s0][s1]hstack=inputs=2,{PALETTE}"
    gif = os.path.join(OUT, f"{name}.gif")
    subprocess.run([FFMPEG, "-y", "-loglevel", "error", "-i", mp4s[0], "-i", mp4s[1], "-filter_complex", graph, gif], check=True)
    for m in mp4s:
        os.remove(m)
    print(f"   {os.path.getsize(gif) // 1024} KB", flush=True)


def sent_to(phone, number):
    """The newest text [phone] sent to [number]: the request Peri texted that guardian, link and all."""
    out = adb(phone, "shell", "content query --uri content://sms/sent --projection address:body --sort 'date DESC'")
    for line in out.splitlines():
        m = re.search(r"address=(\S+), body=(.*)$", line)
        if m and m.group(1).endswith(number):
            return m.group(2)
    return None


def ready(phone=MAMA, moving=True):
    """
    Peri's home, fresh and calm, with no old card in the way. Android can only read a still screen, so the app starts
    with animations off; a clip that taps buttons keeps them off (Peri keeps her poses but doesn't move), the others
    turn them back on and Peri comes alive.
    """
    animations(phone, False)
    fresh(phone)
    for _ in range(3):
        if not tap_text(phone, "Hide|Ocultar|Lovely|Qué bien|¡Qué bien", tries=1):
            break
    if moving:
        animations(phone, True)
    time.sleep(2)


def opening():
    adb(MAMA, "shell", "am", "start", "-n", f"{PKG}/.ui.peri.PeriActivity")
    time.sleep(10)


if not WANTED or "opening" in WANTED:
    ready()
    adb(MAMA, "shell", "am", "force-stop", PKG)
    adb(MAMA, "shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(2)
record("opening", MAMA, 12, opening, skip=1.4)


def new_number():
    sms(MAMA, "447700900123", "Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today for a bill? x")
    time.sleep(16)


if not WANTED or "new-number" in WANTED:
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


if not WANTED or "stranger-call" in WANTED:
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


if not WANTED or "code-during-call" in WANTED:
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


if not WANTED or "safety-pause" in WANTED:
    ready()
record("safety-pause", MAMA, 20, safety_pause)


def website():
    debug(MAMA, "web", "virus-alert.top|Your phone has 5 viruses!|Tap now to clean it or your bank data will be stolen")
    time.sleep(14)


if not WANTED or "website-notice" in WANTED:
    ready()
record("website-notice", MAMA, 15, website)


def remote_app():
    debug(MAMA, "installed", "com.anydesk.anydeskandroid")
    time.sleep(13)


if not WANTED or "remote-app" in WANTED:
    ready()
record("remote-app", MAMA, 14, remote_app)


def first_aid():
    hear(MAMA, "I gave them the code")
    time.sleep(8)
    adb(MAMA, "shell", "input", "swipe", "540", "1700", "540", "900", "800")
    time.sleep(4)


if not WANTED or "first-aid" in WANTED:
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


if not WANTED or "chat" in WANTED:
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


if not WANTED or "trap-card" in WANTED:
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


if not WANTED or "scam-or-not" in WANTED:
    ready(moving=False)
record("scam-or-not", MAMA, 24, game)


def checkup():
    tap_text(MAMA, "Check my phone", tries=2)
    time.sleep(4)
    adb(MAMA, "shell", "input", "swipe", "540", "1800", "540", "700", "900")
    time.sleep(3)
    adb(MAMA, "shell", "input", "swipe", "540", "1800", "540", "700", "900")
    time.sleep(3)


if not WANTED or "checkup" in WANTED:
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


if not WANTED or "playing" in WANTED:
    ready()
record("playing", MAMA, 20, play)


def places(phone):
    """Where each labelled thing on the screen is: {label: [(x, y), …]}, read while it's still."""
    found = {}
    for node in re.finditer(r"<node [^>]*>", dump(phone)):
        label = re.search(r'content-desc="([^"]+)"', node.group(0)) or re.search(r'text="([^"]+)"', node.group(0))
        box = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node.group(0))
        if label and box:
            x1, y1, x2, y2 = map(int, box.groups())
            found.setdefault(unescape(label.group(1)), []).append((str((x1 + x2) // 2), str((y1 + y2) // 2)))
    return found


WARDROBE = {}


def get_dressed():
    # Into her wardrobe from her home, then on one by one: she hops and says what she thinks of each. Last, the look
    # she wears in the other clips, and back home in it.
    adb(MAMA, "shell", "input", "tap", *WARDROBE["Dress Peri"][0])
    time.sleep(4)
    for item in ("Crown", "Heart glasses", "Neckerchief", "Straw hat", "Glasses", "Bow tie"):
        adb(MAMA, "shell", "input", "tap", *WARDROBE[item][0])
        time.sleep(2.6)
    adb(MAMA, "shell", "input", "tap", *WARDROBE["Done"][0])
    time.sleep(4)


if not WANTED or "wardrobe" in WANTED:
    # Undressed first, and every place read while the screens are still; then Peri moves for the clip.
    ready(moving=False)
    WARDROBE.update(places(MAMA))
    adb(MAMA, "shell", "input", "tap", *WARDROBE["Dress Peri"][0])
    time.sleep(4)
    inside = places(MAMA)
    for spot in inside["Nothing"]:
        adb(MAMA, "shell", "input", "tap", *spot)
        time.sleep(1)
    WARDROBE.update(inside)
    ready()
record("wardrobe", MAMA, 38, get_dressed)


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


if not WANTED or "mini-peri" in WANTED:
    ready()
record("mini-peri", MAMA, 22, mini_peri)


# The family, two phones side by side. A guardian's request travels by SMS: the emulators don't pass texts to each
# other, so the very text Mamá's phone sent (the same words and one-time link) is delivered to Papá's through the
# emulator, as every other text in these clips arrives.
def family_approval(clock):
    # Peri doesn't send it: she asks the guardians straight away ("Sending a code is delicate, so I'm asking…").
    hear(MAMA, "tell Ana the code is 482913")
    wait_for(lambda: "approval requested" in logs(MAMA), timeout=30)
    adb(PAPA, "emu", "sms", "send", "15555215556", sent_to(MAMA, "5558"))
    wait_for(lambda: has(screen(PAPA), "te pide permiso"), timeout=40, every=2)
    time.sleep(5)
    # "Call me first": his phone gets her number ready to call, and her Peri sends nothing and offers to call him.
    tap_text(PAPA, "Llamar a Mam", tries=2)
    wait_for(lambda: "approval result=" in logs(MAMA), timeout=30, every=1)
    answered = clock()
    time.sleep(9)
    return [(0, answered + 8)]


if not WANTED or "family-approval" in WANTED:
    ready(PAPA, moving=False)
    # A "was it you?" left from another clip would show first on Papá's phone (his Peri asks the server every half
    # minute): wait for it, answer it, then start clean.
    if wait_for(lambda: has(screen(PAPA), "escrito tú|Was it you"), timeout=40, every=4):
        while tap_text(PAPA, "No he sido yo|No, it wasn", tries=2):
            time.sleep(2)
    ready(PAPA, moving=False)
    ready(MAMA)
record_pair("family-approval", MAMA, PAPA, 60, family_approval, ("Mum (English)", "Dad, her guardian (Spanish)"))


def was_it_you(clock):
    sms(MAMA, "447700900456", "Hi Grandma it's Pablo, new number, my old phone fell in the sea. Text me here")
    # Pablo's Peri checks with the family every half minute: the wait for the question is cut too.
    wait_for(lambda: has(screen(PABLO), "escrito tú|Was it you"), timeout=80, every=2)
    asked = clock()
    time.sleep(3)
    # Any older question still open shows first: answer them all.
    for _ in range(4):
        if not tap_text(PABLO, "No he sido yo|No, it wasn", tries=2):
            break
        time.sleep(2)
    answered = clock()
    # Mamá's Peri says who answered once a minute has passed without a "yes": that minute is cut.
    wait_for(lambda: has(screen(MAMA), "wasn't them"), timeout=110, every=3)
    told = clock()
    time.sleep(7)
    return [(0, min(14, asked - 1)), (asked - 1, answered + 4), (told - 2, told + 7)]


if not WANTED or "was-it-you" in WANTED:
    ready(PABLO, moving=False)
    ready(MAMA, moving=False)
record_pair("was-it-you", MAMA, PABLO, 150, was_it_you, ("Mum (English)", "Pablo, her grandson (Spanish)"))


def hang_up_theirs(clock):
    adb(PABLO, "emu", "gsm", "call", STRANGER)
    time.sleep(3)
    adb(PABLO, "emu", "gsm", "accept", STRANGER)
    answered = clock()
    wait_for(lambda: has(screen(MAMA), "Hang up their call"), timeout=90, every=3)
    shown = clock()
    time.sleep(4)
    tap_text(MAMA, "Hang up their call", tries=2)
    wait_for(lambda: call_state(PABLO) == "0", timeout=25, every=1)
    ended = clock()
    time.sleep(4)
    hang_up(PABLO)
    return [(0, answered + 5), (shown - 2, ended + 4)]


if not WANTED or "hang-up-theirs" in WANTED:
    ready(PABLO)
    ready(MAMA, moving=False)
record_pair("hang-up-theirs", MAMA, PABLO, 120, hang_up_theirs, ("Mum (English)", "Pablo, her grandson (Spanish)"))

# docs/media/screens.png: four screens of the app as it is today, side by side with rounded corners.
def capture(phone, name):
    path = os.path.join(OUT, f"{name}.png")
    open(path, "wb").write(subprocess.run([ADB, "-s", phone, "exec-out", "screencap", "-p"], capture_output=True, timeout=30).stdout)
    return path


if not WANTED or "screens" in WANTED:
    print("== screens", flush=True)
    ready()
    time.sleep(3)
    shots = [capture(MAMA, "screen-home")]
    sms(MAMA, f"6113{int(time.time()) % 100000:05d}", "Correos: su paquete esta retenido, pague 1,99 EUR aqui http://correos-envio.top/p")
    time.sleep(11)
    shots.append(capture(MAMA, "screen-warning"))
    animations(MAMA, False)
    tap_text(MAMA, "Learn", tries=2)
    time.sleep(4)
    shots.append(capture(MAMA, "screen-learn"))
    tap_text(MAMA, "Diary", tries=2)
    time.sleep(4)
    shots.append(capture(MAMA, "screen-diary"))
    animations(MAMA, True)
    # Each phone 300 px wide with 24 px corners, 28 px apart, on a transparent background.
    corner = ("format=rgba,scale=300:-1:flags=lanczos,geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':"
              "a='if(gt(abs(X-W/2),W/2-24)*gt(abs(Y-H/2),H/2-24),if(lte(hypot(abs(X-W/2)-(W/2-24),abs(Y-H/2)-(H/2-24)),24),255,0),255)'")
    parts = [f"[{i}:v]{corner}" + (",pad=iw+28:ih:0:0:color=black@0" if i < 3 else "") + f"[p{i}]" for i in range(4)]
    graph = ";".join(parts) + ";[p0][p1][p2][p3]hstack=inputs=4,format=rgba"
    args = [FFMPEG, "-y", "-loglevel", "error"]
    for shot in shots:
        args += ["-i", shot]
    subprocess.run(args + ["-filter_complex", graph, "-frames:v", "1", os.path.join(ROOT, "docs", "media", "screens.png")], check=True)
    for shot in shots:
        os.remove(shot)
    print(f"   {os.path.getsize(os.path.join(ROOT, 'docs', 'media', 'screens.png')) // 1024} KB", flush=True)

for p in (*PHONES, PAPA):
    animations(p, True)
print("done")
