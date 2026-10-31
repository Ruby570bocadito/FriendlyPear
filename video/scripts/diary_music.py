# The music of "Peri's diary", the second 3-minute video, and its own sound effects, made from scratch with the
# instruments in music_kit.py and placed on its script (video/public/diary): a music-box lullaby while Peri writes at
# night, a bouncy morning groove when she wakes, and for each trick of the day a little chime for the hour, the crook's
# sneaky walk, a tense hush while Peri warns Rosa, a bright hit when the trick is stopped and a sad trombone for the
# crook; the family key gets a warm confident groove, how Peri decides a driving arpeggio with sparkles for the Muse
# Image montage, and the night a tender lullaby that falls silent at "I've fallen" and warms up when Ana answers.
#   python video/scripts/diary_music.py  ->  video/public/diary/music.wav and video/public/diary/sfx/<name>.wav
import json
from pathlib import Path

import numpy as np

from music_kit import SR, bass, bell, birds, book_close, brass, breeze, chime, clock_spin, crickets, flick, hat, hz, impact, kick, owl, pad, page_turn, pluck, put, save, scribble, snap, trombone, ts, wall_clock

LENGTH = 180.0
BEAT = 0.5
folder = Path(__file__).resolve().parent.parent / "public" / "diary"
S = json.loads((folder / "scenes.json").read_text())
lines = json.loads((folder / "lines.json").read_text(encoding="utf-8"))
lengths = json.loads((folder / "durations.json").read_text())


def spoken(scene):
    """Each phrase's start and end, in seconds from the start (as the video places them), and the line itself."""
    out, end = [], 0.0
    for line, length in zip(lines[scene], lengths[scene]):
        start = max(line["at"], end + 0.2)
        end = start + length
        out.append((S[scene][0] + start, S[scene][0] + end, line))
    return out


music = np.zeros(int(SR * (LENGTH + 2)))
LULL = [65, 69, 72, 77, 76, 72, 74, 72, 69, 70, 69, 65, 67, 69, 72, 69]


def lullaby(start, end, gain=0.2):
    """The night's music box in F, over soft chords."""
    chords = [[53, 57, 60], [58, 62, 65], [55, 60, 64], [53, 57, 60]]
    t, i = start, 0
    while t < end - 0.4:
        if i % 4 == 0:
            put(music, pad(chords[(i // 4) % 4], min(2.6, end - t + 0.3), 0.5, 0.8), t, gain * 0.8)
        put(music, bell(LULL[i % len(LULL)] + 12, 1.6, 0.8), t, gain)
        t, i = t + 0.6, i + 1


def morning(start, end, gain=1.0, tune=False):
    """The day's groove: C, G, A minor, F strummed on a little ukulele, kick and snaps, and a whistled tune."""
    chords = [[60, 64, 67], [55, 59, 62], [57, 60, 64], [53, 57, 60]]
    roots = [48, 43, 45, 41]
    whistle = [[79, None, 81, 79, 76, None, 74, None], [74, None, 76, 74, 71, None, 67, None], [72, None, 74, 76, 79, None, 76, None], [77, 76, 74, None, 72, None, None, None]]
    t, bar = start, 0
    while t < end - 0.05:
        for b4 in range(4):
            at = t + b4 * BEAT
            if at >= end:
                break
            put(music, kick(), at, 0.38 * gain if b4 in (0, 2) else 0.0)
            put(music, snap(), at, 0.24 * gain if b4 in (1, 3) else 0.0)
            put(music, hat(), at + BEAT / 2, 0.07 * gain)
            put(music, bass(roots[bar % 4], 0.4), at, 0.34 * gain if b4 in (0, 2) else 0.0)
            for k, n in enumerate(chords[bar % 4]):
                put(music, pluck(n + 12, 0.22, 0.06), at + k * 0.025, 0.06 * gain)
        if tune:
            for i, n in enumerate(whistle[bar % 4]):
                if n and t + i * BEAT / 2 < end:
                    put(music, pluck(n + 12, 0.3, 0.12), t + i * BEAT / 2, 0.1 * gain)
        bar, t = bar + 1, t + 4 * BEAT


def sneak(start, end):
    """The crook tiptoeing in: a creeping pizzicato bass and finger snaps."""
    walk = [[38, 41, 44, 45], [38, 41, 45, 44], [43, 46, 49, 50], [45, 44, 43, 40]]
    t, bar = start, 0
    while t < end - 0.1:
        for b4 in range(4):
            if t + b4 * BEAT < end:
                put(music, pluck(walk[bar % 4][b4], 0.3, 0.09), t + b4 * BEAT, 0.55)
                if b4 in (1, 3):
                    put(music, snap(), t + b4 * BEAT, 0.3)
        bar, t = bar + 1, t + 4 * BEAT


def hush(start, end, notes=(50, 53, 57, 60)):
    """Danger: everything drops to a soft, uneasy chord while Peri speaks."""
    if end > start:
        put(music, pad(list(notes), end - start, 0.3, 0.4), start, 0.18)


def family(start, end):
    """The family key: a warm, confident groove."""
    put(music, pad([60, 64, 67, 72], end - start), start, 0.2)
    t, i = start, 0
    while t < end - 0.3:
        put(music, kick(), t, 0.26 if i % 2 == 0 else 0.0)
        put(music, hat(), t + BEAT / 2, 0.06)
        put(music, pluck([72, 76, 79, 76, 74, 77, 81, 77][i % 8], 0.4, 0.18), t, 0.11)
        put(music, bass([48, 48, 45, 45, 41, 41, 43, 43][i % 8], 0.45), t, 0.3)
        t, i = t + BEAT, i + 1


# Night: Peri writes her diary to a music box, and a twinkle carries us into the morning.
a, b = S["open"]
lullaby(a, b - 0.5, 0.22)
put(music, chime([77, 81, 84, 89, 93], 0.06), b - 0.6, 0.3)
# Morning: Peri drops onto her hill (a soft bump as she lands), and the day starts bouncing.
a, b = S["title"]
put(music, impact(), a + 0.57, 0.32)
morning(a + 0.6, b - 0.2, tune=True)
# The stakes: a low drone, a heartbeat and coins tinkling away.
a, b = S["stakes"]
x = ts(b - a)
put(music, (np.sin(2 * np.pi * hz(41) * x) + 0.5 * np.sin(2 * np.pi * hz(48) * x)) * np.minimum(x / 0.3, 1) * np.minimum((b - a - x) / 0.4, 1), a, 0.18)
for i in range(int(b - a)):
    put(music, kick(), a + i, 0.4)
    put(music, kick(), a + i + 0.25, 0.22)
for i in range(16):
    put(music, pluck(88 - (i % 7) * 2, 0.2, 0.05), a + 0.3 + i * 0.38, 0.05)

# The four tricks: the hour's chime, the crook sneaking in, a hush while Peri warns, a bright hit when it's stopped,
# the morning groove again, and a sad trombone for the crook.
CLOCK = {"e1": [72, 76, 79, 84], "e2": [74, 77, 81, 86], "e3": [71, 74, 79, 83], "e4": [69, 72, 76, 81]}
# The moment each trick is stopped: the end of the line that stops it (Ana's "I never sent that", "Hang up now",
# "Don't scan it"), or the start of Ana's "No." for the family key.
STOPPED = {"e1": (3, 1), "e2": (3, 1), "e3": (1, 1), "e4": (7, 0)}
for e, (stop_line, edge) in STOPPED.items():
    a, b = S[e]
    sp = spoken(e)
    put(music, chime(CLOCK[e], 0.1), a + 0.15, 0.3)
    first_peri = next(s for s in sp if s[2]["who"] == "peri")[0]
    stopped = sp[stop_line][edge]
    sneak(a + 1.6, first_peri - 0.2)
    if e == "e4":
        explained = sp[3][0] - 0.3
        hush(first_peri - 0.3, explained)
        family(explained, stopped - 0.2)
    else:
        hush(first_peri - 0.3, stopped)
    put(music, impact(), stopped, 0.35)
    put(music, brass([60, 64, 67, 72], 0.9), stopped, 0.22)
    morning(stopped + 0.5, b - 0.6, gain=0.55)
    put(music, trombone(58), sp[-1][1] + 0.15, 0.42)

# How Peri decides: a driving arpeggio, and sparkles while her Muse Image pictures pile up.
a, b = S["how"]
put(music, impact(), a, 0.4)
ARP = [[50, 57, 62, 65], [46, 53, 58, 62], [48, 55, 60, 64], [45, 52, 57, 61]]
t, bar = a, 0
while t < b - 0.6:
    notes = ARP[bar % 4]
    put(music, bass(notes[0] - 12, 2.0), t, 0.32)
    for s16 in range(16):
        if t + s16 * BEAT / 4 < b - 0.6:
            put(music, pluck(notes[s16 % 4] + 12, 0.16, 0.05), t + s16 * BEAT / 4, 0.08)
    for b4 in range(4):
        put(music, kick(), t + b4 * BEAT, 0.38)
        put(music, hat(), t + b4 * BEAT + BEAT / 2, 0.1)
    bar, t = bar + 1, t + 4 * BEAT
montage = spoken("how")[4][0]
for i in range(16):
    put(music, bell(84 + [0, 4, 7, 12, 7, 4][i % 6], 1.0, 0.4), montage + i * 0.28, 0.12)

# Night: a tender lullaby, silence but a heartbeat at "I've fallen", warmth from the moment Ana answers.
a, b = S["night"]
sp = spoken("night")
fallen, ana = sp[1][0], sp[3][0]
put(music, chime([72, 76, 79], 0.12), a + 0.15, 0.25)
lullaby(a + 0.4, fallen - 0.2, 0.16)
x = ts(ana - fallen)
put(music, np.sin(2 * np.pi * hz(41) * x) * np.minimum(x / 0.5, 1) * np.minimum((ana - fallen - x) / 0.3 + 0.01, 1), fallen, 0.14)
for i in range(int((ana - fallen) / 0.9)):
    put(music, kick(), fallen + 0.4 + i * 0.9, 0.2)
put(music, pad([53, 60, 65, 69, 72], b - ana + 0.5), ana, 0.22)
for i, n in enumerate([77, 76, 72, 74, 72, 69, 70, 72, 69, 65]):
    put(music, bell(n + 12, 1.6, 0.9), ana + 0.8 + i * 0.6, 0.13)
# Close: back at her desk, the lullaby resolves into one last run of the music box.
a, b = S["close"]
lullaby(a + 0.2, b - 3.2, 0.2)
put(music, chime([77, 81, 84, 89, 93], 0.15, 0.9), b - 3.3, 0.3)
put(music, pad([53, 57, 60, 65, 69], 3.3), b - 3.3, 0.3)

# The places' own sounds, under the music: crickets outside Peri's window, birds on her hill and round Rosa's cottage,
# the kitchen clock, the town in the afternoon, birds and the first crickets at sunset, crickets and an owl at night.
peak = np.abs(music).max()


def lay(sig, start, end, gain):
    """A place's sound from start to end, fading in and out over 0.6 s."""
    n = min(len(sig), int(SR * (end - start)))
    i = np.arange(n)
    put(music, sig[:n] * np.minimum(1, np.minimum(i / (0.6 * SR), (n - i) / (0.6 * SR))), start, gain * peak)


def span(name):
    a, b = S[name]
    return a, b + 0.5, b + 0.5 - a


a, b, d = span("open")
lay(crickets(d, 11), a, b, 0.05)
a, b, d = span("title")
lay(birds(d, 1.2, 12), a, b, 0.09)
lay(breeze(d, seed=13), a, b, 0.05)
a, b, d = span("e1")
lay(birds(d, 1.0, 14), a, b, 0.08)
a, b, d = span("e2")
lay(wall_clock(d), a, b, 0.07)
lay(birds(d, 0.5, 15), a, b, 0.04)
a, b, d = span("e3")
lay(breeze(d, 25, 16), a, b, 0.06)
lay(birds(d, 0.6, 17), a, b, 0.05)
a, b, d = span("e4")
lay(birds(d, 0.6, 18), a, b, 0.06)
lay(crickets(d, 19), a, b, 0.035)
a, b, d = span("night")
lay(crickets(d, 20), a, b, 0.06)
lay(breeze(d, seed=21), a, b, 0.035)
put(music, owl(), a + 0.5, 0.08 * peak)
a, b, d = span("close")
lay(crickets(d, 22), a, b - 0.5, 0.04)

music = music[: int(SR * LENGTH)]
fade = int(SR * 1.5)
music[-fade:] *= np.linspace(1, 0, fade)
save(folder / "music.wav", music, 0.85)
sfx = folder / "sfx"
sfx.mkdir(exist_ok=True)
save(sfx / "page.wav", page_turn(), 0.7)
save(sfx / "pencil.wav", scribble(1.4), 0.5)
save(sfx / "twinkle.wav", chime([84, 88, 91, 96], 0.07), 0.6)
save(sfx / "flick.wav", flick(), 0.6)
save(sfx / "clock.wav", clock_spin(), 0.55)
save(sfx / "close.wav", book_close(), 0.8)
print("music", LENGTH, "s; tricks stopped at", [round(spoken(e)[n][k], 1) for e, (n, k) in STOPPED.items()])
