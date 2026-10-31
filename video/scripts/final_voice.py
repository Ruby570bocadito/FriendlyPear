# The 3-minute videos' voices (video/public/final, "the heist", or another folder such as diary, "Peri's diary"):
# the crook, Peri, Rosa and Ana, line by line (<folder>/lines.json) into vo_<scene>_<n>.wav, trimmed of silence; how
# long each lasts into durations.json, how loud it is frame by frame into mouths.json and which mouth shape it makes
# into visemes.json (for lip sync), which the video and its music read. For real voices, record the same files and run
# with --measure.
#   python video/scripts/final_voice.py [--measure] [--folder=diary]
import asyncio
import json
import subprocess
import sys
from pathlib import Path

import edge_tts

name = next((a.split("=", 1)[1] for a in sys.argv if a.startswith("--folder=")), "final")
folder = Path(__file__).resolve().parent.parent / "public" / name
scenes = json.loads((folder / "lines.json").read_text(encoding="utf-8"))
VOICES = {
    "crook": ("en-US-ChristopherNeural", "+8%", "+6Hz"),
    "peri": ("en-US-AvaMultilingualNeural", "+4%", "+3Hz"),
    "rosa": ("en-GB-SoniaNeural", "+0%", "-5Hz"),
    "ana": ("en-US-EmmaMultilingualNeural", "+4%", "+0Hz"),
}
TRIM = "silenceremove=start_periods=1:start_threshold=-45dB,areverse,silenceremove=start_periods=1:start_threshold=-45dB,areverse,apad=pad_dur=0.06"


async def speak():
    for scene, lines in scenes.items():
        for n, line in enumerate(lines):
            voice, rate, pitch = VOICES[line["who"]]
            raw = folder / f"raw_{scene}_{n}.mp3"
            for attempt in range(4):
                try:
                    await edge_tts.Communicate(line["text"], voice, rate=rate, pitch=pitch).save(str(raw))
                    break
                except edge_tts.exceptions.NoAudioReceived:
                    if attempt == 3:
                        raise
                    await asyncio.sleep(2)
            subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(raw), "-af", TRIM, "-ar", "44100", str(folder / f"vo_{scene}_{n}.wav")], check=True)
            raw.unlink()


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
import wave

import numpy as np

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
