# The 3-minute videos' voices (video/public/final, "the heist", or another folder such as diary, "Peri's diary"):
# the crook, Peri, Rosa and Ana, line by line (<folder>/lines.json) into vo_<scene>_<n>.wav, trimmed of silence; how
# long each lasts into durations.json, how loud it is frame by frame into mouths.json and which mouth shape it makes
# into visemes.json (for lip sync), which the video and its music read. For real voices, record the same files and run
# with --measure.
#
# A folder can choose its own voices in voices.json ({"crook": [voice, rate, pitch], ...}, plus "polish": true). A
# polished line is said from its "speak" text when it has one (the subtitle keeps "text": no shouted capitals for the
# voice, a breath where the line needs one), then made to sound less like a machine reading: pauses longer than
# 0.3 s inside it shortened to 0.2 s, a little warmth and presence, gentle compression and a small room, all lines at
# the same loudness. A line with "phone": true is heard through the phone instead: narrow band, a bit of grit, no room.
# With "sentences": true in voices.json each sentence is said on its own, as loud as the others, and they're joined
# with natural pauses (longest after "..."): a quiet "Beaten..." isn't buried by the shout after it, and the gaps stay
# short. Every character keeps one fixed voice, the same pitch and pace in every line: acting by moving the pitch
# made the voices sound like they kept changing.
#   python video/scripts/final_voice.py [--measure] [--folder=diary] [--only=e4_0,how_1]
import asyncio
import json
import re
import subprocess
import sys
import wave
from pathlib import Path

import edge_tts
import numpy as np

name = next((a.split("=", 1)[1] for a in sys.argv if a.startswith("--folder=")), "final")
only = next((a.split("=", 1)[1].split(",") for a in sys.argv if a.startswith("--only=")), None)
folder = Path(__file__).resolve().parent.parent / "public" / name
scenes = json.loads((folder / "lines.json").read_text(encoding="utf-8"))
VOICES = {
    "crook": ("en-US-ChristopherNeural", "+8%", "+6Hz"),
    "peri": ("en-US-AvaMultilingualNeural", "+4%", "+3Hz"),
    "rosa": ("en-GB-SoniaNeural", "+0%", "-5Hz"),
    "ana": ("en-US-EmmaMultilingualNeural", "+4%", "+0Hz"),
}
config = json.loads((folder / "voices.json").read_text()) if (folder / "voices.json").exists() else {}
VOICES.update({who: tuple(v) for who, v in config.items() if who in VOICES})
POLISH = bool(config.get("polish"))
SENTENCES = bool(config.get("sentences"))
# The pause after a sentence, by how it ends: longest after "...", shortest after a question or an exclamation.
PAUSE = {"...": 0.38, ".": 0.24, "?": 0.22, "!": 0.2}
TRIM = "silenceremove=start_periods=1:start_threshold=-45dB,areverse,silenceremove=start_periods=1:start_threshold=-45dB,areverse,apad=pad_dur=0.06"
# Warmth below 200 Hz, presence around 3 kHz, gentle compression and a small room's early reflections.
ROOM = "highpass=f=70,equalizer=f=180:t=q:w=1:g=1.5,equalizer=f=3200:t=q:w=1.2:g=2.5,acompressor=threshold=-22dB:ratio=2.5:attack=6:release=90:makeup=2,aecho=0.9:0.9:23|41|67:0.09|0.06|0.035"
# The far end of a phone call: 300-3400 Hz, a honky middle, a little grit and squeezed dynamics.
PHONE = "highpass=f=320,lowpass=f=3400,equalizer=f=1500:t=q:w=1:g=4,acompressor=threshold=-26dB:ratio=4:attack=3:release=60:makeup=4,asoftclip=type=tanh"


def tighten(path: Path, longest=0.3, keep=0.2):
    """Shorten every pause inside the line longer than [longest] seconds to [keep], with short crossfades."""
    with wave.open(str(path)) as w:
        params = w.getparams()
        pcm = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32)
    rate, hop = params.framerate, params.framerate // 100
    rms = np.array([np.sqrt(np.mean(pcm[i : i + hop] ** 2)) for i in range(0, len(pcm) - hop + 1, hop)])
    quiet = rms < max(rms.max() * 10 ** (-38 / 20), 60)
    pieces, start, i = [], 0, 0
    fade = int(rate * 0.008)
    while i < len(quiet):
        if quiet[i]:
            j = i
            while j < len(quiet) and quiet[j]:
                j += 1
            if i > 0 and j < len(quiet) and (j - i) * hop > longest * rate:
                cut_from = i * hop + int(keep * rate / 2)
                cut_to = j * hop - int(keep * rate / 2)
                pieces.append(pcm[start:cut_from])
                start = cut_to
            i = j
        else:
            i += 1
    pieces.append(pcm[start:])
    out = pieces[0]
    for piece in pieces[1:]:
        ramp = np.linspace(0, 1, min(fade, len(out), len(piece)))
        n = len(ramp)
        out = np.concatenate([out[: len(out) - n], out[len(out) - n :] * (1 - ramp) + piece[:n] * ramp, piece[n:]])
    with wave.open(str(path), "wb") as w:
        w.setparams(params)
        w.writeframes(np.clip(out, -32768, 32767).astype(np.int16).tobytes())


def level(path: Path, target=-20.0):
    """Every line equally loud: the RMS of its spoken part brought to [target] dBFS, peaks kept under -1 dBFS."""
    with wave.open(str(path)) as w:
        params = w.getparams()
        pcm = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32)
    voiced = pcm[np.abs(pcm) > 0.02 * np.abs(pcm).max()]
    gain = 10 ** ((target - 20 * np.log10(np.sqrt(np.mean(voiced ** 2)) / 32768)) / 20)
    gain = min(gain, 0.89 * 32768 / np.abs(pcm).max())
    with wave.open(str(path), "wb") as w:
        w.setparams(params)
        w.writeframes(np.clip(pcm * gain, -32768, 32767).astype(np.int16).tobytes())


def sentences(text):
    """A line cut into its sentences, each with the pause after it. A "..." on its own (as in "too?! ...I quit")
    only makes the pause before the next sentence longer."""
    out = []
    for part in re.findall(r"[^.!?]+(?:\.\.\.|[.!?]+)?", text):
        part = part.strip()
        if not re.search(r"[A-Za-z0-9]", part):
            if out and part.startswith("..."):
                out[-1] = (out[-1][0], PAUSE["..."])
            continue
        end = "..." if part.endswith("...") else part[-1] if part[-1] in "!?" else "."
        out.append((part, PAUSE[end]))
    return out


async def say(text, voice, rate, pitch, volume, path):
    """Edge saying [text] into [path]: a WAV at 44.1 kHz with the silence around it trimmed."""
    raw = path.with_suffix(".mp3")
    for attempt in range(4):
        try:
            await edge_tts.Communicate(text, voice, rate=rate, pitch=pitch, volume=volume).save(str(raw))
            break
        except edge_tts.exceptions.NoAudioReceived:
            if attempt == 3:
                raise
            await asyncio.sleep(2)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(raw), "-af", TRIM, "-ar", "44100", str(path)], check=True)
    raw.unlink()


def join(pieces, out: Path):
    """The sentences one after another, each followed by its pause (TRIM already left 0.06 s after each)."""
    pcm, params = [], None
    for path, pause in pieces:
        with wave.open(str(path)) as w:
            params = params or w.getparams()
            pcm += [np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16), np.zeros(int(params.framerate * max(0.0, pause - 0.06)), dtype=np.int16)]
        path.unlink()
    with wave.open(str(out), "wb") as w:
        w.setparams(params)
        w.writeframes(np.concatenate(pcm[:-1]).tobytes())


async def speak():
    for scene, lines in scenes.items():
        for n, line in enumerate(lines):
            if only and f"{scene}_{n}" not in only:
                continue
            voice, rate, pitch = VOICES[line["who"]]
            text = line.get("speak", line["text"]) if POLISH else line["text"]
            out = folder / f"vo_{scene}_{n}.wav"
            by_sentence = SENTENCES and POLISH
            if by_sentence:
                pieces = []
                for k, (sentence, pause) in enumerate(sentences(text)):
                    part = folder / f"part_{scene}_{n}_{k}.wav"
                    await say(sentence, voice, rate, pitch, "+0%", part)
                    tighten(part)
                    level(part, -20.0)
                    pieces.append((part, pause))
                join(pieces, out)
            else:
                await say(text, voice, rate, pitch, "+0%", out)
            if POLISH:
                if not by_sentence:
                    tighten(out)
                chain = PHONE if line.get("phone") else ROOM
                done = out.with_name(out.stem + "_done.wav")
                subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(out), "-af", f"{chain},apad=pad_dur=0.05", "-ar", "44100", str(done)], check=True)
                done.replace(out)
                level(out, -21.0 if line.get("phone") else -20.0)
            print(f"{scene}_{n}: {line['who']}")


if "--measure" not in sys.argv:
    asyncio.run(speak())
lengths = {}
for scene, lines in scenes.items():
    lengths[scene] = []
    for n in range(len(lines)):
        probe = ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(folder / f"vo_{scene}_{n}.wav")]
        lengths[scene].append(round(float(subprocess.check_output(probe, text=True)), 2))
(folder / "durations.json").write_text(json.dumps(lengths, indent=2))

# How open each speaker's mouth is, frame by frame (30 a second), from how loud each line is: the video picks the
# talking frame of whoever speaks when this is high, so their lips follow their words.

mouths = {}
for scene, lines in scenes.items():
    for n in range(len(lines)):
        with wave.open(str(folder / f"vo_{scene}_{n}.wav")) as w:
            pcm = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32)
            step = w.getframerate() // 30
        rms = np.array([np.sqrt(np.mean(pcm[i : i + step] ** 2)) for i in range(0, len(pcm) - step + 1, step)])
        peak = np.percentile(rms, 95) if len(rms) else 1
        mouths[f"{scene}_{n}"] = [round(float(v), 2) for v in np.clip(rms / max(peak, 1), 0, 1)]
(folder / "mouths.json").write_text(json.dumps(mouths))

# Which mouth shape each speaker makes on every video frame, calm enough to read like stop-motion: how open from how
# loud (quick to open, slower to close), a little sticky so it doesn't flicker between neighbouring shapes, every
# shape held at least HOLD frames, wide open only on a real peak, and round ("oh") or a grin ("ee") from where the
# sound's energy sits (its spectral centroid, low for o/u, high for e/i).
HOLD = 4
OPENS = [0.12, 0.36, 0.62, 0.9]  # loudness that opens the mouth past closed, slight, half and open
LEVELS = ["closed", "slight", "half", "open", "wide"]
visemes = {}
for scene, lines in scenes.items():
    for n in range(len(lines)):
        with wave.open(str(folder / f"vo_{scene}_{n}.wav")) as w:
            pcm = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32)
            rate = w.getframerate()
        step = rate // 30
        window = np.hanning(step)
        freqs = np.fft.rfftfreq(step, 1 / rate)
        band = (freqs > 80) & (freqs < 4000)
        cent = []
        for i in range(0, len(pcm) - step + 1, step):
            mag = np.abs(np.fft.rfft(pcm[i : i + step] * window))[band]
            cent.append(float((freqs[band] * mag).sum() / max(mag.sum(), 1e-6)))
        cent = np.array(cent)
        lo, hi = (np.percentile(cent, 10), np.percentile(cent, 90)) if len(cent) else (0, 1)
        cent = np.clip((cent - lo) / max(hi - lo, 1), 0, 1)
        loud = np.array(mouths[f"{scene}_{n}"])
        shapes, env, level, held, last_wide = [], 0.0, 0, HOLD, -99
        for k in range(len(loud)):
            env = loud[k] if loud[k] > env else 0.6 * env + 0.4 * loud[k]
            want = level
            while want < 4 and env > OPENS[want] + 0.05:
                want += 1
            while want > 0 and env < OPENS[want - 1] - 0.05:
                want -= 1
            if want == 4 and k - last_wide < 20:
                want = 3
            if want != level and held >= HOLD:
                level, held = want, 0
                c = float(cent[k : k + HOLD].mean()) if k < len(cent) else 0.5
                shape = LEVELS[level]
                if level in (2, 3) and c < 0.16:
                    shape = "round"
                elif level == 3 and c > 0.78:
                    shape = "smile"
                last_wide = k if level == 4 else last_wide
            elif not shapes:
                shape = "closed"
            else:
                shape = shapes[-1]
            held += 1
            shapes.append(shape)
        visemes[f"{scene}_{n}"] = shapes
(folder / "visemes.json").write_text(json.dumps(visemes))
for scene, lines in scenes.items():
    end = 0.0
    for line, length in zip(lines, lengths[scene]):
        start = max(line["at"], end + 0.2)
        end = start + length
        print(f"{scene:7} {start:5.1f}-{end:5.1f}  {line['who']:5} {line['text']}")
