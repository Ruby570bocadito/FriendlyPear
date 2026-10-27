# Peri

A friendly round green pear with a leaf on top. She never scolds and celebrates when you ask before acting (`docs/PERI_VOZ.md`).

All illustrations are made with **Muse Image** (`muse-image-1.0`) by [`generate.ts`](generate.ts): one base Peri from the description in `docs/DISENO.md`, then each state as an **edit of that base**, so she stays the same character. Every animation frame is an edit of its state's own image that changes one small thing and keeps the pose and framing. The exact prompt of every image is in [`prompts.json`](prompts.json). Cost: $0.01 per image; the whole set (70 images, with one redo) was $0.71.

| State | When | Image |
|---|---|---|
| calm | Nothing going on | `peri_calm.webp` |
| listening | Microphone open | `peri_listening.webp` |
| thinking | Waiting for the second opinion and the investigator | `peri_thinking.webp` |
| worried | Doubtful message, unknown caller, emergency countdown | `peri_worried.webp` |
| protective | Scam, code during a call, remote-control app, bank app during a stranger's call | `peri_protective.webp` |
| calling | Calling family | `peri_calling.webp` |
| waiting | Waiting for a guardian's approval | `peri_waiting.webp` |
| happy | Approved, message sent, "I'm fine" | `peri_happy.webp` |
| sleeping | Night, nothing going on | `peri_sleeping.webp` |

Frames of each state: `_blinkhalf` and `_blink` (eyes half and fully closed), `_talk` and `_talkwide` (two mouth shapes), and `_swayleft` and `_swayright` (kept here; the app tilts her smoothly instead). Other illustrations: `calm_wave1` and `calm_wave2` (waving hello), `icon` (the app icon), and `family`, `report` and `shield` (the family screen, the investigator's findings and the setup card).

[`cutout.py`](cutout.py) cuts the cream background out (whatever touches the border, plus cream gaps larger than an eye), feathers and unmixes the edges, and writes 1024 px copies with transparency to `android/app/src/main/res/drawable-nodpi/`. So Peri stands on the app's gradient and, in Mini Peri mode, on top of any app.

In the app, `PeriFace` never blends two poses: she tilts and bobs as one image over her own soft shadow, and each mood moves its own way (a bouncy happy Peri, a steady protective one, slow breathing asleep). Only quick changes swap frames: a four-step blink every few seconds, the mouth while she speaks, and a wave when the app opens. Moods cross-fade with a small pop. With "remove animations" on she only changes mood, without moving.

```bash
cd backend; bun ../assets/peri/generate.ts
```

Regenerate one state from the saved base with `bun ../assets/peri/generate.ts calm`, its frames with `bun ../assets/peri/generate.ts frames calm` (or only some, with `--frame=talk,talkwide`), and the other illustrations with `bun ../assets/peri/generate.ts extras`. Then run `python assets/peri/cutout.py` (needs Pillow and numpy).
