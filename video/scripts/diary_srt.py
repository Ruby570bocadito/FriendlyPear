# Subtitles for a 3-minute video as an .srt file (for YouTube or Devpost, where they can be switched on, searched and
# translated): every line of <folder>/lines.json at the moment the video says it, named as the subtitle band names
# them (PERI, THE CROOK, ROSA, ANA) and with the same figures ("$7.7 billion"). A long line is split at its sentences,
# each timed by the pauses in its voice file, into cues of at most two lines of 42 characters and 7 seconds.
#   python video/scripts/diary_srt.py [--folder=diary] [--out=video/out/friendlypear-diary-v9.en.srt]
import json
import re
import sys
import wave
from pathlib import Path

import numpy as np

name = next((a.split("=", 1)[1] for a in sys.argv if a.startswith("--folder=")), "diary")
video = Path(__file__).resolve().parent.parent
folder = video / "public" / name
out = Path(next((a.split("=", 1)[1] for a in sys.argv if a.startswith("--out=")), video / "out" / f"friendlypear-{name}.en.srt"))
scenes = json.loads((folder / "scenes.json").read_text())
lines = json.loads((folder / "lines.json").read_text(encoding="utf-8"))
lengths = json.loads((folder / "durations.json").read_text())
NAMES = {"crook": "THE CROOK", "peri": "PERI", "rosa": "ROSA", "ana": "ANA"}
# The figures the subtitle band shows instead of the spoken numbers (FIGURES in video/src/cast.tsx).
FIGURES = [("seven point seven billion dollars", "$7.7 billion"), ("over sixty", "over 60"), ("Ninety-one percent", "91%"), ("nine days", "9 days")]
WIDTH, LONGEST = 42, 7.0
# Words a subtitle line shouldn't end on: they belong with what follows.
LEADING = {"the", "a", "an", "to", "of", "in", "on", "at", "for", "with", "over", "and", "or", "but", "my", "her", "his", "their", "your"}


def sentences(text):
    """A line's sentences: it's cut after a full stop, "?", "!" or "..." followed by a space (so "$7.7" stays whole)."""
    return [part for part in re.split(r"(?<=[.!?])\s+", text.strip()) if part]


def pauses(path):
    """The silences inside a voice line, longest first: (start, end) in seconds."""
    with wave.open(str(path)) as w:
        rate = w.getframerate()
        pcm = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(float)
    hop = rate // 100
    rms = np.sqrt(np.mean(pcm[: len(pcm) // hop * hop].reshape(-1, hop) ** 2, axis=1))
    quiet = rms < rms.max() * 10 ** (-35 / 20)
    loud = np.flatnonzero(~quiet)
    runs, i = [], loud[0] if len(loud) else 0
    while len(loud) and i < loud[-1]:
        if quiet[i]:
            j = i
            while quiet[j]:
                j += 1
            if j - i >= 12:
                runs.append((i / 100, j / 100))
            i = j
        else:
            i += 1
    return sorted(runs, key=lambda r: r[0] - r[1])


def wrap(text, label=0):
    """One or two lines of at most WIDTH characters, broken near the middle, after punctuation when it can be (but not
    right after the speaker's name, [label] characters), and never after a one-letter word ("I") or a word that
    leads into the next ("the", "to", "over")."""
    if len(text) <= WIDTH:
        return text

    def cost(i):
        left, right = text[:i], text[i + 1 :]
        last = left.split()[-1]
        mark = 0 if i <= label else 9 if last[-1] in ".!?" else 5 if last[-1] in ",:;" else 0
        return (max(len(left), len(right)) > WIDTH, abs(len(text) / 2 - i) - mark + 8 * (len(last) == 1) + 5 * (last.lower() in LEADING))

    best = min((i for i, c in enumerate(text) if c == " "), key=cost)
    return text[:best] + "\n" + text[best + 1 :]


def stamp(t):
    ms = round(t * 1000)
    return f"{ms // 3600000:02d}:{ms // 60000 % 60:02d}:{ms // 1000 % 60:02d},{ms % 1000:03d}"


cues = []
for scene, said in lines.items():
    end = 0.0
    for n, (line, length) in enumerate(zip(said, lengths[scene])):
        start = max(line["at"], end + 0.2)
        end = start + length
        text = line["text"]
        for a, b in FIGURES:
            text = text.replace(a, b)
        parts = sentences(text)
        # Where each sentence starts and ends: at the pauses in the voice when it has one between each two
        # sentences, otherwise shared out by length.
        gaps = sorted(pauses(folder / f"vo_{scene}_{n}.wav")[: len(parts) - 1])
        if len(gaps) == len(parts) - 1:
            edges = [(0.0 if k == 0 else gaps[k - 1][1], length if k == len(parts) - 1 else gaps[k][0]) for k in range(len(parts))]
        else:
            total, at, edges = sum(len(p) for p in parts), 0.0, []
            for p in parts:
                edges.append((at, at + length * len(p) / total))
                at = edges[-1][1]
        label = f"{NAMES[line['who']]}: "
        group, first = [], True
        for p, (a, b) in zip(parts, edges):
            joined = " ".join(q for q, _ in group + [(p, None)])
            if group and (len(label * first + joined) > 2 * WIDTH or b - group[0][1][0] > LONGEST):
                cues.append([scenes[scene][0] + start + group[0][1][0], scenes[scene][0] + start + group[-1][1][1], wrap(label * first + " ".join(q for q, _ in group), len(label) * first)])
                group, first = [], False
            group.append((p, (a, b)))
        cues.append([scenes[scene][0] + start + group[0][1][0], scenes[scene][0] + start + group[-1][1][1], wrap(label * first + " ".join(q for q, _ in group), len(label) * first)])

# Each cue stays up a moment after the voice stops, as the band does, but never over the next one.
cues.sort()
for cue, nxt in zip(cues, cues[1:] + [[10**9]]):
    cue[1] = min(cue[1] + 0.15, nxt[0] - 0.05)
out.write_text("\n".join(f"{i}\n{stamp(a)} --> {stamp(b)}\n{t}\n" for i, (a, b, t) in enumerate(cues, 1)), encoding="utf-8")
print(len(cues), "cues ->", out)
