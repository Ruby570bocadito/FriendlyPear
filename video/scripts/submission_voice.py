# The submission video's draft voice: Peri tells it, phrase by phrase (video/public/submission/lines.json), each
# spoken by a neural voice into vo_<scene>_<n>.mp3; how long each lasts goes to durations.json, which the video reads.
# For the final voice, record vo_<scene>_<n>.mp3 with the same words and run this with --measure.
#   python video/scripts/submission_voice.py [--measure]
import asyncio
import json
import subprocess
import sys
from pathlib import Path

import edge_tts

VOICE = "en-US-AvaMultilingualNeural"
folder = Path(__file__).resolve().parent.parent / "public" / "submission"
scenes = json.loads((folder / "lines.json").read_text(encoding="utf-8"))


async def speak():
    for scene, phrases in scenes.items():
        for n, phrase in enumerate(phrases):
            # The service sometimes answers with no audio; a second try usually works.
            for attempt in range(4):
                try:
                    await edge_tts.Communicate(phrase["text"], VOICE, rate="-3%").save(str(folder / f"vo_{scene}_{n}.mp3"))
                    break
                except edge_tts.exceptions.NoAudioReceived:
                    if attempt == 3:
                        raise
                    await asyncio.sleep(2)


if "--measure" not in sys.argv:
    asyncio.run(speak())
lengths = {}
for scene, phrases in scenes.items():
    lengths[scene] = []
    for n in range(len(phrases)):
        probe = ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(folder / f"vo_{scene}_{n}.mp3")]
        lengths[scene].append(round(float(subprocess.check_output(probe, text=True)), 2))
(folder / "durations.json").write_text(json.dumps(lengths, indent=2))

# When each phrase would end, if it starts at its time or just after the one before.
for scene, phrases in scenes.items():
    end = 0.0
    for phrase, length in zip(phrases, lengths[scene]):
        start = max(phrase["at"], end + 0.25)
        end = start + length
    print(f"{scene:10} voice ends at {end:5.1f} s  ({sum(lengths[scene]):.1f} s of speech)")
