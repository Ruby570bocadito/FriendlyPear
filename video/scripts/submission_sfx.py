# The submission video's sound effects, made from scratch (no samples): a phone's buzz, a message chime, a ringtone,
# the "calling" tone, Peri's alert, a camera shutter and the clay pops, whooshes and sparkles between scenes.
#   python video/scripts/submission_sfx.py  ->  video/public/submission/sfx/<name>.wav
import wave
from pathlib import Path

import numpy as np

SR = 44100
out = Path(__file__).resolve().parent.parent / "public" / "submission" / "sfx"
out.mkdir(parents=True, exist_ok=True)
rng = np.random.default_rng(7)


def t(seconds):
    return np.arange(int(SR * seconds)) / SR


def env(n, attack=0.004, decay=0.2):
    x = np.arange(n) / SR
    return np.minimum(x / attack, 1) * np.exp(-x / decay)


def tone(hz, seconds, decay=0.25, harmonics=((1, 1.0),)):
    x = t(seconds)
    s = sum(g * np.sin(2 * np.pi * hz * k * x) for k, g in harmonics)
    return s * env(len(x), 0.003, decay)


def place(dest, sig, at, gain=1.0):
    i = int(at * SR)
    j = min(len(dest), i + len(sig))
    dest[i:j] += sig[: j - i] * gain


def save(name, sig, peak=0.9):
    sig = sig / max(1e-9, np.max(np.abs(sig))) * peak
    fade = int(SR * 0.01)
    sig[-fade:] *= np.linspace(1, 0, fade)
    with wave.open(str(out / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((sig * 32767).astype(np.int16).tobytes())


bell = ((1, 1.0), (2.76, 0.25), (5.4, 0.08))

# A phone vibrating twice on a table.
buzz = np.zeros(int(SR * 0.6))
for at in (0.0, 0.3):
    x = t(0.2)
    s = np.sign(np.sin(2 * np.pi * 165 * x)) * (0.6 + 0.4 * np.sin(2 * np.pi * 31 * x))
    s *= np.minimum(np.minimum(x / 0.01, 1), np.minimum((0.2 - x) / 0.02, 1))
    place(buzz, s, at)
buzz = np.convolve(buzz, np.ones(6) / 6, mode="same")
save("buzz", buzz, 0.7)

# A friendly two-note message chime.
ding = np.zeros(int(SR * 0.9))
place(ding, tone(1318.5, 0.6, 0.18, bell), 0.0)
place(ding, tone(1760.0, 0.7, 0.25, bell), 0.12)
save("ding", ding)

# An incoming call's ringtone: a little marimba phrase, twice (about 4 s).
ring = np.zeros(int(SR * 4.2))
phrase = [(1046.5, 0.0), (1318.5, 0.16), (1568.0, 0.32), (1318.5, 0.48), (1568.0, 0.64), (2093.0, 0.8)]
for rep in (0.0, 2.0):
    for hz, at in phrase:
        place(ring, tone(hz, 0.4, 0.12, bell), rep + at, 0.8)
save("ring", ring, 0.75)

# The "calling..." tone heard while a call is placed (two long beeps).
ringback = np.zeros(int(SR * 3.4))
for at in (0.0, 2.0):
    x = t(1.0)
    s = (np.sin(2 * np.pi * 440 * x) + np.sin(2 * np.pi * 480 * x)) * np.minimum(np.minimum(x / 0.02, 1), np.minimum((1.0 - x) / 0.03, 1))
    place(ringback, s, at)
save("ringback", ringback, 0.45)

# A call connects: a soft click and blip.
answer = np.zeros(int(SR * 0.3))
place(answer, tone(880, 0.12, 0.03), 0.0)
place(answer, tone(1320, 0.15, 0.05), 0.06)
save("answer", answer, 0.6)

# Peri's alert for a hard rule: two firm buzzes and a descending two-note chime.
alert = np.zeros(int(SR * 1.3))
place(alert, buzz, 0.0, 0.8)
place(alert, tone(784.0, 0.6, 0.22, bell), 0.05)
place(alert, tone(587.3, 0.8, 0.3, bell), 0.3)
save("alert", alert, 0.85)

# The microphone opens ("I'm all ears"): a little rising bloop.
x = t(0.22)
listen = np.sin(2 * np.pi * np.cumsum(500 + 900 * x / 0.22) / SR) * env(len(x), 0.005, 0.08)
save("listen", listen, 0.6)

# A camera shutter: two clicks of noise.
shutter = np.zeros(int(SR * 0.25))
for at, g in ((0.0, 1.0), (0.09, 0.7)):
    n = rng.standard_normal(int(SR * 0.05))
    n = np.diff(n, prepend=0) * env(len(n), 0.001, 0.012)
    place(shutter, n, at, g)
save("shutter", shutter, 0.7)

# A clay pop, for chips and close-ups.
x = t(0.14)
pop = np.sin(2 * np.pi * np.cumsum(320 + 700 * x / 0.14) / SR) * env(len(x), 0.002, 0.04)
save("pop", pop, 0.6)

# A soft whoosh between scenes.
n = rng.standard_normal(int(SR * 0.7))
n = np.convolve(n, np.ones(24) / 24, mode="same")
x = t(0.7)
save("whoosh", n * np.sin(np.pi * x / 0.7) ** 2, 0.55)

# A stamp landing: a low thump.
x = t(0.35)
thud = np.sin(2 * np.pi * (60 + 120 * np.exp(-x / 0.03)) * x) * np.exp(-x / 0.09)
thud += 0.3 * np.convolve(rng.standard_normal(len(x)), np.ones(40) / 40, mode="same") * np.exp(-x / 0.03)
save("thud", thud, 0.9)

# Sparkles: a quick high arpeggio.
sparkle = np.zeros(int(SR * 1.0))
for i, hz in enumerate((2093.0, 2637.0, 3136.0, 4186.0, 5274.0)):
    place(sparkle, tone(hz, 0.45, 0.15, bell), i * 0.06, 0.5)
save("sparkle", sparkle, 0.5)

# A counter rolling up: quick soft ticks.
ticks = np.zeros(int(SR * 1.3))
for i in range(16):
    place(ticks, tone(1800 + i * 40, 0.04, 0.01), i * 0.075, 0.5)
save("ticks", ticks, 0.4)

print(sorted(p.name for p in out.glob("*.wav")))
