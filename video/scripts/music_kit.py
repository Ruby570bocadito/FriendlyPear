# The instruments and sound effects of the videos' music, made from scratch (no samples): plucks, bass, drums, brass,
# pads, risers, impacts, a sad trombone and, for "Peri's diary", a music box, chimes, a page turn and a pencil.
# final_music.py and diary_music.py play them.
import wave

import numpy as np

SR = 44100
rng = np.random.default_rng(23)


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


def pluck(n, dur=0.3, decay=0.08):
    x = ts(dur)
    f = hz(n)
    return (np.sin(2 * np.pi * f * x) + 0.5 * np.sin(4 * np.pi * f * x) + 0.2 * np.sin(6 * np.pi * f * x)) * env(len(x), 0.002, decay)


def bass(n, dur=0.45):
    x = ts(dur)
    f = hz(n)
    s = sum(np.sin(2 * np.pi * f * k * x) / k ** 1.4 for k in range(1, 6))
    return s * env(len(x), 0.004, 0.18)


def kick():
    x = ts(0.35)
    return np.sin(2 * np.pi * (48 + 110 * np.exp(-x / 0.035)) * x) * np.exp(-x / 0.11)


def snare():
    x = ts(0.22)
    noise = np.convolve(rng.standard_normal(len(x)), [1, -0.6], mode="same")
    return (0.7 * noise + 0.5 * np.sin(2 * np.pi * 190 * x)) * np.exp(-x / 0.07)


def hat(open_=False):
    n = np.diff(rng.standard_normal(int(SR * (0.25 if open_ else 0.05))), prepend=0)
    return n * env(len(n), 0.001, 0.08 if open_ else 0.015)


def snap():
    n = np.diff(rng.standard_normal(int(SR * 0.05)), prepend=0)
    return n * env(len(n), 0.001, 0.012)


def brass(notes, dur=0.35):
    x = ts(dur)
    s = sum(sum(np.sin(2 * np.pi * hz(n) * k * x) / k for k in range(1, 9)) for n in notes) / len(notes)
    return s * np.minimum(x / 0.015, 1) * np.exp(-x / (dur / 2.3))


def pad(notes, dur, fade_in=0.4, fade_out=0.6):
    x = ts(dur)
    s = sum(np.sin(2 * np.pi * hz(n) * x + 0.3 * np.sin(2 * np.pi * 0.3 * x)) for n in notes) / len(notes)
    return s * np.minimum(np.minimum(x / fade_in, 1), np.minimum((dur - x) / fade_out, 1))


def riser(dur=1.4):
    x = ts(dur)
    noise = np.convolve(rng.standard_normal(len(x)), np.ones(6) / 6, mode="same")
    tone = np.sin(2 * np.pi * np.cumsum(200 + 1400 * (x / dur) ** 2) / SR)
    return (0.6 * noise + 0.4 * tone) * (x / dur) ** 2


def impact():
    x = ts(1.6)
    boom = np.sin(2 * np.pi * (40 + 70 * np.exp(-x / 0.06)) * x) * np.exp(-x / 0.45)
    crash = np.diff(rng.standard_normal(len(x)), prepend=0) * np.exp(-x / 0.5) * 0.35
    return boom + crash


def trombone(start_note=58):
    out = np.zeros(int(SR * 2.0))
    for i, n in enumerate([start_note, start_note - 1, start_note - 2]):
        x = ts(0.3)
        put(out, sum(np.sin(2 * np.pi * hz(n) * k * x) / k ** 1.5 for k in range(1, 6)) * env(len(x), 0.02, 0.22), i * 0.32)
    x = ts(0.95)
    wob = hz(start_note - 3) * (1 + 0.025 * np.sin(2 * np.pi * 6 * x)) * (1 - 0.03 * x)
    put(out, sum(np.sin(2 * np.pi * np.cumsum(wob) * k / SR) / k ** 1.5 for k in range(1, 6)) * np.minimum(x / 0.03, 1) * np.exp(-x / 0.6), 0.96)
    return out



def bell(n, dur=1.6, decay=0.7):
    """A music-box tine: a clear note with a bright, quickly fading shimmer above it."""
    x = ts(dur)
    f = hz(n)
    s = np.sin(2 * np.pi * f * x) + 0.35 * np.sin(2 * np.pi * f * 2.76 * x) * np.exp(-x / 0.15) + 0.18 * np.sin(2 * np.pi * f * 5.4 * x) * np.exp(-x / 0.06)
    return s * env(len(x), 0.002, decay)


def chime(notes, gap=0.12, decay=0.5):
    """A little run of music-box notes."""
    out = np.zeros(int(SR * (gap * len(notes) + 1.6)))
    for i, n in enumerate(notes):
        put(out, bell(n, 1.4, decay), i * gap, 0.8)
    return out


def page_turn():
    """A page turning: a breathy swish that rises and falls, and a soft tap as it lands."""
    x = ts(0.55)
    smooth = np.convolve(rng.standard_normal(len(x)), np.ones(8) / 8, mode="same")
    swish = np.diff(smooth, prepend=0) * np.sin(np.pi * np.clip(x / 0.45, 0, 1)) ** 2
    tap = np.sin(2 * np.pi * 140 * x) * np.exp(-np.clip(x - 0.42, 0, None) / 0.03) * (x > 0.42)
    return swish * 1.5 + tap * 0.4


def chirp(f0, f1, dur):
    """One bird note, gliding from f0 to f1."""
    x = ts(dur)
    f = np.linspace(f0, f1, len(x))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * np.clip(x / dur, 0, 1)) ** 2


def birds(dur, density=1.0, seed=1):
    """A few birds in the garden: little runs of chirps at random, each bird in its own range."""
    r = np.random.default_rng(seed)
    out = np.zeros(int(SR * dur))
    t = r.uniform(0, 0.8)
    while t < dur - 0.6:
        base = [2600, 3400, 4300][r.integers(0, 3)]
        for k in range(r.integers(2, 6)):
            f0 = base * r.uniform(0.9, 1.15)
            f1 = f0 * (r.uniform(1.15, 1.5) if r.random() < 0.6 else r.uniform(0.7, 0.9))
            put(out, chirp(f0, f1, r.uniform(0.05, 0.11)), t + k * r.uniform(0.09, 0.16), r.uniform(0.3, 0.6))
        t += r.uniform(0.6, 2.2) / density
    return out / max(1e-9, np.abs(out).max())


def crickets(dur, seed=2):
    """Three crickets, each chirping in little pulses at its own pitch and pace."""
    r = np.random.default_rng(seed)
    out = np.zeros(int(SR * dur))
    for _ in range(3):
        f, rate, g = r.uniform(4200, 5200), r.uniform(2.2, 3.2), r.uniform(0.5, 1.0)
        t = r.uniform(0, 0.5)
        while t < dur - 0.2:
            for p in range(r.integers(3, 5)):
                x = ts(0.018)
                put(out, np.sin(2 * np.pi * f * x) * np.sin(np.pi * x / 0.018), t + p * 0.03, g)
            t += r.uniform(0.9, 1.1) / rate
    return out / max(1e-9, np.abs(out).max())


def breeze(dur, smooth=60, seed=3):
    """Soft air: smoothed noise that swells and fades (a breeze outdoors, a distant town)."""
    r = np.random.default_rng(seed)
    k = np.ones(smooth) / smooth
    b = np.convolve(np.convolve(r.standard_normal(int(SR * dur)), k, mode="same"), k, mode="same")
    b *= 0.6 + 0.4 * np.sin(2 * np.pi * 0.13 * ts(dur) + r.uniform(0, 6))
    return b / max(1e-9, np.abs(b).max())


def wall_clock(dur, every=1.0):
    """A wall clock: tick, tock."""
    out = np.zeros(int(SR * dur))
    for i in range(int(dur / every)):
        x = ts(0.03)
        click = np.diff(np.random.default_rng(i).standard_normal(len(x)), prepend=0) * np.exp(-x / 0.004)
        put(out, click, i * every, 1.0 if i % 2 else 0.7)
    return out / max(1e-9, np.abs(out).max())


def owl():
    """An owl far off: hoo, hoo-hooo."""
    out = np.zeros(int(SR * 1.6))
    for at, d in ((0.0, 0.35), (0.5, 0.25), (0.8, 0.55)):
        x = ts(d)
        f = 420 * (1 - 0.06 * x / d)
        put(out, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * x / d) ** 1.5 * (1 + 0.3 * np.sin(2 * np.pi * 6 * x)), at, 1.0)
    return out / max(1e-9, np.abs(out).max())


def flick():
    """A photo dealt onto the pile: a papery flick and a soft tap."""
    x = ts(0.16)
    smooth = np.convolve(rng.standard_normal(len(x)), np.ones(3) / 3, mode="same")
    swish = np.diff(smooth, prepend=0) * np.exp(-x / 0.03)
    tap = np.sin(2 * np.pi * 160 * x) * np.exp(-np.clip(x - 0.05, 0, None) / 0.02) * (x > 0.05)
    return swish * 2.2 + tap * 0.5


def clock_spin():
    """Time passing on a little clock: ticks speeding up, and a tiny ding."""
    out = np.zeros(int(SR * 1.6))
    t, gap = 0.0, 0.16
    for i in range(10):
        x = ts(0.03)
        put(out, np.diff(np.random.default_rng(40 + i).standard_normal(len(x)), prepend=0) * np.exp(-x / 0.004), t, 0.8)
        t, gap = t + gap, max(0.045, gap * 0.8)
    put(out, bell(96, 1.0, 0.35), t + 0.05, 0.35)
    return out


def book_close():
    """A notebook closing: a soft papery clap and a low thump."""
    x = ts(0.5)
    smooth = np.convolve(rng.standard_normal(len(x)), np.ones(5) / 5, mode="same")
    clap = np.diff(smooth, prepend=0) * np.exp(-x / 0.05)
    thump = np.sin(2 * np.pi * (70 + 60 * np.exp(-x / 0.03)) * x) * np.exp(-x / 0.12)
    return clap * 1.6 + thump


def scribble(dur=1.2):
    """A pencil writing: quick scratchy strokes."""
    out = np.zeros(int(SR * dur))
    t = 0.0
    while t < dur - 0.1:
        n = int(SR * (0.05 + rng.random() * 0.08))
        stroke = np.diff(rng.standard_normal(n), prepend=0) * np.sin(np.pi * np.arange(n) / n)
        put(out, stroke, t, 0.25 + rng.random() * 0.2)
        t += 0.06 + rng.random() * 0.1
    return out


# Warmer, more acoustic instruments for "Peri's diary": a ukulele, an upright bass, a marimba, a shaker, a whistle
# and wind chimes, so the music sounds played rather than beeped.


def soften(sig, cutoff, order=2):
    """Roll off the highs above [cutoff] Hz: a smooth low-pass, done in one go in the frequency domain."""
    f = np.fft.rfftfreq(len(sig), 1 / SR)
    return np.fft.irfft(np.fft.rfft(sig) / np.sqrt(1 + (f / cutoff) ** (2 * order)), len(sig))


def string(n, dur, bright=0.5, ring=1.5, seed=0, body=5000):
    """A plucked string (Karplus-Strong): a burst of noise circling a delay one period long, softened a little on
    every pass, so it rings at the note and fades like a real string, the high harmonics first. [bright] is how hard
    it's plucked (a pick near 1, a fingertip near 0), [ring] the seconds it takes to die away and [body] where the
    instrument's wooden body rolls off the highs."""
    f = hz(n)
    period = max(2, int(SR / f - 0.5))
    made = SR / (period + 0.5)  # the pitch a whole-sample delay really rings at; played back tuned below
    length = int(SR * dur * made / f) + period + 2
    r = np.random.default_rng(seed)
    burst = soften(r.uniform(-1, 1, period + 1), 400 + 5000 * bright)
    burst /= max(1e-9, np.abs(burst).max())
    out = np.zeros(length)
    out[: period + 1] = burst - burst.mean()
    loss = 10 ** (-3 / (ring * made))
    i = period + 1
    while i < length:
        j = min(i + period, length)
        out[i:j] = loss * 0.5 * (out[i - period : j - period] + out[i - period - 1 : j - period - 1])
        i = j
    x = np.arange(int(SR * dur)) * f / made
    sig = soften(np.interp(x, np.arange(length), out), body, 1)
    fade = min(len(sig), int(SR * 0.04))
    sig[len(sig) - fade :] *= np.linspace(1, 0, fade)
    return sig


# The ukulele's chords, its four strings low to high (G C E A, the G tuned high as on a real uke).
UKE = {
    "C": [67, 60, 64, 72], "G": [67, 62, 67, 71], "Am": [69, 60, 64, 69], "F": [69, 60, 65, 69],
    "Dm": [69, 62, 65, 69], "E7": [71, 62, 64, 71], "Em": [67, 64, 67, 71], "G7": [67, 62, 65, 71],
}


def strum(chord, dur=0.5, up=False, seed=0):
    """One ukulele strum: down across all four strings (G to A, a hair apart), or a lighter up-stroke on the top
    three; it lasts [dur], until the next strum stops the strings."""
    notes = UKE[chord][::-1][:3] if up else UKE[chord]
    out = np.zeros(int(SR * (dur + 0.06)))
    for i, n in enumerate(notes):
        put(out, string(n, dur + 0.04, 0.25 if up else 0.35, 1.4, seed * 7 + i, 3500), i * (0.008 if up else 0.012), 0.6 if up else 1.0)
    return out


def upright(n, dur=0.5):
    """A pizzicato upright bass: a dark, round pluck that thumps and fades."""
    return string(n, dur, 0.05, 0.9, n, 900)


def marimba(n, dur=1.0):
    """A marimba bar hit with a yarn mallet: a warm wooden fundamental, its overtone two octaves up and a fainter
    one higher still that fade much sooner, and the mallet's soft knock."""
    x = ts(dur)
    f = hz(n)
    ring = 0.9 * (262 / f) ** 0.5
    s = np.sin(2 * np.pi * f * x) * np.exp(-6.9 * x / ring)
    s += 0.22 * np.sin(2 * np.pi * f * 3.99 * x) * np.exp(-6.9 * x / (ring * 0.25))
    if f * 9.9 < SR / 2.2:
        s += 0.05 * np.sin(2 * np.pi * f * 9.9 * x) * np.exp(-6.9 * x / (ring * 0.08))
    knock = np.convolve(rng.standard_normal(len(x)), np.ones(12) / 12, mode="same") * np.exp(-x / 0.004) * 0.6
    return (s + knock) * np.minimum(x / 0.0015, 1)


def shaker(accent=1.0):
    """An egg shaker: a hiss of beads, swelling for a moment and gone."""
    x = ts(0.1)
    noise = rng.standard_normal(len(x))
    hiss = noise - np.convolve(noise, np.ones(4) / 4, mode="same")
    return hiss * np.minimum(x / 0.014, 1) * np.exp(-x / 0.028) * accent


def whistle(tune, step, rest=0.06):
    """Someone whistling a tune: a pure tone that glides between notes, a gentle vibrato once a note settles and a
    breath of air. [tune] has one note (or None for a rest) every [step] seconds; a held note is the same number
    repeated."""
    n = int(SR * (len(tune) * step + 0.3))
    target = np.full(n, np.nan)
    loud = np.zeros(n)
    for i, note in enumerate(tune):
        if note is not None:
            a, b = int(SR * i * step), int(SR * (i + 1) * step)
            target[a:b] = note
            end_of_phrase = i + 1 >= len(tune) or tune[i + 1] is None
            loud[a : b - (int(SR * rest) if end_of_phrase else 0)] = 1
    # Hold the last pitch through rests (and the first one before it starts), so each note slides in from the last.
    first = np.flatnonzero(~np.isnan(target))[0]
    target[:first] = target[first]
    idx = np.where(np.isnan(target), 0, np.arange(n))
    target = target[np.maximum.accumulate(idx)]
    # The pitch glides into each note in about 25 ms and the sound swells and fades in about 20 ms.
    block = 64
    to_pitch, to_level = 1 - np.exp(-block / (SR * 0.025)), 1 - np.exp(-block / (SR * 0.02))
    g, level = target[0], 0.0
    glide, amp = np.zeros(n), np.zeros(n)
    for i in range(0, n, block):
        g += to_pitch * (target[i] - g)
        level += to_level * (loud[i] - level)
        glide[i : i + block], amp[i : i + block] = g, level
    x = np.arange(n) / SR
    f = 440.0 * 2 ** ((glide - 69) / 12) * (1 + 0.007 * np.sin(2 * np.pi * 5.6 * x) * np.clip(amp, 0, 1))
    phase = 2 * np.pi * np.cumsum(f) / SR
    tone = np.sin(phase) + 0.04 * np.sin(2 * phase)
    air = np.convolve(rng.standard_normal(n), np.ones(3) / 3, mode="same") * 0.05
    return (tone + air) * amp


def wind_chimes(dur, density=0.5, seed=6):
    """Wind chimes in the garden: six tuned tubes (C, D, E, G, A, C), knocked now and then by the breeze in little
    clusters, each tube ringing long with its bell-like overtones."""
    r = np.random.default_rng(seed)
    tubes = [84, 86, 88, 91, 93, 96]
    out = np.zeros(int(SR * dur))
    t = r.uniform(0.2, 1.5)
    while t < dur - 0.5:
        for k in range(r.integers(2, 6)):
            n = tubes[r.integers(0, len(tubes))]
            x = ts(3.0)
            f = hz(n)
            tube = (np.sin(2 * np.pi * f * x) + 0.4 * np.sin(2 * np.pi * f * 2.76 * x) * np.exp(-x / 0.6) + 0.15 * np.sin(2 * np.pi * f * 5.4 * x) * np.exp(-x / 0.25)) * env(len(x), 0.001, 1.1)
            put(out, tube, t + k * r.uniform(0.08, 0.22), r.uniform(0.25, 1.0))
        t += r.uniform(1.5, 4.0) / density
    return out / max(1e-9, np.abs(out).max())


def save(path, sig, peak=0.9):
    sig = sig / max(1e-9, np.max(np.abs(sig))) * peak
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((sig * 32767).astype(np.int16).tobytes())
