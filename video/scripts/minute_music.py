# The one-minute demo's music and sound effects, made from scratch (no samples): a plucky marimba groove at
# 120 BPM that turns tense while Peri warns, with clay "pops" and whooshes on each scene change.
#   python video/scripts/minute_music.py          ->  video/public/minute/music.wav    (the one-minute demo)
#   python video/scripts/minute_music.py thirty   ->  video/public/minute/music30.wav  (the 30-second cut)
#   python video/scripts/minute_music.py submission -> video/public/submission/music.wav (the 3-minute submission)
import sys
import wave
from pathlib import Path

import numpy as np

SR = 44100
MODE = sys.argv[1] if len(sys.argv) > 1 else "minute"
THIRTY = MODE == "thirty"
LENGTH = {"thirty": 30.0, "submission": 180.0}.get(MODE, 60.0)
BEAT = 0.5  # 120 BPM
out = np.zeros(int(SR * (LENGTH + 1)))


def note_hz(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def add(sig, t, gain=1.0):
    i = int(t * SR)
    j = min(len(out), i + len(sig))
    out[i:j] += sig[: j - i] * gain


def env(n, attack=0.005, decay=0.4):
    t = np.arange(n) / SR
    return np.minimum(t / attack, 1) * np.exp(-t / decay)


def marimba(n, dur=0.6):
    k = int(SR * dur)
    t = np.arange(k) / SR
    f = note_hz(n)
    s = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t / 0.05)
    return s * env(k, 0.002, dur / 3)


def pad(notes, dur):
    k = int(SR * dur)
    t = np.arange(k) / SR
    s = sum(np.sin(2 * np.pi * note_hz(n) * t + np.sin(2 * np.pi * 0.3 * t)) for n in notes) / len(notes)
    fade = np.minimum(np.minimum(t / 0.4, 1), np.minimum((dur - t) / 0.5, 1))
    return s * fade


def bass(n, dur):
    k = int(SR * dur)
    t = np.arange(k) / SR
    f = note_hz(n)
    return (np.sin(2 * np.pi * f * t) + 0.2 * np.sin(4 * np.pi * f * t)) * env(k, 0.01, dur / 2)


def kick():
    k = int(SR * 0.35)
    t = np.arange(k) / SR
    return np.sin(2 * np.pi * (45 + 90 * np.exp(-t / 0.04)) * t) * np.exp(-t / 0.12)


def shaker():
    k = int(SR * 0.08)
    rng = np.random.default_rng(len(out) % 97)
    s = rng.standard_normal(k)
    s = np.diff(s, prepend=0)  # brighter
    return s * env(k, 0.003, 0.02)


def pop(t, up=True):
    k = int(SR * 0.18)
    tt = np.arange(k) / SR
    f = (300 + 900 * tt / 0.18) if up else (900 - 600 * tt / 0.18)
    add(np.sin(2 * np.pi * np.cumsum(f) / SR) * env(k, 0.002, 0.05), t, 0.5)


def whoosh(t, dur=0.6):
    k = int(SR * dur)
    rng = np.random.default_rng(int(t * 10))
    s = rng.standard_normal(k)
    s = np.convolve(s, np.ones(18) / 18, mode="same")
    tt = np.arange(k) / SR
    add(s * np.sin(np.pi * tt / dur) ** 2, t - dur / 2, 0.9)


def sparkle(t):
    for i, n in enumerate([84, 88, 91, 96, 100]):
        add(marimba(n, 0.5), t + i * 0.06, 0.18)


# Chords (C major): C, G, Am, F; tense: Am, F, Dm, E.
HAPPY = [(48, [60, 64, 67]), (43, [59, 62, 67]), (45, [60, 64, 69]), (41, [60, 65, 69])]
TENSE = [(45, [57, 60, 64]), (41, [57, 60, 65]), (38, [57, 62, 65]), (40, [56, 59, 64])]
MELODY = [76, 79, 81, 79, 76, 74, 72, 74, 76, 79, 84, 81, 79, 76, 74, 72]
MELODY_B = [72, 76, 79, 81, 79, 76, 74, 76, 72, 76, 79, 84, 83, 79, 76, 74]
# The engine room (how Peri decides): Am, F, C, G.
TECH = [(45, [57, 60, 64]), (41, [57, 60, 65]), (48, [55, 60, 64]), (43, [55, 59, 62])]


def groove(start, end, melody=True, tune=MELODY, octave=0):
    bar = 0
    t = start
    while t < end - 0.01:
        root, chord = HAPPY[bar % 4]
        add(bass(root, 2.0), t, 0.35)
        add(pad(chord, 2.0), t, 0.08)
        for b in range(4):
            add(kick(), t + b * BEAT, 0.45 if b % 2 == 0 else 0.25)
            for h in range(2):
                add(shaker(), t + b * BEAT + h * BEAT / 2, 0.08 if h else 0.04)
            # an off-beat chord pluck
            for n in chord:
                add(marimba(n + 12, 0.25), t + b * BEAT + BEAT / 2, 0.07)
        if melody:
            for i in range(4):
                add(marimba(tune[(bar * 4 + i) % len(tune)] + octave, 0.5), t + i * BEAT, 0.2)
        bar += 1
        t += 2.0


def arps(start, end):
    """Quick marimba arpeggios over a light kick, for the parts about how Peri works."""
    bar = 0
    t = start
    while t < end - 0.01:
        root, chord = TECH[bar % 4]
        add(bass(root, 2.0), t, 0.3)
        add(pad(chord, 2.0), t, 0.07)
        notes = [chord[0] + 12, chord[1] + 12, chord[2] + 12, chord[1] + 24]
        for s in range(16):
            add(marimba(notes[s % 4], 0.18), t + s * BEAT / 4, 0.06)
        for b in range(4):
            add(kick(), t + b * BEAT, 0.35 if b % 2 == 0 else 0.0)
            add(shaker(), t + b * BEAT + BEAT / 2, 0.06)
        bar += 1
        t += 2.0


def hook(end):
    """A low, curious pad and a ticking pluck."""
    add(pad([45, 52, 57], end), 0, 0.18)
    for i in range(int(end / BEAT)):
        add(marimba(69 if i % 4 else 64, 0.3), i * BEAT, 0.12)


def tense(start, end):
    """A code during a call: minor pad, heartbeat kick, low pulses."""
    for bar in range(int((end - start) / 2)):
        t = start + bar * 2.0
        root, chord = TENSE[bar % 4]
        add(pad(chord, 2.0), t, 0.16)
        add(bass(root - 12, 2.0), t, 0.3)
        for b in range(4):
            add(kick(), t + b * BEAT, 0.5 if b in (0, 1) else 0.0)
            add(marimba(root + 24, 0.2), t + b * BEAT + BEAT / 2, 0.06)


def finale(t):
    """The end card: a big chord, a sparkle and a long ring."""
    add(pad([48, 55, 60, 64, 67, 72], LENGTH - t - 0.5), t, 0.2)
    add(bass(36, 6.0), t, 0.4)
    add(kick(), t, 0.6)
    for i, n in enumerate([72, 76, 79, 84, 79, 84, 88]):
        add(marimba(n, 1.0), t + 0.5 + i * 0.25, 0.18)
    add(marimba(84, 3.0), t + 4, 0.25)
    add(marimba(79, 3.0), t + 4, 0.2)


if MODE == "submission":
    # hook 0-14, problem 14-22, Peri and the new number 22-52, code 52-72, picture, family and a fall 72-112,
    # how Peri works 112-160, a friend 160-170, end 170-180. Its sound effects are placed by the video itself.
    hook(14)
    tense(14, 22)
    groove(22, 52)
    tense(52, 72)
    groove(72, 112, tune=MELODY_B)
    arps(112, 160)
    groove(160, 170, octave=12)
    finale(170)
    cuts, sparkles = (), ()
elif THIRTY:
    # hook 0-4, Peri 4-8, new number 8-14, code 14-20, family 20-24, end 24-30
    hook(4)
    groove(4, 14)
    tense(14, 20)
    groove(20, 24)
    finale(24)
    cuts, sparkles = (4, 8, 14, 20, 24), (6.5, 13.0, 23.0, 25.0)
else:
    # hook 0-6, Peri and the new number 6-24, code 24-36, family and proof 36-52, end 52-60
    hook(6)
    groove(6, 24)
    tense(24, 36)
    groove(36, 52)
    finale(52)
    cuts, sparkles = (6, 12, 24, 36, 46, 52), (10.0, 22.0, 44.0, 48.0, 50.0, 53.2)

# Scene changes and Peri's entrances.
for t in cuts:
    whoosh(t)
    pop(t + 0.4)
for t in sparkles:
    sparkle(t)

out = out[: int(SR * LENGTH)]
fade = int(SR * 1.5)
out[-fade:] *= np.linspace(1, 0, fade)
out /= np.max(np.abs(out)) / 0.85
pcm = (out * 32767).astype(np.int16)
folder = Path(__file__).resolve().parent.parent / "public"
dest = folder / "submission" / "music.wav" if MODE == "submission" else folder / "minute" / ("music30.wav" if THIRTY else "music.wav")
with wave.open(str(dest), "wb") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print(dest)
