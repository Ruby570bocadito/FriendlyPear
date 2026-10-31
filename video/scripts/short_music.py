# The short's music and sound effects ("a scammer's worst day"), made from scratch (no samples). The music follows
# the short's timeline (video/public/short/timeline.json, from short_voice.py): a sneaky heist groove (pizzicato bass
# creeping chromatically, finger snaps, tiptoe notes) while the crook schemes, a sad trombone each time he loses a
# round, and a victory jingle on the end card.
#   python video/scripts/short_music.py  ->  video/public/short/music.wav and video/public/short/sfx/<name>.wav
import json
import wave
from pathlib import Path

import numpy as np

SR = 44100
folder = Path(__file__).resolve().parent.parent / "public" / "short"
timeline = json.loads((folder / "timeline.json").read_text())
LENGTH = timeline["length"]
lines = timeline["lines"]
losses = [l for l in lines if l["top"] in ("crook_shock", "crook_angry", "crook_quit") and l["who"] == "crook" and l["text"] in ("Wait. Who is THAT?", "A PEAR?!", "I quit. I'm getting a real job.")]
ending = next(l for l in lines if l["top"] == "end")
BEAT = 0.5
rng = np.random.default_rng(5)


def hz(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def ts(seconds):
    return np.arange(int(SR * seconds)) / SR


def env(n, attack=0.004, decay=0.2):
    x = np.arange(n) / SR
    return np.minimum(x / attack, 1) * np.exp(-x / decay)


def put(dest, sig, at, gain=1.0):
    i = int(at * SR)
    if i < 0 or i >= len(dest):
        return
    j = min(len(dest), i + len(sig))
    dest[i:j] += sig[: j - i] * gain


def pizz(n, dur=0.25, decay=0.07):
    x = ts(dur)
    f = hz(n)
    return (np.sin(2 * np.pi * f * x) + 0.5 * np.sin(4 * np.pi * f * x) + 0.15 * np.sin(6 * np.pi * f * x)) * env(len(x), 0.002, decay)


def twang(notes, dur=0.18):
    x = ts(dur)
    s = sum(np.sin(2 * np.pi * hz(n) * x * (1 + 0.004 * np.sin(2 * np.pi * 6 * x))) for n in notes) / len(notes)
    return s * env(len(x), 0.002, 0.05)


def snap():
    n = np.diff(rng.standard_normal(int(SR * 0.05)), prepend=0)
    return n * env(len(n), 0.001, 0.012)


def brass(notes, dur=0.4):
    x = ts(dur)
    s = sum(sum(np.sin(2 * np.pi * hz(n) * k * x) / k for k in range(1, 8)) for n in notes) / len(notes)
    return s * np.minimum(x / 0.02, 1) * np.exp(-x / (dur / 2.2))


def save(path, sig, peak=0.9):
    sig = sig / max(1e-9, np.max(np.abs(sig))) * peak
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((sig * 32767).astype(np.int16).tobytes())


# The sneaky groove, in D minor: the bass creeps up chromatically, snaps on 2 and 4, tiptoe notes every other bar.
WALK = [[38, 41, 44, 45], [38, 41, 45, 44], [43, 46, 49, 50], [45, 44, 43, 40]]
STABS = [[62, 65, 69], [62, 65, 69], [67, 70, 74], [69, 73, 76]]
TIPTOE = [81, None, 82, None, 81, 80, 81, None]
music = np.zeros(int(SR * (LENGTH + 0.5)))
bar = 0
t = 0.0
while t < ending["start"] - 0.1:
    for b in range(4):
        at = t + b * BEAT
        if at >= ending["start"]:
            break
        put(music, pizz(WALK[bar % 4][b], 0.3, 0.09), at, 0.6)
        if b in (1, 3):
            put(music, snap(), at, 0.35)
        put(music, twang(STABS[bar % 4]), at + BEAT / 2, 0.12)
    if bar % 2 == 1:
        for i, n in enumerate(TIPTOE):
            if n is not None and t + i * BEAT / 2 < ending["start"]:
                put(music, pizz(n, 0.15, 0.04), t + i * BEAT / 2, 0.16)
    bar += 1
    t += 4 * BEAT

# A sad trombone each time the crook loses a round.
def trombone(start_note):
    out = np.zeros(int(SR * 2.0))
    for i, n in enumerate([start_note, start_note - 1, start_note - 2]):
        x = ts(0.32)
        out[int(i * 0.34 * SR): int(i * 0.34 * SR) + len(x)] += np.sin(2 * np.pi * np.cumsum(np.full(len(x), hz(n))) / SR) * env(len(x), 0.02, 0.25)
    x = ts(0.9)
    wobble = hz(start_note - 3) * (1 + 0.02 * np.sin(2 * np.pi * 6 * x)) * (1 - 0.03 * x)
    out[int(1.02 * SR): int(1.02 * SR) + len(x)] += np.sin(2 * np.pi * np.cumsum(wobble) / SR) * np.minimum(x / 0.03, 1) * np.exp(-x / 0.6)
    return out


for loss in losses:
    put(music, trombone(58), loss["end"] + 0.05, 0.4)

# The end card: a victory jingle and a ringing chord.
v = ending["start"]
for i, (notes, at) in enumerate(([[62, 66, 69], 0.0], [[62, 66, 69], 0.18], [[64, 67, 71], 0.36], [[66, 69, 74], 0.6])):
    put(music, brass(notes, 0.35 if i < 3 else 1.6), v + at, 0.5)
x = ts(LENGTH - v)
pad = sum(np.sin(2 * np.pi * hz(n) * x) for n in (50, 57, 62, 66, 69)) / 5
put(music, pad * np.minimum(x / 0.3, 1), v + 0.6, 0.3)

music = music[: int(SR * LENGTH)]
fade = int(SR * 1.2)
music[-fade:] *= np.linspace(1, 0, fade)
save(folder / "music.wav", music, 0.85)

# The short's own sound effects.
sfx = folder / "sfx"
sfx.mkdir(exist_ok=True)
typing = np.zeros(int(SR * 1.6))
for i in range(14):
    k = np.diff(rng.standard_normal(int(SR * 0.02)), prepend=0) * env(int(SR * 0.02), 0.001, 0.006)
    put(typing, k, i * 0.11 + rng.uniform(0, 0.03), rng.uniform(0.5, 1.0))
save(sfx / "typing.wav", typing, 0.6)
x = ts(0.3)
save(sfx / "send.wav", np.sin(2 * np.pi * np.cumsum(500 + 1600 * x / 0.3) / SR) * env(len(x), 0.005, 0.1), 0.5)
ding = np.zeros(int(SR * 0.8))
put(ding, pizz(88, 0.5, 0.18), 0.0)
put(ding, pizz(93, 0.6, 0.22), 0.1)
save(sfx / "ding.wav", ding, 0.6)
ring = np.zeros(int(SR * 2.2))
for rep in (0.0, 1.1):
    for i in range(8):
        x = ts(0.06)
        put(ring, np.sin(2 * np.pi * (hz(84) if i % 2 else hz(88)) * x) * env(len(x), 0.002, 0.05), rep + i * 0.07)
save(sfx / "ring.wav", ring, 0.6)
alert = np.zeros(int(SR * 1.0))
for at in (0.0, 0.25):
    x = ts(0.16)
    put(alert, np.sign(np.sin(2 * np.pi * 160 * x)) * np.minimum(np.minimum(x / 0.01, 1), np.minimum((0.16 - x) / 0.02, 1)), at, 0.4)
put(alert, pizz(79, 0.5, 0.2), 0.05, 0.8)
put(alert, pizz(74, 0.6, 0.25), 0.3, 0.8)
save(sfx / "alert.wav", alert, 0.75)
x = ts(0.7)
boing = np.sin(2 * np.pi * np.cumsum(220 + 160 * np.sin(2 * np.pi * 9 * x) * np.exp(-x / 0.25)) / SR) * np.exp(-x / 0.3)
save(sfx / "boing.wav", boing, 0.7)
score = np.zeros(int(SR * 0.9))
put(score, pizz(91, 0.5, 0.2), 0.0)
put(score, pizz(96, 0.7, 0.3), 0.12)
save(sfx / "score.wav", score, 0.55)
x = ts(0.26)
save(sfx / "whip.wav", np.convolve(rng.standard_normal(len(x)), np.ones(10) / 10, mode="same") * np.sin(np.pi * x / 0.26) ** 3, 0.45)
x = ts(0.12)
save(sfx / "pop.wav", np.sin(2 * np.pi * np.cumsum(380 + 900 * x / 0.12) / SR) * env(len(x), 0.002, 0.035), 0.5)
print("music", round(LENGTH, 2), "s; losses at", [round(l["end"], 2) for l in losses], "; sfx", sorted(p.name for p in sfx.glob("*.wav")))
