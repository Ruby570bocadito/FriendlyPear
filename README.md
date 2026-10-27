# 🍐 FriendlyPear

**An AI voice agent that protects anyone — and especially older adults — from scams, and keeps their family in the loop.**

Peri, a friendly pear that lives in your phone, reads your messages aloud, calls whoever you ask, and when something smells like a scam it checks with the real person or with your family before you lose a cent.

Built for the **Meta Global AI Developer Hackathon** · Indie track · *Build for Your World*.

> All product code was written Oct 27 – Nov 4, 2026. Concept and design documents were drafted before the building period.
> The app is bilingual (English and Spanish): it follows the phone's language, or the one chosen in Settings. Internal planning docs under `docs/` are in Spanish.

## A companion, not a watchdog

Nobody wants an app "to control your phone". Peri is presented, truthfully, as a friend who reads your messages aloud, calls whoever you ask and keeps you company, and who also spots scams. She works for the person, not for the family: the family never reads messages, calls, location or apps, and only gets a one-time link to approve something risky when the person asks for it. Every permission is explained first and can be refused, and the app never says "control", "monitor" or "watch". See [`docs/CONFIANZA.md`](docs/CONFIANZA.md) (in Spanish), including how to suggest it to a relative.

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

- **Layered shield**: on-device rules with log-odds scoring; hard rules (code during a call, look-alike brand domains) that no model can override; a model opinion moves the score by at most ±2. Any link without TLS (`http://`), on a port other than 443 or pointing to a bare IP address always gets a warning, even from family: *"its link has no padlock, so it isn't safe"*.
- **Voice orders**: "call the boy", "put me through to my daughter", "ask mum for help", "call my daughter, I need help", "tell Laura I'll be late", "read me my messages", "I've fallen". Aliases set by the family, diminutives and nicknames ("Pablito" → Pablo), relations ("my granddaughter"), one-letter transcription slips.
- **Agent loop**: perceive → evaluate → decide → ask for permission by risk → act → check the result, with a plan B (next saved number, or the number to dial by hand). Calling and texting always need a spoken "yes"; dictating a code needs a guardian and is never done alone.
- **Safe reading**: links, emails, codes and unknown numbers are never read aloud; scams are not read at all, only Peri's warning.
- **"Look at this picture"**: show Peri a letter, an invoice, a QR code or a screenshot, from her button (Android's photo picker, no permission) or by sharing it with Peri from any app. Muse Spark reads it; SAM 3.1 marks the QR code with a red ring; Peri says in her own words what not to do ("Don't scan its QR code. Don't open its web address. Don't pay anything it asks for."). A scam picture also arms the safety pause for money apps. Tested on the real API with a fake parcel notice: scam (97%), QR marked exactly, 5.8 s, about 1,800 tokens. The picture is never stored.
- **Safety pause**: a money app (bank, PayPal, Revolut…) or a remote-control app opened during a stranger's call, or within 30 minutes of a scam, a doubtful message or a stranger's call, gets a full-screen pause over it: Peri says why and offers to call the family to check (button or a spoken "yes"), "Leave this app", or "Continue anyway", which unlocks after 5 seconds. A speed bump, not a wall: the person always decides.
- **"Are they guiding you over the phone?"**: installing a remote-control app (AnyDesk, TeamViewer…) gets a warning and an offer to tell the family; opening a banking app while a stranger is on the line gets *"Please hang up and call your bank yourself on the number on your card."*
- **Family setup**: guardians, relations, nicknames and whether the family agreed a secret word, set up together on the person's phone. The word itself is never stored: Peri only reminds you to ask for it. Changing who the guardians are needs a current guardian's yes and then waits 48 hours, so nobody can quietly take over the family's protection.
- **Two modes: the app or Mini Peri**: the full screen, or only the pear (cut out, with her own shadow) floating over other apps. She pops up with a little bounce; tap her to talk (a soft green ring pulses while she listens), drag her and she glides to the nearest side, drop her on the "×" at the bottom to put her away, long-press to open the app. What she says appears in a speech bubble pointing at her, with smaller letters for long sentences so it never covers the screen. She also comes out by herself beside the call screen during a call she placed or a picked-up call from a stranger, and leaves when it ends.
- **A friend with personality**: Peri greets you by the time of day (said aloud only the first time each day), and when you tap her she has an idea and shares a little anti-scam tip; she says sorry when she didn't catch you, and gives a thumbs up when you leave a risky app. Permission explanations are written in her bubble rather than read aloud, so the first start isn't a lecture. The layout keeps Peri on screen with the phone's large text on, and her voice uses Google's speech engine when the phone has it. In **Settings → Peri's voice** you can hear every voice the phone has for her language and pick the nicest; a button leads to Android's speech settings (or Google's engine in the Play Store) for more natural voices.
- **An animated, friendly look**: Peri tilts and bobs smoothly, blinks, glances around, moves her mouth while she speaks and waves hello; each mood moves its own way (bouncy when happy, steady when protecting you, breathing slowly asleep), and she keeps still with "remove animations" on. Big rounded buttons, a status chip ("Protecting you" / "Setup not finished") and a card with any setup step still missing; every text has at least 7:1 contrast.
- **Emergency**: "I've fallen" → 10-second countdown → SMS with location and a call to the guardian. It never dials an emergency number.
- **Muse Spark client** with tool calling, a token cap and a response cache.
- **Second opinion with Muse Spark Open** (`POST /v1/opinion`, device token): only messages the rules left as doubtful are accepted; the model answers scam / doubt / ok with a confidence, turned into a vote of at most ±2. On the emulator, end to end: a fake "nephew" asking for money → doubtful for the rules, +1.9 from the model → Peri warns, 2.6 s after the SMS; a gym reminder → doubtful, −1.9 → Peri stays quiet, 2.1 s. Peri always speaks her own sentence, never the model's, so a message cannot talk the model into saying something unsafe.
- **Investigator with Muse Spark** (`POST /v1/investigate`, device token, doubtful messages only): an agent that looks things up before judging, with read-only tools: how old a web domain is (public RDAP registry), cards of known scam scripts, and whether the family has a relation the sender claims (relations only, never names or numbers). At most 3 rounds and 6 lookups. The phone calls it only when a message is still doubtful after the second opinion; both votes together stay within ±2, and the evidence (never the message) is kept on the phone for the family to see in the family setup. Tested on the real API: a fake Correos parcel fee → it checked the domain and the scam cards → scam, +1.9; a pharmacy pickup notice → ok, −1.3.

Peri speaks the phone's language (English or Spanish), or the one chosen in **Settings → Language**, which switches the screens and her voice at once without restarting; she understands orders in both, since families mix them. Scam signals are looked for in both languages whatever the phone's language is. See [`docs/agentes/ROADMAP.md`](docs/agentes/ROADMAP.md) for everything else.

## For the family

There is no app for the family to install, and nothing to watch. A guardian (for example the grandchild) is a saved contact the person marks as guardian. When the person asks for something risky (dictating a code, or changing who the guardians are), the guardian gets an SMS with a **one-time link valid for 15 minutes** to a simple page: "No" first, then "Yes, I approve". Silence counts as no. Tested end to end with two emulators: the grandmother's phone and the guardian's phone (Ana), where tapping "Yes, I approve" let Peri go on.

If the person turns on **"Let my guardians see Peri's news"** in the family setup, each guardian gets a personal link (by SMS) to **Peri's news**: a page with the last 7 days at a glance and a timeline of what Peri did ("warned about a WhatsApp message that looked like a scam", "paused them before a money app, and they chose to call family") and the approvals asked. Only the kind of event travels (level and channel, or the pause's answer); never text, numbers, names, calls, location or apps. The person sees exactly the same list on their own phone, and turning sharing off makes every link stop working at once. Links (only their hash) and the news are saved in `backend/data/`, so they survive a restart. A weekly summary sent to guardians is still on the roadmap (`avisos-familia`).

## iPhone

Not built yet: iOS needs a Mac to build, and it doesn't let apps read other apps' notifications or draw over them. [`docs/IPHONE.md`](docs/IPHONE.md) (in Spanish) maps each feature to its iOS equivalent: the shared Kotlin core via Kotlin Multiplatform, an SMS filter extension, the safety pause with the Screen Time API, Peri in a Live Activity, and "Share it with Peri".

## Meta models

How each model is used in the engine and for the images, with its limits: [`docs/META_MODELS.md`](docs/META_MODELS.md).


| Model | Job in FriendlyPear | State |
|---|---|---|
| Muse Spark | Investigator agent with read-only tools for doubtful messages; summaries for guardians | Called from the phone when the second opinion leaves a message doubtful |
| Muse Spark Open (open weights, on Baseten) | Second opinion on doubtful messages: a vote of at most ±2 on the rules' score | Working end to end on the emulator |
| Muse Voice Transcribe | Peri's ears, if the person turns it on in Settings: each voice order is recorded on the phone (it stops after a long pause, for people who speak slowly) and written down by Meta's speech model | Working on the emulator and the real API ("Oye Peri, llama a mi hija, por favor." exact, 1.8 s); off by default |
| Muse Spark (multimodal) + SAM 3.1 | "Look at this picture": Spark reads a letter, invoice or screenshot and names the risky things; SAM 3.1 finds the QR code so the phone can mark it | Working on the emulator and the real API |
| Muse Image | Peri's nine illustrated states (calm, listening, thinking, worried, protective, calling, waiting, happy, sleeping), each an edit of one base so she stays the same character, plus animation frames made as edits of each state (two blink steps, three mouth shapes, glances to each side), a wave hello, the app icon and illustrations for the family screen; cut out for any background (97 images, $0.98) | In the app, animated; prompts in `assets/peri/prompts.json` |

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

### Run it yourself

1. **SDK path**: in `android/local.properties`, `sdk.dir=C:/Users/you/AppData/Local/Android/Sdk` (forward slashes work on Windows too).
2. **Backend** (optional: without it Peri still protects with the on-phone rules alone):
   ```bash
   cd backend && cp .env.example .env    # set DEVICE_TOKEN to 32+ random characters, plus the API keys you have
   bun src/main.ts                       # prints which layers are on: second opinion, investigator, pictures
   ```
   For the emulator, set `PUBLIC_BASE_URL=http://10.0.2.2:8787` so guardian links open from the phone.
3. **Point the app at it**, in `~/.gradle/gradle.properties` (outside the repo):
   ```properties
   friendlypear.backendUrl=http://10.0.2.2:8787
   friendlypear.deviceToken=<the same DEVICE_TOKEN>
   ```
4. **Install and grant** (debug build): `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`, open FriendlyPear and accept what Peri asks for. Notification access, usage access and "appear on top" are granted from the setup card (or with `adb shell cmd notification allow_listener app.friendlypear/app.friendlypear.services.MessageListener`, `adb shell appops set app.friendlypear GET_USAGE_STATS allow` and `adb shell appops set app.friendlypear SYSTEM_ALERT_WINDOW allow`).

Secrets never go in the repo: the backend reads `backend/.env` (see `.env.example`), the app reads `~/.gradle/gradle.properties`.

## Privacy

Only doubtful messages ever leave the phone, plus what the person chooses to send: a picture they show Peri, and their voice orders if they turn on Muse Voice Transcribe in Settings (off by default; Android's own recognizer otherwise). None of it is stored. Contacts are read on the phone and never sent anywhere. Permissions are asked one at a time, the first time they are needed, and Peri first says what each one is for and what she won't do with it. Reading texts is optional: Android locks it for apps installed from a file, so Peri also protects from notifications alone, and the step can be hidden. A "no" is respected: Peri says what still works (for example, she gives you the number to dial yourself) and never asks again on every start. Peri never accesses call audio, and the microphone stays closed during any phone call, even on speaker.

Security of the backend, whose guardian links are public URLs: requests are limited per IP (30 a minute on `/a/` and `/g/`, 240 on the phone's API), an IP that keeps sending wrong device tokens is shut out for 15 minutes, bodies are capped and read only up to the limit, and every answer carries security headers (no sniffing, no referrer, no embedding, CSP on pages, HSTS over HTTPS). Link tokens are stored only as hashes. Release builds speak HTTPS only; plain HTTP to the developer's machine exists in debug builds alone, and the debug voice bridge only answers `adb`.

Icons in the app's buttons: [Material Icons](https://fonts.google.com/icons) (Apache License 2.0). Everything else, including Peri, was made during the event.
