# The final 3-minute video's music and its own sound effects ("the heist"), made from scratch (no samples), at
# 120 BPM so every scene change (video/public/final/scenes.json) falls on a bar. A sneaky cold open, a big hit on the
# title, a tense drop for the stakes, a caper groove with drums and brass for the four rounds (a sad trombone each time
# the crook loses), a driving arpeggio for how Peri works, a soft warm part that falls silent at "I've fallen", and a
# victory ending.
#   python video/scripts/final_music.py  ->  video/public/final/music.wav and video/public/final/sfx/<name>.wav
import json
from pathlib import Path

import numpy as np

from music_kit import SR, bass, brass, env, hat, hz, impact, kick, pad, pluck, put, riser, rng, save, snap, snare, trombone, ts

LENGTH = 180.0
BEAT = 0.5
folder = Path(__file__).resolve().parent.parent / "public" / "final"
S = json.loads((folder / "scenes.json").read_text())
lines = json.loads((folder / "lines.json").read_text(encoding="utf-8"))
lengths = json.loads((folder / "durations.json").read_text())


def spoken(scene):
    """Each phrase's start and end, in seconds from the start (as the video places them)."""
    out, end = [], 0.0
    for line, length in zip(lines[scene], lengths[scene]):
        start = max(line["at"], end + 0.2)
        end = start + length
        out.append((S[scene][0] + start, S[scene][0] + end, line))
    return out


music = np.zeros(int(SR * (LENGTH + 1)))

# Cold open: sneaky pizzicato bass creeping up, snaps, tiptoe notes; a riser into the title.
WALK = [[38, 41, 44, 45], [38, 41, 45, 44], [43, 46, 49, 50], [45, 44, 43, 40]]
t, bar = S["cold"][0], 0
while t < S["cold"][1] - 1.5:
    for b in range(4):
        put(music, pluck(WALK[bar % 4][b], 0.3, 0.09), t + b * BEAT, 0.6)
        if b in (1, 3):
            put(music, snap(), t + b * BEAT, 0.35)
    if bar % 2:
        for i, n in enumerate([81, None, 82, None, 81, 80, 81, None]):
            if n:
                put(music, pluck(n, 0.15, 0.04), t + i * BEAT / 2, 0.15)
    bar, t = bar + 1, t + 4 * BEAT
put(music, riser(1.5), S["title"][0] - 1.5, 0.35)


def caper(start, end, key=0, brassy=True, melody=None):
    """The rounds' groove: kick, snare, hats, a walking minor bass and brass hits."""
    walk = [[38, 41, 45, 44], [43, 46, 50, 49], [41, 45, 48, 47], [45, 44, 43, 40]]
    chords = [[62, 65, 69], [67, 70, 74], [65, 69, 72], [69, 73, 76]]
    t, bar = start, 0
    while t < end - 0.05:
        for b in range(4):
            at = t + b * BEAT
            if at >= end:
                break
            put(music, kick(), at, 0.55 if b in (0, 2) else 0.0)
            put(music, snare(), at, 0.32 if b in (1, 3) else 0.0)
            put(music, hat(b == 3 and bar % 2 == 1), at + BEAT / 2, 0.12)
            put(music, hat(), at, 0.06)
            put(music, bass(walk[bar % 4][b] + key), at, 0.5)
        if brassy and bar % 2 == 0:
            put(music, brass([n + key for n in chords[bar % 4]], 0.3), t, 0.28)
            put(music, brass([n + key for n in chords[bar % 4]], 0.25), t + 1.5 * BEAT, 0.2)
        if melody:
            for i, n in enumerate(melody[bar % len(melody)]):
                if n:
                    put(music, pluck(n + key, 0.25, 0.08), t + i * BEAT / 2, 0.16)
        bar, t = bar + 1, t + 4 * BEAT


TUNE = [[74, None, 77, None, 76, 74, 72, None], [74, None, 69, None, 70, None, 72, None], [77, 76, 74, None, 72, None, 70, 69], [69, None, 73, None, 76, None, None, None]]

# Title: a big hit and the groove, bright.
put(music, impact(), S["title"][0], 0.6)
caper(S["title"][0], S["title"][1], key=0, melody=TUNE)
# The stakes: everything drops to a low drone and a heartbeat, then a riser into round 1.
x = ts(S["stakes"][1] - S["stakes"][0])
put(music, (np.sin(2 * np.pi * hz(38) * x) + 0.5 * np.sin(2 * np.pi * hz(45) * x)) * np.minimum(x / 0.3, 1), S["stakes"][0], 0.25)
for i in range(int((S["stakes"][1] - S["stakes"][0]) / 1.0)):
    put(music, kick(), S["stakes"][0] + i * 1.0, 0.45)
    put(music, kick(), S["stakes"][0] + i * 1.0 + 0.25, 0.25)
put(music, riser(1.4), S["r1"][0] - 1.4, 0.35)
# The four rounds: an impact on each banner, the groove (a different key colour per round), a riser before the next.
for r, key in (("r1", 0), ("r2", 2), ("r3", -2), ("r4", 3)):
    a, b = S[r]
    put(music, impact(), a, 0.6)
    if r == "r4":
        # The family key: the caper stops for a warm, confident groove while Peri explains it, until Ana says no.
        key_from, key_to = spoken("r4")[3][0] - 0.4, spoken("r4")[7][0] - 0.2
        caper(a + 1.0, key_from, key=key)
        put(music, pad([60, 64, 67, 72], key_to - key_from), key_from, 0.22)
        t2, i = key_from, 0
        while t2 < key_to - 0.3:
            put(music, kick(), t2, 0.28 if i % 2 == 0 else 0.0)
            put(music, hat(), t2 + BEAT / 2, 0.06)
            put(music, pluck([72, 76, 79, 76, 74, 77, 81, 77][i % 8], 0.4, 0.18), t2, 0.12)
            put(music, bass([48, 48, 45, 45, 41, 41, 43, 43][i % 8], 0.45), t2, 0.32)
            t2, i = t2 + BEAT, i + 1
        put(music, impact(), key_to, 0.45)
        caper(key_to, b - 1.0, key=key)
    else:
        caper(a + 1.0, b - 1.0, key=key, melody=TUNE if r in ("r1", "r3") else None)
    put(music, riser(1.0), b - 1.0, 0.3)
    last = spoken(r)[-1]
    put(music, trombone(58 + key), last[1] + 0.15, 0.45)
# How Peri works: a driving arpeggio over a four-on-the-floor kick.
a, b = S["tech"]
put(music, impact(), a, 0.5)
ARP = [[50, 57, 62, 65], [46, 53, 58, 62], [48, 55, 60, 64], [45, 52, 57, 61]]
t, bar = a, 0
while t < b - 0.05:
    notes = ARP[bar % 4]
    put(music, bass(notes[0] - 12, 2.0), t, 0.35)
    for s16 in range(16):
        put(music, pluck(notes[s16 % 4] + 12, 0.16, 0.05), t + s16 * BEAT / 4, 0.09)
    for b4 in range(4):
        put(music, kick(), t + b4 * BEAT, 0.42)
        put(music, hat(), t + b4 * BEAT + BEAT / 2, 0.12)
        if b4 in (1, 3):
            put(music, snare(), t + b4 * BEAT, 0.18)
    bar, t = bar + 1, t + 4 * BEAT
# The heart: soft and warm, falling silent at "I've fallen", warm again when Ana answers.
a, b = S["heart"]
fallen = spoken("heart")[1][0]
ana = spoken("heart")[3][0]
put(music, pad([60, 64, 67, 72], fallen - a + 0.3, fade_out=0.4), a, 0.22)
for i, n in enumerate([72, 76, 79, 76, 74, 76, 72, None, 74, 77, 81, 79]):
    if n and a + i * 0.4 < fallen - 0.3:
        put(music, pluck(n, 0.5, 0.25), a + 0.2 + i * 0.4, 0.14)
x = ts(ana - fallen)
put(music, np.sin(2 * np.pi * hz(36) * x) * np.minimum(x / 0.5, 1) * np.minimum((x[-1] - x) / 0.3 + 0.01, 1), fallen, 0.18)
for i in range(int((ana - fallen) / 0.9)):
    put(music, kick(), fallen + 0.4 + i * 0.9, 0.22)
put(music, pad([53, 60, 65, 69, 72], b - ana + 0.5), ana, 0.25)
# The finale: a fanfare when Peri answers, the groove and a last big chord.
a, b = S["finale"]
put(music, impact(), a, 0.55)
fan = spoken("finale")[1][0]
for notes, at in (([62, 66, 69], 0.0), ([62, 66, 69], 0.2), ([64, 67, 71], 0.4), ([66, 69, 74], 0.65)):
    put(music, brass(notes, 0.3 if at < 0.6 else 1.6), fan + at, 0.4)
caper(fan + 1.0, b - 3.0, key=0, melody=TUNE)
put(music, impact(), b - 3.0, 0.4)
put(music, pad([50, 57, 62, 66, 69, 74], 3.0), b - 3.0, 0.35)

music = music[: int(SR * LENGTH)]
fade = int(SR * 1.5)
music[-fade:] *= np.linspace(1, 0, fade)


save(folder / "music.wav", music, 0.85)
sfx = folder / "sfx"
sfx.mkdir(exist_ok=True)
save(sfx / "impact.wav", impact(), 0.9)
save(sfx / "riser.wav", riser(1.0), 0.6)
x = ts(0.18)
save(sfx / "slam.wav", (np.sin(2 * np.pi * (70 + 200 * np.exp(-x / 0.02)) * x) + 0.4 * np.diff(rng.standard_normal(len(x)), prepend=0)) * np.exp(-x / 0.06), 0.9)
x = ts(0.5)
save(sfx / "flash.wav", np.diff(rng.standard_normal(len(x)), prepend=0) * np.exp(-x / 0.12), 0.4)
print("music", LENGTH, "s; crook losses at", [round(spoken(r)[-1][1], 1) for r in ("r1", "r2", "r3", "r4")])
