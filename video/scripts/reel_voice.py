# The interview reel's voices: each line said by its character with a neural voice into video/public/reel/vo_<n>.wav,
# and the reel's timeline (when each line and shot starts and ends) into timeline.json, which the reel and its music
# read. To change a line, edit LINES and run this again.
#   python video/scripts/reel_voice.py [--measure]
import asyncio
import json
import subprocess
import sys
from pathlib import Path

import edge_tts

folder = Path(__file__).resolve().parent.parent / "public" / "reel"

VOICES = {
    "rosa": ("en-GB-SoniaNeural", "+2%", "-5Hz"),
    "lock": ("en-US-GuyNeural", "+4%", "-14Hz"),
    "cam": ("en-GB-RyanNeural", "+4%", "-2Hz"),
    "peri": ("en-US-AvaMultilingualNeural", "+6%", "+3Hz"),
}

# Each line: who says it, the shot it's on, what they say, and the pause after it (for a buzzer, a stamp, a beat).
LINES = [
    ("rosa", "rosa_desk", "Who wants to guard my phone?", 0.3),
    ("lock", "padlock", "I lock everything. Nobody gets in. Not even your grandson.", 0.2),
    ("rosa", "rosa_meh", "Not even Pablo? Next!", 0.6),
    ("cam", "camera", "I'll watch every message, and report it all to your family.", 0.2),
    ("rosa", "rosa_meh", "A spy? Next!", 0.6),
    ("peri", "peri_tie", "Hi! I'm Peri. I stop scams before any money moves.", 0.25),
    ("peri", "peri_tie", "Risky? I ask your family first. They never see your messages.", 0.3),
    ("rosa", "rosa_desk", "Weaknesses?", 0.25),
    ("peri", "peri_tie", "I'm a pear. I bruise easily.", 0.35),
    ("rosa", "rosa_laugh", "Ha! You're hired!", 0.9),
    ("peri", "peri_stop", "Hang up now. That's a scam. Call your bank yourself.", 0.4),
    ("rosa", "rosa_phone", "Best hire ever.", 0.35),
    ("peri", "end", "FriendlyPear. Your phone's new bodyguard.", 1.4),
]
SCAM_BEFORE = 10  # the scam message arrives, with no words, before this line
SCAM_LENGTH = 2.2


# Silence trimmed from both ends, so the lines follow each other like a sketch.
TRIM = "silenceremove=start_periods=1:start_threshold=-45dB,areverse,silenceremove=start_periods=1:start_threshold=-45dB,areverse,apad=pad_dur=0.08"


async def speak():
    for n, (who, _, text, _) in enumerate(LINES):
        voice, rate, pitch = VOICES[who]
        raw = folder / f"raw_{n}.mp3"
        for attempt in range(4):
            try:
                await edge_tts.Communicate(text, voice, rate=rate, pitch=pitch).save(str(raw))
                break
            except edge_tts.exceptions.NoAudioReceived:
                if attempt == 3:
                    raise
                await asyncio.sleep(2)
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(raw), "-af", TRIM, "-ar", "44100", str(folder / f"vo_{n}.wav")], check=True)
        raw.unlink()


if "--measure" not in sys.argv:
    asyncio.run(speak())

t = 0.35
lines = []
for n, (who, shot, text, pause) in enumerate(LINES):
    if n == SCAM_BEFORE:
        lines.append({"who": "scam", "shot": "scam", "text": "", "start": round(t, 3), "end": round(t + SCAM_LENGTH, 3)})
        t += SCAM_LENGTH
    probe = ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(folder / f"vo_{n}.wav")]
    length = float(subprocess.check_output(probe, text=True))
    lines.append({"n": n, "who": who, "shot": shot, "text": text, "start": round(t, 3), "end": round(t + length, 3)})
    t += length + pause
(folder / "timeline.json").write_text(json.dumps({"length": round(t, 3), "lines": lines}, indent=2))
for line in lines:
    print(f"{line['start']:6.2f}-{line['end']:6.2f}  {line['who']:5} {line['shot']:10} {line['text']}")
print("total", round(t, 2))
