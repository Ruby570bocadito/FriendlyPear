"""
Every feature in docs/FEATURES.md, driven for real on two emulators with the debug build and Peri's server running:
Mamá (5556, English, guardians Ana and Papá) and Pablo (5560, Spanish, paired, sharing Peri's news). Each test makes
the thing happen (an SMS, a call, a voice order through the debug broadcast, a tap) and checks what Peri logged and
what the screen says. Prints a table and saves a screenshot of each test (feat_<name>.png); a crash or ANR fails it.

System animations are switched off while it runs (Peri then stands still, so Android's UI dump can read the screen)
and put back at the end, with each phone's app language.

    python eval/features.py [out_dir] [test ...]
"""
import atexit
import os
import re
import subprocess
import sys
import time

sys.stdout.reconfigure(encoding="utf-8", errors="replace")
ADB = os.environ.get("ADB", "adb")
OUT = sys.argv[1] if len(sys.argv) > 1 else "."
ONLY = set(sys.argv[2:])
PKG = "app.friendlypear"
MAMA = os.environ.get("MAMA", "emulator-5556")
PABLO = os.environ.get("PABLO", "emulator-5560")
PHONES = (MAMA, PABLO)
STRANGER = "699000222"
ENV = {**os.environ, "MSYS_NO_PATHCONV": "1"}
ANIMATIONS = ("animator_duration_scale", "transition_animation_scale", "window_animation_scale")
results = []


def adb(phone, *args, timeout=60):
    try:
        return subprocess.run([ADB, "-s", phone, *args], capture_output=True, timeout=timeout, env=ENV).stdout.decode("utf-8", "replace")
    except subprocess.TimeoutExpired:
        return "TIMEOUT"


def setup():
    saved = {}
    for phone in PHONES:
        saved[phone] = ([adb(phone, "shell", "settings", "get", "global", k).strip() for k in ANIMATIONS],
                        re.search(r"\[(.*)\]", adb(phone, "shell", "cmd", "locale", "get-app-locales", PKG)))
        for k in ANIMATIONS:
            adb(phone, "shell", "settings", "put", "global", k, "0")

    def restore():
        for phone, (scales, locales) in saved.items():
            for k, v in zip(ANIMATIONS, scales):
                adb(phone, "shell", "settings", "put", "global", k, v if v not in ("", "null") else "1")
            if locales is not None:
                adb(phone, "shell", "cmd", "locale", "set-app-locales", PKG, "--locales", locales.group(1) or "")
            hang_up(phone)
    atexit.register(restore)


def logs(phone):
    return "\n".join(l.split(" FriendlyPear: ", 1)[1] for l in adb(phone, "logcat", "-d", "-s", "FriendlyPear").splitlines() if " FriendlyPear: " in l)


def dump(phone):
    """The screen's UI tree, or "" when Android can't take it (never an old file left from before)."""
    adb(phone, "shell", "rm", "-f", "/sdcard/ui.xml")
    for _ in range(3):
        if "dumped" in adb(phone, "shell", "uiautomator", "dump", "/sdcard/ui.xml", timeout=40):
            return adb(phone, "exec-out", "cat", "/sdcard/ui.xml")
        time.sleep(1)
    return ""


def unescape(s):
    return s.replace("&apos;", "'").replace("&quot;", '"').replace("&amp;", "&").replace("&#10;", " ")


def screen(phone):
    """Every text and description on screen, joined: what Peri says in her bubble, the cards, the buttons."""
    return " | ".join(unescape(t) for t in re.findall(r'(?:text|content-desc)="([^"]+)"', dump(phone)) if t.strip())


def tap_text(phone, label, tries=4):
    """Taps the middle of the first element whose text or description starts with [label] (a regex)."""
    want = re.compile(r"(?:" + label + r")", re.I)
    for _ in range(tries):
        for node in re.finditer(r"<node [^>]*>", dump(phone)):
            attrs = node.group(0)
            texts = [unescape(t) for t in re.findall(r'(?:text|content-desc)="([^"]*)"', attrs)]
            box = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', attrs)
            if box and any(want.match(t) for t in texts):
                x1, y1, x2, y2 = map(int, box.groups())
                adb(phone, "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                return True
        time.sleep(1.5)
    return False


def tap_class(phone, cls):
    m = re.search(r'class="' + re.escape(cls) + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', dump(phone))
    if not m:
        return False
    x1, y1, x2, y2 = map(int, m.groups())
    adb(phone, "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    return True


def shot(phone, name):
    data = subprocess.run([ADB, "-s", phone, "exec-out", "screencap", "-p"], capture_output=True, timeout=30).stdout
    open(os.path.join(OUT, f"feat_{name}.png"), "wb").write(data)


def hear(phone, text):
    adb(phone, "shell", f"am broadcast -a {PKG}.debug.HEAR --es text '{text}'")


def debug(phone, key, value):
    adb(phone, "shell", f"am broadcast -a {PKG}.debug.HEAR --es {key} '{value}'")


def sms(phone, sender, text):
    adb(phone, "emu", "sms", "send", sender, text)


def call_state(phone):
    m = re.search(r"mCallState=(\d)", adb(phone, "shell", "dumpsys", "telephony.registry"))
    return m.group(1) if m else "?"


def hang_up(phone):
    for _ in range(4):
        if call_state(phone) == "0":
            return
        adb(phone, "shell", "input", "keyevent", "KEYCODE_ENDCALL")
        time.sleep(1)


def fresh(phone, activity=".ui.peri.PeriActivity"):
    hang_up(phone)
    adb(phone, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(phone, "shell", "am", "force-stop", "com.android.chrome")
    adb(phone, "shell", "am", "force-stop", PKG)
    adb(phone, "logcat", "-c")
    adb(phone, "shell", "am", "start", "-n", f"{PKG}/{activity}")
    time.sleep(7)
    # A new badge can sit over the screen: close it.
    tap_text(phone, "Lovely|Qué bien|¡Qué bien", tries=1)


def wait_for(fn, timeout=25, every=2):
    end = time.time() + timeout
    while True:
        got = fn()
        if got or time.time() > end:
            return got
        time.sleep(every)


def verdict(phone, channel="SMS"):
    """The message's level once Peri settled: the rules', or the models' when the rules left it doubtful."""
    lg = logs(phone)
    m = re.search(rf"message channel={channel} level=(\w+) hardRule=(\w+)", lg)
    if not m:
        return None
    if m.group(1) == "DOUBT":
        later = re.search(r"second opinion [^\n]*level=(\w+)", lg[m.end():])
        return (later.group(1) if later else "DOUBT"), m.group(2)
    return m.group(1), m.group(2)


def crashed(phone):
    crash = adb(phone, "logcat", "-d", "-b", "crash")
    anr = [l for l in adb(phone, "logcat", "-d", "-b", "system").splitlines() if "ANR in" in l and PKG in l]
    return (crash.strip() and crash.strip().splitlines()[:6]) or anr


def test(name, phones=(MAMA,)):
    def wrap(fn):
        if ONLY and name not in ONLY:
            return fn
        print(f"\n== {name}", flush=True)
        try:
            ok, detail = fn()
        except Exception as e:  # noqa: BLE001 (a broken test is a failed test, the run goes on)
            ok, detail = False, f"{type(e).__name__}: {e}"
        for p in phones:
            bad = crashed(p)
            if bad:
                ok, detail = False, f"CRASH on {p}: {bad}"
        shot(phones[0], name)
        print(("ok   " if ok else "FAIL ") + str(detail)[:500], flush=True)
        results.append((name, ok, str(detail)[:170]))
        return fn
    return wrap


def has(text, *patterns):
    return all(re.search(p, text, re.I | re.S) for p in patterns)


setup()

# ---------------------------------------------------------------- 1. Protection

@test("sms_lookalike_brand")
def _():
    fresh(MAMA)
    sms(MAMA, "611222333", "Correos: su paquete esta retenido, pague 1,99 EUR aqui http://correos-envio.top/p")
    got = wait_for(lambda: verdict(MAMA))
    return got == ("SCAM", "BRAND_LOOKALIKE"), got


@test("sms_from_family_is_quiet")
def _():
    fresh(MAMA)
    sms(MAMA, "15555215554", "Hi Mum, are we still on for Sunday lunch? I'll bring dessert")
    got = wait_for(lambda: verdict(MAMA))
    return bool(got) and got[0] == "OK", got


@test("sms_new_number_offers_real_daughter")
def _():
    fresh(MAMA)
    sms(MAMA, "447700900123", "Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today for a bill? x")
    # Doubtful by the rules alone: Peri speaks once the second opinion is in.
    wait_for(lambda: "second opinion" in logs(MAMA) or (verdict(MAMA) or ("",))[0] == "SCAM", timeout=40)
    # She offers to call Ana's saved number and listens: one "yes" and the phone calls her (5554), not the sender.
    hear(MAMA, "yes")
    dialing = wait_for(lambda: call_state(MAMA) == "2", timeout=12)
    # Android hides the numbers in its call log dump; the call screen says who it's calling.
    time.sleep(2)
    calling = screen(MAMA)
    got = verdict(MAMA)
    hang_up(MAMA)
    return bool(got) and got[0] != "OK" and bool(dialing) and has(calling, "Ana") and "447700" not in calling.replace(" ", ""), f"{got} dialing={bool(dialing)} · {calling[:120]}"


@test("sms_insecure_link_from_contact")
def _():
    fresh(MAMA)
    sms(MAMA, "15555215560", "Grandma here are the wedding photos http://fotos-boda.example.com/album")
    wait_for(lambda: verdict(MAMA))
    time.sleep(12)
    got = verdict(MAMA)
    s = screen(MAMA)
    return bool(got) and got[0] != "OK" and has(s, "padlock"), f"{got} · {s[:200]}"


@test("sms_code_without_call_is_quiet")
def _():
    fresh(MAMA)
    sms(MAMA, "22395", "Your Google verification code is 482913. Don't share it.")
    got = wait_for(lambda: verdict(MAMA))
    return bool(got) and got[0] != "SCAM" and got[1] == "null", got


@test("fake_peri_text")
def _():
    fresh(MAMA)
    sms(MAMA, "611000999", "Peri here! Your family needs you to confirm your account at http://peri-familia.top/ok")
    got = wait_for(lambda: verdict(MAMA))
    return bool(got) and got[0] == "SCAM", got


@test("email_scam")
def _():
    fresh(MAMA)
    debug(MAMA, "email", "Netflix <billing@netflix-help.top>|Your payment failed. Update your card in 24 hours at http://netflix-help.top or lose your account")
    wait_for(lambda: verdict(MAMA, "EMAIL"))
    time.sleep(10)
    got = verdict(MAMA, "EMAIL")
    return bool(got) and got[0] != "OK", got


@test("website_scare_notice")
def _():
    fresh(MAMA)
    debug(MAMA, "web", "virus-alert.top|Your phone has 5 viruses!|Tap now to clean it or your bank data will be stolen")
    wait_for(lambda: verdict(MAMA, "BROWSER_NOTIFICATION"))
    time.sleep(10)
    got = verdict(MAMA, "BROWSER_NOTIFICATION")
    s = screen(MAMA)
    return bool(got) and got[0] != "OK", f"{got} · {s[:200]}"


@test("stranger_call_before_and_after")
def _():
    fresh(MAMA)
    adb(MAMA, "emu", "gsm", "call", STRANGER)
    screened = wait_for(lambda: "incoming call screened" in logs(MAMA), timeout=15)
    time.sleep(4)
    before = screen(MAMA)
    adb(MAMA, "emu", "gsm", "accept", STRANGER)
    time.sleep(6)
    adb(MAMA, "emu", "gsm", "cancel", STRANGER)
    time.sleep(9)
    asked = screen(MAMA)
    hear(MAMA, "the bank")
    time.sleep(8)
    hear(MAMA, "they wanted the code they texted me")
    time.sleep(9)
    after = screen(MAMA)
    return bool(screened) and has(asked, "who|they say") and has(after, "code"), f"screened={bool(screened)} · ringing: {before[:120]} · asked: {asked[:160]} · after: {after[:200]}"


@test("code_during_call")
def _():
    fresh(MAMA)
    adb(MAMA, "emu", "gsm", "call", "911000222")
    time.sleep(4)
    adb(MAMA, "emu", "gsm", "accept", "911000222")
    time.sleep(4)
    sms(MAMA, "22395", "Your bank verification code is 482913. Never share it with anyone.")
    got = wait_for(lambda: verdict(MAMA))
    time.sleep(3)
    s = screen(MAMA)
    adb(MAMA, "emu", "gsm", "cancel", "911000222")
    return got == ("SCAM", "CODE_DURING_CALL") and "482913" not in s, f"{got} · code on screen: {'482913' in s} · {s[:160]}"


@test("safety_pause_bank_during_call")
def _():
    fresh(MAMA)
    adb(MAMA, "emu", "gsm", "call", STRANGER)
    time.sleep(4)
    adb(MAMA, "emu", "gsm", "accept", STRANGER)
    time.sleep(4)
    debug(MAMA, "opened", "com.bbva.bbvacontigo")
    time.sleep(5)
    s = screen(MAMA)
    shot(MAMA, "safety_pause_screen")
    left = tap_text(MAMA, "Leave this app", tries=2)
    answered = wait_for(lambda: re.search(r"safety pause answer=\w+", logs(MAMA)), timeout=8)
    adb(MAMA, "emu", "gsm", "cancel", STRANGER)
    return has(s, "Wait a moment") and has(s, "Hang up") and bool(answered), f"left={left} {answered and answered.group(0)} · {s[:220]}"


@test("remote_app_installed")
def _():
    fresh(MAMA)
    debug(MAMA, "installed", "com.anydesk.anydeskandroid")
    time.sleep(6)
    s = screen(MAMA)
    return has(s, "AnyDesk"), s[:240]


@test("screen_control_app")
def _():
    fresh(MAMA)
    debug(MAMA, "intruder", "com.evil.helper|Helper")
    got = wait_for(lambda: "an app can control the screen" in logs(MAMA), timeout=10)
    time.sleep(3)
    s = screen(MAMA)
    return bool(got) and has(s, "Helper"), s[:240]


@test("sim_swap")
def _():
    fresh(MAMA)
    debug(MAMA, "sim", "new")
    got = wait_for(lambda: "line: NEW_SIM" in logs(MAMA), timeout=10)
    time.sleep(4)
    s = screen(MAMA)
    debug(MAMA, "sim", "back")
    return bool(got) and has(s, "bank|phone company|SIM"), s[:240]


@test("first_aid_after_scam")
def _():
    fresh(MAMA)
    hear(MAMA, "I gave them the code")
    time.sleep(7)
    s = screen(MAMA)
    return has(s, "step|WhatsApp|bank|card"), s[:240]


@test("chat_answers_and_checks_pasted_message")
def _():
    fresh(MAMA)
    opened = tap_text(MAMA, "Write to Peri", tries=2)
    time.sleep(3)
    s0 = screen(MAMA)
    typed = tap_class(MAMA, "android.widget.EditText")
    adb(MAMA, "shell", "input", "text", "I%sgot%sthis:%sYour%sparcel%sis%sheld,%spay%s1.99%sat%shttp://correos-envio.top")
    time.sleep(1)
    sent = tap_text(MAMA, "Send", tries=2)
    got = wait_for(lambda: re.search(r"chat check level=(\w+)", logs(MAMA)), timeout=15)
    time.sleep(3)
    s = screen(MAMA)
    return bool(got) and got.group(1) == "SCAM", f"opened={opened} typed={typed} sent={sent} {got and got.group(0)} · {s[-220:]} · offered: {s0[:120]}"


@test("block_scam_number")
def _():
    fresh(MAMA)
    sms(MAMA, "611777888", "Correos: pague 1,99 EUR en http://correos-envio.top/p ahora")
    wait_for(lambda: verdict(MAMA))
    time.sleep(6)
    blocked = tap_text(MAMA, "Block this number", tries=3)
    time.sleep(3)
    said = screen(MAMA)
    sms(MAMA, "611777888", "Ultimo aviso: pague hoy en http://correos-envio.top/p")
    got = wait_for(lambda: "message from a blocked sender silenced" in logs(MAMA), timeout=15)
    return blocked and bool(got), f"tapped={blocked} silenced={bool(got)} · {said[:200]}"


@test("report_for_police")
def _():
    fresh(MAMA)
    sms(MAMA, "611222334", "Correos: su paquete esta retenido, pague 1,99 EUR aqui http://correos-envio.top/p")
    wait_for(lambda: verdict(MAMA))
    time.sleep(6)
    opened = tap_text(MAMA, "Report it", tries=3)
    time.sleep(3)
    wrote = tap_text(MAMA, "Write it", tries=3)
    got = wait_for(lambda: re.search(r"report written=(\w+)", logs(MAMA)), timeout=90, every=3)
    time.sleep(2)
    s = screen(MAMA)
    return bool(got) and got.group(1) == "true", f"opened={opened} write={wrote} {got and got.group(0)} · {s[:200]}"


# ---------------------------------------------------------------- 2. Voice, family help

@test("voice_call_by_name")
def _():
    fresh(MAMA)
    hear(MAMA, "call Pablo")
    dialing = wait_for(lambda: call_state(MAMA) == "2", timeout=10)
    s = screen(MAMA)
    if not dialing:
        hear(MAMA, "yes")
        dialing = wait_for(lambda: call_state(MAMA) == "2", timeout=10)
    hang_up(MAMA)
    return bool(dialing), f"dialing={bool(dialing)} · {s[:200]}"


@test("voice_call_by_nickname")
def _():
    fresh(MAMA)
    hear(MAMA, "call Pablito")
    dialing = wait_for(lambda: call_state(MAMA) == "2", timeout=10)
    s = screen(MAMA)
    if not dialing:
        hear(MAMA, "yes")
        dialing = wait_for(lambda: call_state(MAMA) == "2", timeout=10)
    hang_up(MAMA)
    return bool(dialing), f"dialing={bool(dialing)} · {s[:200]}"


@test("ask_for_help")
def _():
    fresh(MAMA)
    hear(MAMA, "call my daughter, I need help")
    dialing = wait_for(lambda: call_state(MAMA) == "2", timeout=10)
    s = screen(MAMA)
    if not dialing:
        hear(MAMA, "yes")
        dialing = wait_for(lambda: call_state(MAMA) == "2", timeout=10)
    hang_up(MAMA)
    return bool(dialing), f"dialing={bool(dialing)} · {s[:200]}"


@test("emergency_countdown_cancelled")
def _():
    fresh(MAMA)
    hear(MAMA, "I have fallen")
    time.sleep(3)
    s = screen(MAMA)
    hear(MAMA, "I am fine")
    time.sleep(14)
    after = screen(MAMA)
    called = call_state(MAMA) == "2"
    hang_up(MAMA)
    return not called and has(after, "glad|fine|cancel|good"), f"called={called} · countdown: {s[:150]} · after: {after[:150]}"


@test("family_approval_for_a_code")
def _():
    fresh(MAMA)
    hear(MAMA, "tell Ana the code is 482913")
    time.sleep(4)
    hear(MAMA, "yes")
    got = wait_for(lambda: re.search(r"approval requested[^\n]*", logs(MAMA)), timeout=20)
    s = screen(MAMA)
    return bool(got), f"{got and got.group(0)} · {s[:200]}"


@test("weekly_summary")
def _():
    fresh(MAMA)
    debug(MAMA, "summary", "now")
    got = wait_for(lambda: re.search(r"weekly summary sent to \d+ of \d+[^\n]*", logs(MAMA)), timeout=15)
    return bool(got), got and got.group(0)


# ---------------------------------------------------------------- 3. Two phones

@test("family_pears_both_ways", phones=(MAMA, PABLO))
def _():
    fresh(PABLO)
    fresh(MAMA)
    a = wait_for(lambda: "pear published=true" in logs(MAMA), timeout=25)
    b = wait_for(lambda: "pear published=true" in logs(PABLO), timeout=25)
    tap_text(MAMA, "Family")
    time.sleep(5)
    s = screen(MAMA)
    return bool(a) and bool(b) and has(s, "Pablo"), f"Mamá published={bool(a)} Pablo published={bool(b)} · {s[:220]}"


@test("pear_attention_reaches_family", phones=(MAMA, PABLO))
def _():
    fresh(MAMA)
    fresh(PABLO)
    sms(PABLO, "611222335", "Correos: su paquete esta retenido, pague 1,99 EUR aqui http://correos-envio.top/p")
    att = wait_for(lambda: "state=attention" in logs(PABLO), timeout=25)
    seen = wait_for(lambda: has(screen(MAMA), "Pablo"), timeout=75, every=5)
    return bool(att) and bool(seen), f"Pablo attention={bool(att)} · Mamá: {screen(MAMA)[:220]}"


@test("was_it_you", phones=(MAMA, PABLO))
def _():
    fresh(PABLO)
    fresh(MAMA)
    sms(MAMA, "447700900456", "Hi Grandma it's Pablo, new number, my old phone fell in the sea. Text me here")
    asked = wait_for(lambda: has(screen(PABLO), "escrito tú|Was it you|has sido"), timeout=90, every=5)
    s = screen(PABLO)
    # Any older question still open shows first: answer them all (the server keeps each one 15 minutes).
    answered = False
    for _ in range(4):
        if not tap_text(PABLO, "No he sido yo|No, it wasn", tries=2):
            break
        answered = True
        time.sleep(3)
    told = wait_for(lambda: has(screen(MAMA), "wasn't them|It's a trap"), timeout=90, every=5)
    m = screen(MAMA)
    return bool(asked) and answered and bool(told), f"asked={bool(asked)} answered={answered} told={bool(told)} · Pablo: {s[:140]} · Mamá: {m[:160]}"


@test("same_trap_whole_family", phones=(MAMA, PABLO))
def _():
    fresh(MAMA)
    fresh(PABLO)
    sms(MAMA, "611222336", "Correos: su paquete esta retenido, pague 1,99 EUR aqui http://correos-envio.top/p")
    sms(PABLO, "611222337", "Correos: su paquete esta retenido, pague 2,99 EUR aqui http://correos-pago.top/p")
    seen = wait_for(lambda: has(screen(MAMA), "reaching your family|reaching several|same trap"), timeout=90, every=5)
    return bool(seen), screen(MAMA)[:240]


@test("family_hangs_up_a_stranger", phones=(MAMA, PABLO))
def _():
    fresh(MAMA)
    fresh(PABLO)
    adb(PABLO, "emu", "gsm", "call", STRANGER)
    time.sleep(4)
    adb(PABLO, "emu", "gsm", "accept", STRANGER)
    tapped = wait_for(lambda: tap_text(MAMA, "Hang up their call", tries=1), timeout=90, every=5)
    asked = wait_for(lambda: "a guardian asked to hang up" in logs(PABLO), timeout=60, every=3)
    ended = wait_for(lambda: call_state(PABLO) == "0", timeout=20)
    hang_up(PABLO)
    return bool(tapped) and bool(asked) and bool(ended), f"tapped={bool(tapped)} asked={bool(asked)} ended={bool(ended)}"


@test("blocks_shared_with_family", phones=(MAMA, PABLO))
def _():
    fresh(PABLO)
    time.sleep(3)
    before = re.findall(r"blocks shared=\w+ mine=\d+ family=(\d+)", logs(PABLO))
    fresh(MAMA)
    sms(MAMA, "611888999", "Ha ganado un iPhone! Reclamelo en http://premio-ya.xyz antes de hoy")
    wait_for(lambda: verdict(MAMA))
    time.sleep(6)
    tapped = tap_text(MAMA, "Block this number", tries=3)
    time.sleep(6)
    fresh(PABLO)
    after = wait_for(lambda: re.findall(r"blocks shared=\w+ mine=\d+ family=(\d+)", logs(PABLO)), timeout=60, every=5)
    grew = bool(after) and (not before or int(after[-1]) > int(before[-1]))
    return tapped and grew, f"tapped={tapped} · Pablo's family prints {before} → {after}"


# ---------------------------------------------------------------- 4. Screens

@test("diary_learn_family_tabs")
def _():
    fresh(MAMA)
    seen = {}
    for tab, want in (("Diary", "Today|day|week"), ("Learn", "Scam or not|trap"), ("Family", "guardian|Family")):
        tap_text(MAMA, tab)
        time.sleep(3)
        seen[tab] = has(screen(MAMA), want)
    return all(seen.values()), seen


def home(phone=MAMA):
    """Peri's home with her buttons showing: any card (setup step, trap, website) hidden first."""
    fresh(phone)
    for _ in range(3):
        if not tap_text(phone, "Hide|Ocultar", tries=1):
            break
        time.sleep(1)


def tap_scrolling(phone, label, swipes=6):
    """Taps [label], scrolling down the screen until it shows."""
    for _ in range(swipes):
        if tap_text(phone, label, tries=1):
            return True
        adb(phone, "shell", "input", "swipe", "540", "1800", "540", "800", "500")
        time.sleep(1)
    return False


@test("scam_or_not_game")
def _():
    home()
    tap_text(MAMA, "Scam or not")
    time.sleep(3)
    answers = 0
    for _ in range(6):
        if tap_text(MAMA, "It's a trap", tries=2):
            answers += 1
        time.sleep(3)
        tap_text(MAMA, "Next", tries=1)
        time.sleep(2)
    s = screen(MAMA)
    return answers >= 5, f"{answers} answered · {s[:200]}"


@test("trap_comics")
def _():
    fresh(MAMA)
    tap_text(MAMA, "Learn")
    time.sleep(3)
    opened = tap_text(MAMA, "The held parcel")
    time.sleep(3)
    s = screen(MAMA)
    return opened and has(s, "parcel") and not has(s, "Traps I know"), s[:220]


@test("security_checkup")
def _():
    home()
    tap_text(MAMA, "Check my phone")
    time.sleep(5)
    s = screen(MAMA)
    return has(s, "Security checkup") and has(s, "in order") and has(s, "lock"), s[:240]


@test("what_peri_can_do")
def _():
    fresh(MAMA)
    tap_text(MAMA, "Protecting you")
    time.sleep(4)
    s = screen(MAMA)
    return has(s, "What Peri can do") and has(s, "contacts"), s[:240]


@test("settings")
def _():
    fresh(MAMA)
    tap_text(MAMA, "Settings")
    time.sleep(4)
    s = screen(MAMA)
    lang = tap_scrolling(MAMA, "Language|English")
    return has(s, "voice|Voice|Peri") and lang, s[:240]


@test("wardrobe")
def _():
    fresh(MAMA)
    tap_text(MAMA, "Dress Peri")
    time.sleep(4)
    s = screen(MAMA)
    surprise = tap_text(MAMA, "Surprise me", tries=2)
    return surprise, s[:200]


@test("welcome_pages")
def _():
    fresh(MAMA)
    tap_text(MAMA, "Settings")
    time.sleep(4)
    opened = tap_scrolling(MAMA, "See Peri's welcome again", swipes=10)
    time.sleep(4)
    pages = 0
    for _ in range(6):
        if tap_text(MAMA, "Next", tries=1):
            pages += 1
        time.sleep(2)
    adb(MAMA, "shell", "input", "keyevent", "KEYCODE_BACK")
    return opened and pages >= 4, f"opened={opened} {pages} pages"


@test("playing_with_peri")
def _():
    fresh(MAMA)
    lines = []
    for x, y in ((540, 500), (540, 330), (540, 640), (540, 520)):
        adb(MAMA, "shell", "input", "tap", str(x), str(y))
        time.sleep(3)
        bubble = re.findall(r'text="([^"]{25,})"', dump(MAMA))
        lines.append(bubble[0] if bubble else "")
    return len(set(lines)) >= 2, f"{len(set(lines))} different bubbles: {[l[:40] for l in lines]}"


@test("mini_peri_over_other_apps")
def _():
    fresh(MAMA)
    adb(MAMA, "shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(2)
    adb(MAMA, "emu", "gsm", "call", STRANGER)
    time.sleep(3)
    # Mini Peri comes out beside a stranger's call once it's picked up.
    adb(MAMA, "emu", "gsm", "accept", STRANGER)
    time.sleep(6)
    # Each window's block starts with "Window #"; Mini Peri is one of the app's drawn over the others.
    blocks = adb(MAMA, "shell", "dumpsys", "window", "windows").split("Window #")
    overlay = any(PKG in b and "APPLICATION_OVERLAY" in b for b in blocks)
    adb(MAMA, "emu", "gsm", "cancel", STRANGER)
    return overlay, f"overlay={overlay}"


# ---------------------------------------------------------------- report

print("\n\nRESULT")
for name, ok, detail in results:
    print(f"{'ok  ' if ok else 'FAIL'}  {name:40s} {detail}")
print(f"\n{sum(ok for _, ok, _ in results)}/{len(results)} passed")
sys.exit(0 if all(ok for _, ok, _ in results) else 1)
