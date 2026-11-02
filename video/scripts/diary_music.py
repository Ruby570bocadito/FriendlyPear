# The music of "Peri's diary", the second 3-minute video, made from scratch with the instruments in music_kit.py and
# placed on its script (video/public/diary): a music-box lullaby while Peri writes at night and, by day, Peri's theme,
# a friendly little band (a strummed ukulele, a plucked upright bass, an egg shaker, finger snaps, a glockenspiel and a
# whistled tune) that keeps the day warm. Each trick of the day gets a chime for the hour, the crook's cartoon sneak (a
# creeping bass with a marimba tiptoeing), a soft hush while Peri warns Rosa, a bright ta-da when the trick is stopped
# and a sad trombone for the crook; the family key gets the theme warmly, how Peri decides a busier version with
# sparkles for the Muse Image montage, and the night a tender lullaby that falls silent at "I've fallen" and warms up
# when Ana answers. The places' own sounds play underneath: crickets, birds, wind chimes, a wall clock, an owl.
#   python video/scripts/diary_music.py  ->  video/public/diary/music.wav
import json
import subprocess
from pathlib import Path

import numpy as np

from music_kit import SR, bell, birds, breeze, chime, crickets, hz, impact, kick, marimba, owl, pad, put, save, shaker, snap, strum, trombone, ts, upright, wall_clock, whistle, wind_chimes

LENGTH = 180.0
BEAT = 0.5
LOUDNESS = -24.9  # LUFS of the whole music track, as in v6, so it sits under the voices as before
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


# Every part of the band plays on its own track. At the end each track is brought to a fixed loudness against the
# others (the RMS of the part while it plays, dBFS), so no instrument can come out too loud or too quiet.
LEVELS = {"tune": -20, "box": -21, "uke": -23, "sneak": -23, "hits": -21, "bass": -25, "drone": -25, "arp": -27, "kick": -28, "pad": -28, "glock": -28, "snap": -33, "shaker": -36}
tracks = {part: np.zeros(int(SR * (LENGTH + 2))) for part in LEVELS}
LULL = [65, 69, 72, 77, 76, 72, 74, 72, 69, 70, 69, 65, 67, 69, 72, 69]
# Peri's theme: C, G, A minor, F, a chord a bar, strummed down, down-up, up, down-up; her tune on top.
CHORDS = ["C", "G", "Am", "F"]
ROOTS = [48, 43, 45, 41]
STRUM = [(0, False), (1, False), (1.5, True), (2.5, True), (3, False), (3.5, True)]
TUNE = [[79, None, 81, 79, 76, None, 74, None], [74, None, 76, 74, 71, None, 67, None], [72, None, 74, 76, 79, None, 76, None], [77, 76, 74, None, 72, None, None, None]]
ARP = {"C": [72, 76, 79, 84], "G": [71, 74, 79, 83], "Am": [72, 76, 81, 84], "F": [72, 77, 81, 84]}


def lullaby(start, end, gain=1.0):
    """The night's music box in F, over soft chords."""
    chords = [[53, 57, 60], [58, 62, 65], [55, 60, 64], [53, 57, 60]]
    t, i = start, 0
    while t < end - 0.4:
        if i % 4 == 0:
            put(tracks["pad"], pad(chords[(i // 4) % 4], min(2.6, end - t + 0.3), 0.5, 0.8), t, gain)
        put(tracks["box"], bell(LULL[i % len(LULL)] + 12, 1.6, 0.8), t, gain)
        t, i = t + 0.6, i + 1


def theme(start, end, gain=1.0, tune=False, busy=False):
    """Peri's theme, the friendly bed of the day: the strummed ukulele, the upright bass on the root and the fifth,
    the shaker on every eighth, snaps on 2 and 4 and a glockenspiel twinkle every other bar; [tune] whistles her tune
    on top, [busy] adds a soft kick on every beat and a marimba running up and down the chord."""
    t, bar = start, 0
    while t < end - 0.05:
        chord, root = CHORDS[bar % 4], ROOTS[bar % 4]
        for k, (beat, up) in enumerate(STRUM):
            nxt = STRUM[k + 1][0] if k + 1 < len(STRUM) else 4
            if t + beat * BEAT < end:
                put(tracks["uke"], strum(chord, min(nxt - beat, (end - t) / BEAT - beat) * BEAT, up, bar * 8 + k), t + beat * BEAT, gain)
        for b8 in range(8):
            if t + b8 * BEAT / 2 < end:
                put(tracks["shaker"], shaker(1.0 if b8 % 2 else 0.55), t + b8 * BEAT / 2, gain)
                if busy:
                    for s in range(2):
                        put(tracks["arp"], marimba(ARP[chord][[0, 1, 2, 3, 2, 1, 2, 1][b8] if s == 0 else [1, 2, 3, 2, 3, 2, 1, 2][b8]], 0.4), t + b8 * BEAT / 2 + s * BEAT / 4, 0.5 * gain)
        for b4 in range(4):
            at = t + b4 * BEAT
            if at >= end:
                break
            if b4 in (1, 3):
                put(tracks["snap"], snap(), at, gain)
            if b4 in (0, 2):
                put(tracks["bass"], upright(root if b4 == 0 else root + 7, 0.95), at, gain)
            if busy:
                put(tracks["kick"], kick(), at, gain)
        if bar % 2 == 0:
            put(tracks["glock"], bell(ARP[chord][-1] + 12, 1.6, 0.8), t + 0.02, 0.6 * gain)
        if tune:
            put(tracks["tune"], whistle(TUNE[bar % 4], BEAT / 2), t, gain)
        bar, t = bar + 1, t + 4 * BEAT


def sneak(start, end):
    """The crook tiptoeing in, a cartoon sneak rather than a threat: a creeping pizzicato bass, a marimba tiptoeing
    on the off-beats three octaves up, and finger snaps."""
    walk = [[38, 41, 44, 45], [38, 41, 45, 44], [43, 46, 49, 50], [45, 44, 43, 40]]
    t, bar = start, 0
    while t < end - 0.1:
        for b4 in range(4):
            at = t + b4 * BEAT
            if at >= end - 0.1:
                break
            n = walk[bar % 4][b4]
            put(tracks["sneak"], upright(n, 0.45), at, 1.0)
            put(tracks["sneak"], marimba(n + 36, 0.3), at + BEAT / 2, 0.3)
            if b4 in (1, 3):
                put(tracks["snap"], snap(), at, 0.8)
        bar, t = bar + 1, t + 4 * BEAT


def hush(start, end, notes=(50, 53, 57, 60)):
    """Danger: everything drops to a soft, uneasy chord while Peri speaks."""
    if end > start:
        put(tracks["pad"], pad(list(notes), end - start, 0.3, 0.4), start, 1.0)


def tada(at):
    """A trick stopped: a bright ukulele strum and a glockenspiel run up to the top C, over a soft boom."""
    put(tracks["hits"], impact(), at, 0.6)
    put(tracks["uke"], strum("C", 1.4), at, 1.3)
    for i, n in enumerate([84, 88, 91, 96]):
        put(tracks["glock"], bell(n, 1.4, 0.6), at + i * 0.07, 1.0)


# Night: Peri writes her diary to a music box, and a twinkle carries us into the morning.
a, b = S["open"]
lullaby(a, b - 0.5)
put(tracks["glock"], chime([77, 81, 84, 89, 93], 0.06), b - 0.6, 1.0)
# Morning: Peri drops onto the meadow (a soft bump as she lands), and her theme starts, whistled.
a, b = S["title"]
put(tracks["hits"], impact(), a + 0.57, 0.6)
theme(a + 0.6, b - 0.2, tune=True)
# The stakes: a low drone, a heartbeat and coins tinkling away.
a, b = S["stakes"]
x = ts(b - a)
put(tracks["drone"], (np.sin(2 * np.pi * hz(41) * x) + 0.5 * np.sin(2 * np.pi * hz(48) * x)) * np.minimum(x / 0.3, 1) * np.minimum((b - a - x) / 0.4, 1), a, 1.0)
for i in range(int(b - a)):
    put(tracks["drone"], kick(), a + i, 2.2)
    put(tracks["drone"], kick(), a + i + 0.25, 1.2)
for i in range(16):
    put(tracks["glock"], bell(88 - (i % 7) * 2, 0.4, 0.1), a + 0.3 + i * 0.38, 0.5)

# The four tricks: the hour's chime, the crook sneaking in, a hush while Peri warns, a ta-da when it's stopped, the
# theme again, and a sad trombone for the crook.
CLOCK = {"e1": [72, 76, 79, 84], "e2": [74, 77, 81, 86], "e3": [71, 74, 79, 83], "e4": [69, 72, 76, 81]}
# The moment each trick is stopped: the end of the line that stops it (Ana's "I never sent that", "Hang up now",
# "Don't scan it"), or the start of Ana's "No." for the family key.
STOPPED = {"e1": (3, 1), "e2": (3, 1), "e3": (2, 1), "e4": (7, 0)}
for e, (stop_line, edge) in STOPPED.items():
    a, b = S[e]
    sp = spoken(e)
    put(tracks["glock"], chime(CLOCK[e], 0.1), a + 0.15, 1.0)
    first_peri = next(s for s in sp if s[2]["who"] == "peri")[0]
    stopped = sp[stop_line][edge]
    sneak(a + 1.6, first_peri - 0.2)
    if e == "e4":
        explained = sp[3][0] - 0.3
        hush(first_peri - 0.3, explained)
        theme(explained, stopped - 0.2, 0.75)
    else:
        hush(first_peri - 0.3, stopped)
    tada(stopped)
    theme(stopped + 0.5, b - 0.6, 0.6)
    put(tracks["hits"], trombone(58), sp[-1][1] + 0.15, 1.0)

# How Peri decides: her theme, busier, and sparkles while her Muse Image pictures pile up.
a, b = S["how"]
put(tracks["hits"], impact(), a, 0.7)
theme(a, b - 0.6, busy=True)
montage = spoken("how")[4][0]
for i in range(16):
    put(tracks["glock"], bell(84 + [0, 4, 7, 12, 7, 4][i % 6], 1.0, 0.4), montage + i * 0.28, 0.9)

# Night: a tender lullaby, silence but a heartbeat at "I've fallen", warmth from the moment Ana answers.
a, b = S["night"]
sp = spoken("night")
fallen, ana = sp[1][0], sp[3][0]
put(tracks["glock"], chime([72, 76, 79], 0.12), a + 0.15, 0.8)
lullaby(a + 0.4, fallen - 0.2, 0.75)
x = ts(ana - fallen)
put(tracks["drone"], np.sin(2 * np.pi * hz(41) * x) * np.minimum(x / 0.5, 1) * np.minimum((ana - fallen - x) / 0.3 + 0.01, 1), fallen, 0.8)
for i in range(int((ana - fallen) / 0.9)):
    put(tracks["drone"], kick(), fallen + 0.4 + i * 0.9, 1.1)
put(tracks["pad"], pad([53, 60, 65, 69, 72], b - ana + 0.5), ana, 1.0)
for i, n in enumerate([77, 76, 72, 74, 72, 69, 70, 72, 69, 65]):
    put(tracks["box"], bell(n + 12, 1.6, 0.9), ana + 0.8 + i * 0.6, 0.6)
# Close: back at her desk, the lullaby resolves into one last run of the music box.
a, b = S["close"]
lullaby(a + 0.2, b - 3.2, 0.9)
put(tracks["glock"], chime([77, 81, 84, 89, 93], 0.15, 0.9), b - 3.3, 1.0)
put(tracks["pad"], pad([53, 57, 60, 65, 69], 3.3), b - 3.3, 1.2)


def rms_db(sig):
    """How loud a part is while it plays: the RMS of its 50 ms frames within 40 dB of its loudest, in dBFS."""
    hop = int(SR * 0.05)
    frames = np.sqrt(np.mean(sig[: len(sig) // hop * hop].reshape(-1, hop) ** 2, axis=1))
    playing = frames[frames > frames.max() * 0.01]
    return 20 * np.log10(np.sqrt(np.mean(playing**2)) + 1e-12)


music = np.zeros_like(tracks["uke"])
for part, target in LEVELS.items():
    if np.abs(tracks[part]).max() > 0:
        music += tracks[part] * 10 ** ((target - rms_db(tracks[part])) / 20)

# The places' own sounds, under the music: crickets outside Peri's window, birds and wind chimes in her meadow,
# birds round Rosa's cottage, the kitchen clock, the town in the afternoon, birds, chimes and the first crickets at
# sunset in the garden, crickets and an owl at night.
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
lay(birds(d, 1.2, 12), a, b, 0.1)
lay(breeze(d, seed=13), a, b, 0.05)
lay(wind_chimes(d, 0.6, 31), a, b, 0.06)
a, b, d = span("e1")
lay(birds(d, 1.0, 14), a, b, 0.09)
a, b, d = span("e2")
lay(wall_clock(d), a, b, 0.07)
lay(birds(d, 0.5, 15), a, b, 0.05)
a, b, d = span("e3")
lay(breeze(d, 25, 16), a, b, 0.06)
lay(birds(d, 0.6, 17), a, b, 0.06)
a, b, d = span("e4")
lay(birds(d, 0.6, 18), a, b, 0.07)
lay(wind_chimes(d, 0.4, 32), a, b, 0.06)
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
# As loud overall as LOUDNESS (measured with ffmpeg's EBU R128 meter), peaks kept under 0.89.
save(folder / "music.wav", music, 0.85)
meter = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", str(folder / "music.wav"), "-af", "ebur128", "-f", "null", "-"], capture_output=True, text=True).stderr
measured = float(meter.rsplit("I:", 1)[1].split("LUFS")[0])
save(folder / "music.wav", music, min(0.89, 0.85 * 10 ** ((LOUDNESS - measured) / 20)))
print("music", LENGTH, "s;", round(measured, 1), "LUFS before levelling; tricks stopped at", [round(spoken(e)[n][k], 1) for e, (n, k) in STOPPED.items()])
