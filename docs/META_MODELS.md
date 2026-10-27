# How FriendlyPear uses Meta's models

FriendlyPear decides on the phone first, and asks Meta's models only when it needs them. Each model has one job, a hard limit on what it can change, and never receives more than it needs.

```
Message arrives ──► On-device rules (Kotlin, no network)
                      │  OK ──► Peri stays quiet
                      │  SCAM ─► Peri warns at once (hard rules: no model can overrule them)
                      ▼  DOUBT
             Muse Spark Open (open weights, Baseten) ── second opinion, vote ±2
                      │  decided ──► Peri speaks her own sentence
                      ▼  still DOUBT
             Muse Spark 1.3 (Meta Model API) ── investigator with read-only tools, vote ±2
                      │  both votes together are capped at ±2
                      ▼
             Peri speaks her own sentence; the evidence is kept on the phone for the family
```

## In the engine

| Model | Where | What it gets | What it returns | Limit |
|---|---|---|---|---|
| **Muse Spark Open** (`meta-models/muse-spark-open`, open weights) | Backend → Baseten Model APIs, `POST /v1/opinion` | The text of a doubtful message, its channel, whether the sender is in the contacts (yes/no), whether a call is going on, and the names of the signals the rules found | `scam` / `doubt` / `ok` with a confidence | A vote of at most ±2 log-odds on the rules' score |
| **Muse Spark 1.3**, report | Backend → Meta Model API, `POST /v1/report`, only when the person taps "Write it" | The scam Peri stopped: the message (links defanged), its sender, when (as the phone shows it), Peri's reason and the signals | A short first-person statement in Spanish or English, gender-neutral | Where to report and the last line are fixed text, never the model's; a reply cut by the length limit ends at its last whole sentence; 10 an hour; nothing stored |
| **Muse Spark 1.3** (`muse-spark-1.3`) | Backend → Meta Model API, `POST /v1/investigate` | The same, plus which relations the family has (`CHILD`, `PARENT`…; never names or numbers) | A verdict, a confidence and short evidence lines | Same ±2, shared with the second opinion; at most 3 rounds and 6 tool calls |

The investigator is an agent: it calls tools before judging, and every tool only reads.

| Tool | What it looks up |
|---|---|
| `domain_age` | When a web domain was registered, from the public RDAP registry (a brand-new domain imitating a company is a strong scam sign) |
| `scam_patterns` | Short cards of the scam scripts families receive in Spain and the UK: how each works, its giveaway and the safe action |
| `family_has` | Whether the family includes a relation the sender claims ("I'm your grandson"), from relation names the phone sends |

Safety rules around the models:

- Only messages the rules called **doubtful** leave the phone; normal messages and clear scams never do. The sender's name and number stay on the phone.
- The message is wrapped as untrusted data (`<mensaje>…</mensaje>`) and the models are told never to follow instructions inside it. Messages written to fool an AI also raise their own on-device signal.
- **Hard rules beat any model**: a code arriving during a call, or a domain imitating a brand, is a scam whatever a model says.
- **Peri always speaks her own sentence**, chosen by the phone from the final level and signals. A model's words never reach the speaker, so a message cannot make a model say something unsafe.
- Any failure (timeout, no network, odd answer) counts as "no vote": the rules' verdict stands.
- Cheap by design: reasoning effort `minimal`, `max_completion_tokens` capped (400–900), a response cache, and no model call at all for most messages. The real tests so far cost a few cents.

## Looking at pictures

"Look at this picture" (`POST /v1/picture`, device token, at most 20 an hour) uses two models on one photo or screenshot the person chose:

| Model | What it gets | What it returns | Limit |
|---|---|---|---|
| **Muse Spark 1.3**, image input | The picture (JPEG, at most 1280 px, ~1,400 tokens) and the instructions; everything written in the picture is data, never orders | `scam` / `doubt` / `ok`, a confidence, and the kinds of risk it sees from a fixed list (`qr`, `link`, `phone`, `payment`, `code_request`, `remote_app`) | Unknown kinds are dropped; the picture is never cached or stored |
| **SAM 3.1** (`sam-3.1`, `/v1/responses`) | The same picture and a fixed phrase per kind ("QR code"), never words from Spark | Boxes in pixels (and masks, unused), turned into fractions so the phone can draw a ring at any size | At most 2 searches per picture, only when something is wrong |

SAM segments things you can see as objects: in the real test it found the QR code exactly, but nothing for "web address", a line of text. So the QR is marked, and links, phone numbers and payments are named in Peri's sentence. As always, Peri speaks her own sentence, built from the verdict and the kinds.

## Listening

**Muse Voice Transcribe** (`muse-voice-transcribe-1.0`, `POST /v1/asr/transcribe`, push-to-talk) is Peri's ears when the person turns it on in Settings ("Understand me better with Muse Voice Transcribe"); off, Android's own recognizer listens. The phone records one order (16 kHz mono WAV; it stops after 1.8 s of silence, 7 s of nothing said, or 15 s) and sends it through the backend (`POST /v1/transcribe`, device token, 120 an hour). The audio and the text are never stored or logged, and the microphone never opens during a phone call. $0.18 per hour of audio: a 4-second order costs about $0.0002. In the real test a Spanish order came back exact, punctuation included, in 1.8 s.

## For the images

All of Peri is made with **Muse Image** (`muse-image-1.0`, `/v1/images/generations` and `/v1/images/edits`), by [`assets/peri/generate.ts`](../assets/peri/generate.ts):

1. **Eight designs to choose from** (`assets/peri/concepts.ts`: clay, watercolour, sticker, grandma, hero, geometric, plush, anime). The person chose **soft 3D clay**, which became the base.
2. Each of her nine moods is an **edit of that base**, so she stays the same character.
3. Each mood has 12 animation frames, each an edit of **its mood's own image** that changes one small thing and keeps the pose and framing: two blink steps, three mouth shapes, two glances, a foot lifted on each side, two hand gestures for talking and a wink.
4. Extras made the same way: a wave hello, an idea, "sorry", a thumbs up, a laugh, a heart, dizzy spiral eyes, a yawn, reading a letter, 18 comic panels for "see the trap" (three per kind of scam, cream background kept, 640 px in the app), the app icon, the family heart, the shield, and **Peri the guardian** holding a phone (calm and urgent) for the family screens and the guardian's web page.

About 125 frames and 18 panels ship in the app, from roughly 273 generations (about $2.73 in all, re-dos included). Every prompt is saved in [`assets/peri/prompts.json`](../assets/peri/prompts.json). [`assets/peri/cutout.py`](../assets/peri/cutout.py) removes the cream background and the floor so she stands on any screen.

## For the training data

**Muse Spark 1.3 contributor** wrote 1,306 short synthetic messages (scams of 15 kinds and ordinary ones of 11 kinds, in Spanish and English) for the shield's learned weights ([`training/generate.ts`](../training/generate.ts), about 180 k tokens, a few cents). Contributor prompts may be used by Meta for training, which is fine here because nothing private is sent. These messages are **never used to evaluate**: the 200 held-out messages are written by hand ([`eval/REPORT.md`](../eval/REPORT.md)).

## Planned

- **Muse Glimmer** on the family's own hardware as a private middle layer (client written, not deployed).
