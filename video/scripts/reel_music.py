# The interview reel's music and sound effects, made from scratch (no samples). The music follows the reel's
# timeline (video/public/reel/timeline.json, from reel_voice.py): an office-comedy groove (swing pizzicato, walking
# bass, brushes) during the interview, a fanfare when Peri is hired, a dead stop and a tense pulse when the scam
# arrives, and a bright ending.
#   python video/scripts/reel_music.py  ->  video/public/reel/music.wav and video/public/reel/sfx/<name>.wav
import json
import wave
from pathlib import Path

import numpy as np

SR = 44100
folder = Path(__file__).resolve().parent.parent / "public" / "reel"
timeline = json.loads((folder / "timeline.json").read_text())
LENGTH = timeline["length"]
lines = timeline["lines"]
hired = next(l for l in lines if l["shot"] == "rosa_laugh")
scam = next(l for l in lines if l["shot"] == "scam")
calm = next(l for l in lines if l["shot"] == "rosa_phone")
ending = next(l for l in lines if l["shot"] == "end")

BEAT = 60 / 112
SWING = BEAT * 2 / 3
rng = np.random.default_rng(11)


def hz(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def ts(seconds):
    return np.arange(int(SR * seconds)) / SR


def env(n, attack=0.004, decay=0.2):
    x = np.arange(n) / SR
    return np.minimum(x / attack, 1) * np.exp(-x / decay)


def put(dest, sig, at, gain=1.0):
    i = int(at * SR)
    if i >= len(dest) or i < 0:
        return
    j = min(len(dest), i + len(sig))
    dest[i:j] += sig[: j - i] * gain


def pluck(n, dur=0.3, decay=0.09):
    x = ts(dur)
    f = hz(n)
    s = np.sin(2 * np.pi * f * x) + 0.45 * np.sin(4 * np.pi * f * x) + 0.2 * np.sin(6 * np.pi * f * x)
    return s * env(len(x), 0.002, decay)


def upright(n, dur=0.5):
    x = ts(dur)
    f = hz(n)
    s = np.sin(2 * np.pi * f * x) + 0.3 * np.sin(4 * np.pi * f * x) + 0.08 * np.sin(6 * np.pi * f * x)
    return s * env(len(x), 0.006, 0.28)


def brush(dur=0.18, decay=0.06):
    n = rng.standard_normal(int(SR * dur))
    n = np.convolve(n, np.ones(5) / 5, mode="same")
    return n * env(len(n), 0.003, decay)


def snap():
    n = np.diff(rng.standard_normal(int(SR * 0.05)), prepend=0)
    return n * env(len(n), 0.001, 0.01)


def brass(notes, dur=0.5):
    x = ts(dur)
    s = sum(sum(np.sin(2 * np.pi * hz(n) * k * x) / k for k in range(1, 8)) for n in notes) / len(notes)
    return s * np.minimum(x / 0.02, 1) * np.exp(-x / (dur / 2.5))


def pad(notes, dur):
    x = ts(dur)
    s = sum(np.sin(2 * np.pi * hz(n) * x + 0.4 * np.sin(2 * np.pi * 0.25 * x)) for n in notes) / len(notes)
    return s * np.minimum(np.minimum(x / 0.3, 1), np.minimum((dur - x) / 0.6, 1))


# F, Dm, Gm, C7: chord tones (comping) and a walking bass line per bar.
BARS = [
    ([65, 69, 72], [41, 45, 48, 50]),
    ([62, 65, 69], [38, 41, 45, 48]),
    ([67, 70, 74], [43, 46, 50, 52]),
    ([64, 67, 70, 72], [48, 46, 43, 40]),
]
TUNE = [
    [72, None, 69, None, 65, 67, 69, None],
    [74, None, 72, None, 69, None, 65, None],
    [70, None, 69, 67, None, 74, None, None],
    [72, 70, 69, 67, 64, None, 72, None],
]


def groove(dest, start, end, melody_from=1, up=0):
    bar = 0
    t = start
    while t < end - BEAT:
        chord, walk = BARS[bar % 4]
        for b in range(4):
            at = t + b * BEAT
            if at >= end:
                break
            put(dest, upright(walk[b]), at, 0.55)
            if b in (1, 3):
                put(dest, brush(), at, 0.16)
                put(dest, snap(), at, 0.1)
                for n in chord:
                    put(dest, pluck(n + up, 0.2, 0.06), at + SWING, 0.1)
            put(dest, brush(0.08, 0.02), at + SWING, 0.05)
        if (bar // 4) % 2 == melody_from % 2:
            for i, n in enumerate(TUNE[bar % 4]):
                at = t + (i // 2) * BEAT + (SWING if i % 2 else 0)
                if n is not None and at < end:
                    put(dest, pluck(n + up, 0.3, 0.1), at, 0.22)
        bar += 1
        t += 4 * BEAT


music = np.zeros(int(SR * (LENGTH + 0.5)))
# The interview.
groove(music, 0.0, hired["start"] + 0.5)
# Hired: a fanfare.
fan = hired["start"] + 0.55
put(music, brass([65, 69, 72]), fan, 0.5)
put(music, brass([67, 72, 76], 0.25), fan + 0.32, 0.45)
put(music, brass([69, 72, 77], 0.9), fan + 0.52, 0.55)
crash = rng.standard_normal(int(SR * 1.4))
put(music, np.diff(crash, prepend=0) * env(len(crash), 0.002, 0.35), fan + 0.52, 0.12)
# The scam: everything stops, then a low pulse and a ticking clock until Peri has spoken.
x = ts(calm["start"] - scam["start"])
drone = (np.sin(2 * np.pi * hz(45) * x) + 0.6 * np.sin(2 * np.pi * hz(52) * x)) * (0.6 + 0.4 * np.sin(2 * np.pi * 2 * x))
drone *= np.minimum(np.minimum(x / 0.4, 1), np.minimum((x[-1] - x) / 0.3 + 0.001, 1))
put(music, drone, scam["start"] + 0.25, 0.22)
t = scam["start"] + 0.25
while t < calm["start"] - 0.2:
    put(music, pluck(93, 0.06, 0.015), t, 0.12)
    t += BEAT / 2
# Calm again: the groove, brighter, and a last big chord under the end card.
groove(music, calm["start"], ending["start"], melody_from=0, up=12)
put(music, pad([53, 60, 65, 69, 72], LENGTH - ending["start"] + 0.3), ending["start"], 0.35)
put(music, upright(41, 2.0), ending["start"], 0.6)
for i, n in enumerate([65, 69, 72, 77, 81, 84]):
    put(music, pluck(n, 0.8, 0.3), ending["start"] + 0.1 + i * 0.09, 0.2)

music = music[: int(SR * LENGTH)]
fade = int(SR * 1.2)
music[-fade:] *= np.linspace(1, 0, fade)


def save(path, sig, peak=0.9):
    sig = sig / max(1e-9, np.max(np.abs(sig))) * peak
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((sig * 32767).astype(np.int16).tobytes())


save(folder / "music.wav", music, 0.85)

# The reel's own sound effects.
sfx = folder / "sfx"
sfx.mkdir(exist_ok=True)
x = ts(0.6)
buzzer = (np.sign(np.sin(2 * np.pi * 146 * x)) + np.sign(np.sin(2 * np.pi * 151 * x))) * np.minimum(np.minimum(x / 0.01, 1), np.minimum((0.6 - x) / 0.05, 1))
save(sfx / "buzzer.wav", np.convolve(buzzer, np.ones(4) / 4, mode="same"), 0.55)
bell = np.zeros(int(SR * 1.2))
for at, n in ((0.0, 91), (0.14, 96)):
    y = ts(1.0)
    put(bell, (np.sin(2 * np.pi * hz(n) * y) + 0.3 * np.sin(2 * np.pi * hz(n) * 2.76 * y)) * env(len(y), 0.002, 0.35), at)
save(sfx / "bell.wav", bell, 0.6)
n = rng.standard_normal(int(SR * 0.26))
y = ts(0.26)
whip = np.convolve(n, np.ones(10) / 10, mode="same") * np.sin(np.pi * y / 0.26) ** 3
save(sfx / "whip.wav", whip, 0.5)
y = ts(0.5)
scratch = np.sin(2 * np.pi * np.cumsum(900 * np.exp(-y / 0.12) + 120) / SR) * np.exp(-y / 0.2)
scratch += 0.5 * np.convolve(rng.standard_normal(len(y)), np.ones(3) / 3, mode="same") * np.exp(-y / 0.1)
save(sfx / "scratch.wav", scratch, 0.6)
notify = np.zeros(int(SR * 1.1))
for at in (0.0, 0.28):
    y = ts(0.2)
    put(notify, np.sign(np.sin(2 * np.pi * 170 * y)) * np.minimum(np.minimum(y / 0.01, 1), np.minimum((0.2 - y) / 0.02, 1)), at, 0.5)
for i, n in enumerate((76, 81, 88)):
    put(notify, pluck(n, 0.4, 0.12), 0.55 + i * 0.1, 0.8)
save(sfx / "notify.wav", notify, 0.7)
y = ts(0.35)
stamp = np.sin(2 * np.pi * (55 + 110 * np.exp(-y / 0.03)) * y) * np.exp(-y / 0.09)
save(sfx / "stamp.wav", stamp, 0.9)
y = ts(0.12)
save(sfx / "pop.wav", np.sin(2 * np.pi * np.cumsum(380 + 900 * y / 0.12) / SR) * env(len(y), 0.002, 0.035), 0.5)
# Ba-dum-tss, after Peri's joke.
rim = np.zeros(int(SR * 1.2))
for at, f0 in ((0.0, 190), (0.16, 140)):
    y = ts(0.3)
    put(rim, np.sin(2 * np.pi * np.cumsum(f0 * (1 + 0.6 * np.exp(-y / 0.02))) / SR) * np.exp(-y / 0.1), at)
cym = np.diff(rng.standard_normal(int(SR * 0.9)), prepend=0)
put(rim, cym * env(len(cym), 0.002, 0.25), 0.34, 0.5)
save(sfx / "rimshot.wav", rim, 0.7)
print("music", round(LENGTH, 2), "s; sfx", sorted(p.name for p in sfx.glob("*.wav")))
