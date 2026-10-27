# 🍐 FriendlyPear

**An AI voice agent that protects anyone — and especially older adults — from scams, and keeps their family in the loop.**

Peri, a friendly pear that lives in your phone, reads your messages aloud, calls whoever you ask, and when something smells like a scam it checks with the real person or with your family before you lose a cent.

Built for the **Meta Global AI Developer Hackathon** · Indie track · *Build for Your World*.

> All product code was written Oct 27 – Nov 4, 2026. Concept and design documents were drafted before the building period.
> The app is bilingual (English and Spanish) and follows the phone's language. Internal planning docs under `docs/` are in Spanish.

## Why

Scams work because they **isolate** the victim and **rush** them: "Mum, I broke my phone, this is my new number, can you send me money?", "Your bank here: read me the code you just got". Voice assistants can call and read messages but don't judge them; spam filters warn about everything or nothing. FriendlyPear breaks the isolation: before anything risky happens, Peri checks through a second channel — the real family member, at the number already saved.

## What already works

Tested on the Android emulator (API 37) on Oct 27:

| Moment | What happens |
|---|---|
| **"I have a new number"** | A text from an unknown number claims to be the daughter and asks for money. Peri warns, finds the real daughter in the contact book and says *"If you say yes, I'll call Laura"*. One spoken "yes" and the phone calls her saved number. |
| **A code during a call** | During a phone call, an SMS with a one-time code arrives. A hard rule fires at once: *"You got a code while on the phone: nobody genuine will ask for it, so hang up and don't tell anyone."* The code is never read aloud. |

Also working, with unit tests:

- **Layered shield**: on-device rules with log-odds scoring; hard rules (code during a call, look-alike brand domains) that no model can override; a model opinion moves the score by at most ±2.
- **Voice orders**: "call the boy", "put me through to my daughter", "tell Laura I'll be late", "read me my messages", "I've fallen". Aliases set by the family, diminutives and nicknames ("Pablito" → Pablo), relations ("my granddaughter"), one-letter transcription slips.
- **Agent loop**: perceive → evaluate → decide → ask for permission by risk → act → check the result, with a plan B (next saved number, or the number to dial by hand). Calling and texting always need a spoken "yes"; dictating a code needs a guardian and is never done alone.
- **Safe reading**: links, emails, codes and unknown numbers are never read aloud; scams are not read at all, only Peri's warning.
- **Emergency**: "I've fallen" → 10-second countdown → SMS with location and a call to the guardian. It never dials an emergency number.
- **Muse Spark client** with tool calling, a token cap and a response cache.

Peri currently speaks Spanish (the demo family lives in Spain); English speech is in progress. See [`docs/agentes/ROADMAP.md`](docs/agentes/ROADMAP.md) for everything else.

## Meta models

| Model | Job in FriendlyPear | State |
|---|---|---|
| Muse Spark | Investigator agent with read-only tools for doubtful messages; summaries for guardians | Client done and tested against the API |
| Muse Spark Open or Muse Glimmer (open weights) | Private second opinion before anything reaches the cloud | Client done; hosting pending |
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

On the emulator, save a contact as "Laura hija" with any number, then:

```bash
adb emu sms send 699000111 "Mama, se me ha roto el movil, este es mi numero nuevo, me haces un Bizum?"
adb shell am broadcast -a app.friendlypear.debug.HEAR --es text "si"
```

The broadcast stands in for the microphone in debug builds only.

Secrets never go in the repo: the backend reads `backend/.env` (see `.env.example`), the app reads `~/.gradle/gradle.properties`.

## Privacy

Only doubtful messages ever leave the phone. Contacts are read on the phone and never sent anywhere. Peri says what each permission is for before asking, and the app keeps working with any of them denied (asking for them one by one, when first needed, is in progress). Peri never accesses call audio.
