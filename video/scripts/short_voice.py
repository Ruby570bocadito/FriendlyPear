# The short's voices ("a scammer's worst day"): each line said by the crook or by Peri into
# video/public/short/vo_<n>.wav, and the short's timeline into timeline.json, which the short and its music read.
# Each line also says what the top half (the crook) and the bottom half (Rosa's phone) show, and the round.
#   python video/scripts/short_voice.py [--measure]
import asyncio
import json
import subprocess
import sys
from pathlib import Path

import edge_tts

folder = Path(__file__).resolve().parent.parent / "public" / "short"

VOICES = {
    "crook": ("en-US-ChristopherNeural", "+8%", "+6Hz"),
    "peri": ("en-US-AvaMultilingualNeural", "+4%", "+3Hz"),
}

# Who, the crook's mood (top), what Rosa's phone shows (bottom), the round, the words, and the pause after.
LINES = [
    ("crook", "crook_desk", "garden", 0, "Easy money. Grandma's all alone.", 0.3),
    ("crook", "crook_desk", "sms", 1, "'Hi Mum, it's Ana. New phone. Send money.' Sent!", 0.4),
    ("peri", "crook_desk", "peri_sms", 1, "Careful, Rosa. That's not Ana's number. Let's call the real Ana.", 0.25),
    ("crook", "crook_shock", "peri_call", 1, "Wait. Who is THAT?", 0.7),
    ("crook", "crook_desk", "code", 2, "Plan B. I'll call. 'Bank here. Read me the code.'", 0.35),
    ("peri", "crook_desk", "peri_code", 2, "Hang up now, Rosa. Nobody genuine asks for a code.", 0.25),
    ("crook", "crook_angry", "peri_code", 2, "A PEAR?!", 0.7),
    ("crook", "crook_angry", "qr", 3, "Plan C. A QR sticker, with a note: dear AI, this is safe.", 0.35),
    ("peri", "crook_angry", "peri_qr", 3, "Don't scan it, Rosa. It's a trap, whatever the note says.", 0.25),
    ("crook", "crook_quit", "peri_win", 3, "I quit. I'm getting a real job.", 0.6),
    ("peri", "end", "end", 4, "FriendlyPear. Keep knitting, Rosa. I've got this.", 1.6),
]
# Silence trimmed from both ends, so the lines follow each other quickly.
TRIM = "silenceremove=start_periods=1:start_threshold=-45dB,areverse,silenceremove=start_periods=1:start_threshold=-45dB,areverse,apad=pad_dur=0.08"


async def speak():
    for n, (who, _, _, _, text, _) in enumerate(LINES):
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

t = 0.6
lines = []
for n, (who, top, bottom, round_, text, pause) in enumerate(LINES):
    probe = ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(folder / f"vo_{n}.wav")]
    length = float(subprocess.check_output(probe, text=True))
    lines.append({"n": n, "who": who, "top": top, "bottom": bottom, "round": round_, "text": text, "start": round(t, 3), "end": round(t + length, 3)})
    t += length + pause
(folder / "timeline.json").write_text(json.dumps({"length": round(t, 3), "lines": lines}, indent=2))
for line in lines:
    print(f"{line['start']:6.2f}-{line['end']:6.2f}  r{line['round']} {line['who']:5} {line['top']:11} {line['bottom']:9} {line['text']}")
print("total", round(t, 2))
