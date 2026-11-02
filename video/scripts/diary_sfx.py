# The sound effects of "Peri's diary", made from scratch (no samples) but built like the real things, in layers: a
# rubber stamp is the stamp's knock, the paper's slap, the table's thump and the little peel as it lifts; a page turn is
# air and crackling paper landing with a flap; a laptop's keys are a click and a thock each, never evenly spaced; a
# camera shutter rings like metal; a phone's tones are heard through its little speaker; chimes are struck bars. Every
# effect is brought to the same loudness, so the video sets their levels against the voices on one scale.
#   python video/scripts/diary_sfx.py  ->  video/public/diary/sfx/<name>.wav
import wave
from pathlib import Path

import numpy as np

SR = 44100
rng = np.random.default_rng(31)
out = Path(__file__).resolve().parent.parent / "public" / "diary" / "sfx"
out.mkdir(parents=True, exist_ok=True)


def t(seconds):
    return np.arange(int(SR * seconds)) / SR


def noise(seconds):
    return rng.standard_normal(int(SR * seconds))


def env(n, attack=0.002, decay=0.1):
    x = np.arange(n) / SR
    return np.minimum(x / max(attack, 1e-5), 1) * np.exp(-x / decay)


def biquad(x, kind, f0, q=0.707):
    """One RBJ-cookbook filter: low-pass, high-pass or band-pass (0 dB at its peak)."""
    w0 = 2 * np.pi * f0 / SR
    alpha, cos = np.sin(w0) / (2 * q), np.cos(w0)
    if kind == "lp":
        b = [(1 - cos) / 2, 1 - cos, (1 - cos) / 2]
    elif kind == "hp":
        b = [(1 + cos) / 2, -(1 + cos), (1 + cos) / 2]
    else:
        b = [alpha, 0.0, -alpha]
    a = [1 + alpha, -2 * cos, 1 - alpha]
    b0, b1, b2 = (v / a[0] for v in b)
    a1, a2 = a[1] / a[0], a[2] / a[0]
    y = np.zeros(len(x))
    x1 = x2 = y1 = y2 = 0.0
    for i, xi in enumerate(x):
        yi = b0 * xi + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, xi, y1, yi
        y[i] = yi
    return y


def put(dest, sig, at, gain=1.0):
    i = int(at * SR)
    if i >= len(dest):
        return
    j = min(len(dest), i + len(sig))
    dest[i:j] += sig[: j - i] * gain


def bar(hz, seconds=1.0, ring=0.5, gain=1.0):
    """A struck bar (marimba, glockenspiel): its partials die fast, the fundamental rings; a soft mallet click."""
    x = t(seconds)
    s = np.sin(2 * np.pi * hz * x) * np.exp(-x / ring)
    s += 0.28 * np.sin(2 * np.pi * hz * 3.93 * x) * np.exp(-x / 0.09)
    s += 0.08 * np.sin(2 * np.pi * hz * 9.2 * x) * np.exp(-x / 0.03)
    s *= np.minimum(x / 0.0015, 1)
    put(s, biquad(noise(0.006) * env(int(SR * 0.006), 0.0003, 0.0015), "hp", 2500), 0, 0.15)
    return s * gain


def save(name, sig, rms_db=-18.0):
    """Every effect at the same loudness (RMS of its loud part), peaks under -1 dBFS, with a tiny fade at the end."""
    sig = np.asarray(sig, dtype=np.float64)
    loud = sig[np.abs(sig) > 0.1 * np.abs(sig).max()]
    sig = sig * 10 ** (rms_db / 20) / max(np.sqrt(np.mean(loud ** 2)), 1e-9)
    sig *= min(1.0, 0.89 / np.abs(sig).max())
    fade = int(SR * 0.008)
    sig[-fade:] *= np.linspace(1, 0, fade)
    with wave.open(str(out / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((sig * 32767).astype(np.int16).tobytes())


def phone_speaker(sig):
    """As heard from a phone's little speaker or earpiece: no lows, no highs."""
    return biquad(biquad(sig, "hp", 350), "lp", 3600)


# A rubber stamp: the knock of the stamp, the paper's slap, the table's thump, and the little peel as it lifts.
def stamp(weight=1.0):
    s = np.zeros(int(SR * 0.42))
    n = noise(0.08)
    put(s, biquad(n * env(len(n), 0.0008, 0.03), "bp", 190, 2.2) * 3.2 * weight, 0)
    put(s, biquad(n * env(len(n), 0.0005, 0.016), "bp", 470, 2.8) * 1.6, 0)
    put(s, biquad(n * env(len(n), 0.0003, 0.009), "hp", 1800) * 0.9, 0)
    x = t(0.25)
    put(s, np.sin(2 * np.pi * (70 + 70 * np.exp(-x / 0.008)) * x) * np.exp(-x / 0.06) * 0.7 * weight, 0)
    peel = noise(0.07) * np.hanning(int(SR * 0.07))
    put(s, biquad(peel, "bp", 3200, 1.0) * 0.18, 0.17)
    return s


save("stamp", stamp())
save("slam", stamp(1.6), -17.0)

# A photo dealt onto the pile: a quick papery swish, its corner tapping down, a short slide.
flick = np.zeros(int(SR * 0.26))
sw = noise(0.09) * np.sin(np.linspace(0, np.pi, int(SR * 0.09))) ** 2
put(flick, biquad(sw, "bp", 2600, 0.8) * 0.9, 0)
n = noise(0.04)
put(flick, biquad(n * env(len(n), 0.0004, 0.005), "bp", 2800, 1.4) * 1.4, 0.085)
put(flick, biquad(n * env(len(n), 0.0008, 0.018), "bp", 750, 2.0) * 1.1, 0.085)
slide = noise(0.12)
put(flick, biquad(slide * env(len(slide), 0.005, 0.04), "lp", 5000) * 0.18, 0.095)
save("flick", flick, -21.0)

# A page turning: air moving, paper crackling as it bends, and a soft flap as it lands.
dur = 0.66
page = np.zeros(int(SR * dur))
x = t(0.52)
shape = np.sin(np.pi * np.clip(x / 0.52, 0, 1)) ** 1.6
put(page, biquad(noise(0.52), "bp", 1500, 0.55) * shape * 0.8, 0)
sparks = (rng.random(len(x)) < 0.006) * rng.standard_normal(len(x)) * 6
put(page, biquad(sparks, "hp", 2400) * shape * 0.55, 0)
n = noise(0.08)
put(page, biquad(n * env(len(n), 0.001, 0.028), "bp", 850, 1.2) * 1.2, 0.5)
put(page, biquad(n * env(len(n), 0.0005, 0.008), "hp", 2500) * 0.5, 0.5)
save("page", page, -20.0)

# A laptop keyboard: each key a click and a thock, never evenly spaced, a space bar now and then.
typing = np.zeros(int(SR * 1.7))
at, k = 0.04, 0
while at < 1.6:
    v = rng.uniform(0.6, 1.0)
    n = noise(0.045)
    space = k % 6 == 5
    put(typing, biquad(n * env(len(n), 0.0003, 0.003), "bp", rng.uniform(3600, 5400), 1.5) * v, at)
    put(typing, biquad(n * env(len(n), 0.001, 0.016 if space else 0.011), "bp", 320 if space else rng.uniform(480, 820), 2.0) * v * (1.2 if space else 0.8), at)
    at += rng.uniform(0.07, 0.16) + (0.22 if rng.random() < 0.12 else 0)
    k += 1
save("typing", typing, -21.0)

# A message sent: a little swoosh up and a bubbly pop.
send = np.zeros(int(SR * 0.32))
for i, fc in enumerate((700, 1200, 2000, 3200)):
    n = noise(0.06) * np.hanning(int(SR * 0.06))
    put(send, biquad(n, "bp", fc, 1.2) * 0.6, i * 0.035)
x = t(0.06)
put(send, np.sin(2 * np.pi * np.cumsum(700 + 900 * x / 0.06) / SR) * env(len(x), 0.002, 0.025), 0.17)
save("send", send, -20.0)

# A message arrives: two notes on a little glockenspiel.
ding = np.zeros(int(SR * 1.0))
put(ding, bar(1318.5, 0.8, 0.35), 0.0)
put(ding, bar(1760.0, 0.9, 0.45), 0.12)
save("ding", phone_speaker(ding) * 0.6 + ding * 0.4, -19.0)

# An incoming call: a marimba phrase, twice, from the phone's speaker.
ring = np.zeros(int(SR * 4.2))
for rep in (0.0, 2.0):
    for hz, at in ((1046.5, 0.0), (1318.5, 0.16), (1568.0, 0.32), (1318.5, 0.48), (1568.0, 0.64), (2093.0, 0.8)):
        put(ring, bar(hz, 0.45, 0.14), rep + at, 0.8)
save("ring", phone_speaker(ring), -19.0)

# Calling: the ringback tone in the earpiece, with the faint hiss of an open line.
ringback = biquad(noise(3.4), "lp", 2500) * 0.02
for at in (0.0, 2.0):
    x = t(1.0)
    put(ringback, (np.sin(2 * np.pi * 440 * x) + np.sin(2 * np.pi * 480 * x)) * np.minimum(np.minimum(x / 0.02, 1), np.minimum((1.0 - x) / 0.03, 1)) * 0.5, at)
save("ringback", phone_speaker(ringback), -22.0)

# The call is answered: a click, the line opening, a soft two-note blip.
answer = np.zeros(int(SR * 0.4))
n = noise(0.01)
put(answer, biquad(n * env(len(n), 0.0002, 0.002), "hp", 1500), 0)
hiss = noise(0.35) * np.minimum(t(0.35) / 0.15, 1)
put(answer, biquad(hiss, "lp", 3000) * 0.04, 0.02)
put(answer, bar(880, 0.2, 0.05, 0.5), 0.03)
put(answer, bar(1320, 0.25, 0.07, 0.5), 0.09)
save("answer", phone_speaker(answer), -21.0)

# Peri's warning: two firm buzzes of the phone on the table, and a calm descending chime.
alert = np.zeros(int(SR * 1.3))
for at in (0.0, 0.28):
    x = t(0.18)
    buzz = np.sign(np.sin(2 * np.pi * 160 * x)) * (0.7 + 0.3 * np.sin(2 * np.pi * 37 * x))
    buzz *= np.minimum(np.minimum(x / 0.01, 1), np.minimum((0.18 - x) / 0.02, 1))
    put(alert, biquad(buzz, "lp", 700) * 0.8, at)
put(alert, bar(784.0, 0.7, 0.25), 0.05)
put(alert, bar(587.3, 0.9, 0.32), 0.3)
save("alert", alert, -18.0)

# A camera shutter: two metallic clicks, the blades' ring and a little body.
shutter = np.zeros(int(SR * 0.22))
for at, g in ((0.0, 1.0), (0.075, 0.75)):
    x = t(0.06)
    ring_ = sum(np.sin(2 * np.pi * f * x) * np.exp(-x / d) for f, d in ((2300, 0.012), (4100, 0.008), (6900, 0.005))) * 0.5
    n = noise(0.06)
    click = biquad(n * env(len(n), 0.0002, 0.0025), "hp", 3000) * 1.2
    body = biquad(n * env(len(n), 0.0005, 0.015), "bp", 300, 1.5) * 1.0
    put(shutter, (ring_ + click + body) * g, at)
save("shutter", shutter, -20.0)

# Peri's microphone opens: a little rising two-note bloop.
listen = np.zeros(int(SR * 0.24))
put(listen, bar(880, 0.12, 0.04), 0.0)
put(listen, bar(1320, 0.16, 0.06), 0.06)
save("listen", listen, -21.0)

# "Beaten... by a pear?!": a cartoon spring, wobbling down.
x = t(0.75)
freq = 230 + 120 * np.sin(2 * np.pi * 13 * x) * np.exp(-x / 0.3) + 70 * np.exp(-x / 0.08)
phase = 2 * np.pi * np.cumsum(freq) / SR
boing = (np.sin(phase) + 0.35 * np.sin(2 * phase) + 0.12 * np.sin(3 * phase)) * env(len(x), 0.004, 0.32)
save("boing", boing, -19.0)

# A point for Peri: a bright arpeggio and a sparkle on top.
score = np.zeros(int(SR * 1.1))
for i, hz in enumerate((1046.5, 1318.5, 1568.0, 2093.0)):
    put(score, bar(hz, 0.7, 0.3), i * 0.085)
put(score, bar(4186.0, 0.5, 0.15, 0.25), 0.34)
save("score", score, -19.0)

# A result landing: a low boom, a breath of air and a high shimmer.
impact = np.zeros(int(SR * 1.6))
x = t(1.2)
put(impact, np.sin(2 * np.pi * (38 + 30 * np.exp(-x / 0.05)) * x) * np.exp(-x / 0.45), 0)
n = noise(0.9)
put(impact, biquad(n * env(len(n), 0.004, 0.25), "lp", 1800) * 0.35, 0)
put(impact, bar(2093.0, 1.2, 0.5, 0.18), 0.02)
put(impact, bar(3136.0, 1.2, 0.4, 0.12), 0.05)
save("impact", impact, -17.0)

# A counter rolling up: quick clicks that speed up, then slow down.
ticks = np.zeros(int(SR * 1.6))
at, gap = 0.0, 0.11
while at < 1.5:
    n = noise(0.02)
    put(ticks, biquad(n * env(len(n), 0.0003, 0.004), "bp", 3000, 2.0) + biquad(n * env(len(n), 0.0005, 0.009), "bp", 1200, 3.0) * 0.6, at)
    at += gap
    gap = max(0.035, gap * 0.86) if at < 1.0 else gap * 1.25
save("ticks", ticks, -22.0)

# A pencil writing: strokes of graphite on paper, each with its own pressure.
pencil = np.zeros(int(SR * 1.5))
at = 0.0
while at < 1.35:
    length = rng.uniform(0.06, 0.16)
    n = noise(length)
    contour = np.sin(np.linspace(0, np.pi, len(n))) ** 0.7 * (0.7 + 0.3 * rng.random(len(n)))
    put(pencil, biquad(n * contour, "bp", rng.uniform(2800, 4200), 0.9) * rng.uniform(0.5, 1.0), at)
    at += length + rng.uniform(0.02, 0.09)
save("pencil", pencil, -24.0)

# Time passing on a little clock: ticks speeding up, and a tiny ding.
clock = np.zeros(int(SR * 1.7))
at, gap = 0.0, 0.16
for i in range(10):
    n = noise(0.03)
    put(clock, biquad(n * env(len(n), 0.0003, 0.004), "bp", 2400, 2.5) + biquad(n * env(len(n), 0.0005, 0.01), "bp", 900, 3.0) * 0.5, at, 1.0 if i % 2 else 0.7)
    at, gap = at + gap, max(0.045, gap * 0.8)
put(clock, bar(2637.0, 0.9, 0.35, 0.8), at + 0.05)
save("clock", clock, -21.0)

# The diary shutting: the felt cover clapping onto the pages, a puff of air, a soft thump.
close = np.zeros(int(SR * 0.6))
n = noise(0.3)
put(close, biquad(n * env(len(n), 0.001, 0.05), "lp", 1400) * 1.2, 0)
put(close, biquad(n * env(len(n), 0.0005, 0.012), "bp", 1200, 1.0) * 0.6, 0)
x = t(0.3)
put(close, np.sin(2 * np.pi * (75 + 50 * np.exp(-x / 0.01)) * x) * np.exp(-x / 0.09) * 0.9, 0)
save("close", close, -19.0)

# A twinkle: four quick high bells.
twinkle = np.zeros(int(SR * 1.2))
for i, hz in enumerate((2093.0, 2637.0, 3136.0, 4186.0)):
    put(twinkle, bar(hz, 0.8, 0.3, 0.8), i * 0.07)
save("twinkle", twinkle, -21.0)
print("diary sound effects written to", out)
