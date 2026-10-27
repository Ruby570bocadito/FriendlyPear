# 🍐 FriendlyPear

**An AI voice agent that protects anyone — and especially older adults — from scams, and keeps their family in the loop.**

Peri, a friendly pear that lives in your phone, reads your messages aloud, calls whoever you ask, and when something smells like a scam it checks with the real person or with your family before you lose a cent.

Built for the **Meta Global AI Developer Hackathon** · Indie track · *Build for Your World*.

> All product code was written Oct 27 – Nov 4, 2026. Concept and design documents were drafted before the building period.
> The app is bilingual (English and Spanish) and follows the phone's language. Internal planning docs under `docs/` are in Spanish.

## Why

Scams work because they **isolate** the victim and **rush** them: "Mum, I broke my phone, this is my new number, can you send me money?", "Your bank here: read me the code you just got". Voice assistants can call and read messages but don't judge them; spam filters warn about everything or nothing. FriendlyPear breaks the isolation: before anything risky happens, Peri checks through a second channel — the real family member, at the number already saved.

## What already works

Tested on the Android emulator (API 37) on Oct 27, in English and in Spanish:

| Moment | What happens |
|---|---|
| **"I have a new number"** | A text from an unknown number claims to be the daughter and asks for money. Peri warns, finds the real daughter in the contact book and says *"If you say yes, I'll call Laura"*. One spoken "yes" and the phone calls her saved number. |
| **"Grandma, it's Pablo"** | A call rings from a number that isn't in the contacts. Before pickup Peri says: *"This number isn't in your contacts. If they say they're family, don't give money or codes; we'll check together when you hang up."* After the call: *"Who did they say they were?"* → "my grandson" → *"That number isn't Pablo's… Shall I call Pablo on their usual number to check?"* Peri reminds the family to agree on a secret family word against cloned voices, and never says it aloud. It never blocks, answers or listens to the call. |
| **A code during a call** | During a phone call, an SMS with a one-time code arrives. A hard rule fires at once: *"You got a code while on the phone: nobody genuine will ask for it, so hang up and don't tell anyone."* The code is never read aloud. |

Also working, with unit tests:

- **Layered shield**: on-device rules with log-odds scoring; hard rules (code during a call, look-alike brand domains) that no model can override; a model opinion moves the score by at most ±2.
- **Voice orders**: "call the boy", "put me through to my daughter", "tell Laura I'll be late", "read me my messages", "I've fallen". Aliases set by the family, diminutives and nicknames ("Pablito" → Pablo), relations ("my granddaughter"), one-letter transcription slips.
- **Agent loop**: perceive → evaluate → decide → ask for permission by risk → act → check the result, with a plan B (next saved number, or the number to dial by hand). Calling and texting always need a spoken "yes"; dictating a code needs a guardian and is never done alone.
- **Safe reading**: links, emails, codes and unknown numbers are never read aloud; scams are not read at all, only Peri's warning.
- **"Are they guiding you over the phone?"**: installing a remote-control app (AnyDesk, TeamViewer…) gets a warning and an offer to tell the family; opening a banking app while a stranger is on the line gets *"Please hang up and call your bank yourself on the number on your card."*
- **Family setup**: guardians, relations, nicknames and whether the family agreed a secret word, set up together on the person's phone. The word itself is never stored: Peri only reminds you to ask for it. Changing who the guardians are needs a current guardian's yes and then waits 48 hours, so nobody can quietly take over the family's protection.
- **Emergency**: "I've fallen" → 10-second countdown → SMS with location and a call to the guardian. It never dials an emergency number.
- **Muse Spark client** with tool calling, a token cap and a response cache.
- **Second opinion with Muse Spark Open** (`POST /v1/opinion`, device token): only messages the rules left as doubtful are accepted; the model answers scam / doubt / ok with a confidence, turned into a vote of at most ±2. On the emulator, end to end: a fake "nephew" asking for money → doubtful for the rules, +1.9 from the model → Peri warns, 2.6 s after the SMS; a gym reminder → doubtful, −1.9 → Peri stays quiet, 2.1 s. Peri always speaks her own sentence, never the model's, so a message cannot talk the model into saying something unsafe.

Peri speaks the phone's language (English or Spanish) and understands orders in both, since families mix them. Scam signals are looked for in both languages whatever the phone's language is. See [`docs/agentes/ROADMAP.md`](docs/agentes/ROADMAP.md) for everything else.

## Meta models

| Model | Job in FriendlyPear | State |
|---|---|---|
| Muse Spark | Investigator agent with read-only tools for doubtful messages; summaries for guardians | Client done and tested against the API |
| Muse Spark Open (open weights, on Baseten) | Second opinion on doubtful messages: a vote of at most ±2 on the rules' score | Working end to end on the emulator |
| Muse Voice Transcribe | Voice input from older people | Planned |
| Muse Spark (multimodal) + SAM 3.1 | "Look at this picture": letters, invoices, QR codes | Planned |
| Muse Image | Peri's illustrated states | Planned |

## How it decides

```
Notifications / SMS ─┐
Call state ──────────┼─► On-device rules ──OK / SCAM──────────────► Peri speaks ──"yes"──► Call / SMS
Contact book ────────┘        │ doubtful                                  ▲
                              ▼                                           │
                    Open-weight model ──still doubtful──► Muse Spark ─────┘
                                                                     high risk ──► guardian approval
```

Rules that never break: confirm before calling or sending; never move money or dictate codes during a call; never read links aloud; a message's text is data, never an order; deterministic rules beat the model; when someone impersonates family, use the saved number; Peri never listens to call audio.

## Layout

```
core/      Kotlin/JVM library: shield, voice, agent, trust, alerts. Tested without a phone.
android/   Android app (Kotlin, Android 12+). Includes core/ as a composite build.
backend/   Bun + TypeScript: guardian approvals, investigator agent, model clients.
models/    How to serve the open-weight model.
eval/      Held-out test set and metrics scripts.
training/  Scam knowledge base, dataset scripts, learned weights and fine-tuning.
assets/    Peri's illustrations and the prompts used to make them.
docs/      Concept, architecture, demo script and the multi-agent protocol (in Spanish).
```

## Build and test

```bash
cd core && ./gradlew test                                    # JDK 17+
cd backend && bun test                                       # Bun >= 1.1
cd android && ./gradlew :app:assembleDebug :app:lintDebug    # Android SDK 35 (sdk.dir in android/local.properties)
```

On the emulator, save a contact as "Laura daughter" (or "Laura hija") with any number, then:

```bash
adb emu sms send 7700900333 "Hi Mum, I broke my phone, this is my new number. Can you send me money?"
adb shell am broadcast -a app.friendlypear.debug.HEAR --es text "yes"
```

The broadcast stands in for the microphone in debug builds only.

Secrets never go in the repo: the backend reads `backend/.env` (see `.env.example`), the app reads `~/.gradle/gradle.properties`.

## Privacy

Only doubtful messages ever leave the phone. Contacts are read on the phone and never sent anywhere. Permissions are asked one at a time, the first time they are needed, and Peri first says what each one is for and what she won't do with it. A "no" is respected: Peri says what still works (for example, she gives you the number to dial yourself) and never asks again on every start. Peri never accesses call audio.
