# Everything FriendlyPear does

A complete catalogue of what is built, how each piece decides, which Meta model it uses, how it was tested, and how
to try it yourself on the Android emulator. The short version is in the [README](../README.md); the numbers are in
[`eval/REPORT.md`](../eval/REPORT.md).

**Status key:** ✅ tested end to end on the emulator (Android 17, API 37) · 📱 also on a real phone (OPPO Reno14 F,
ColorOS) · 🧪 unit tests in `core/` or `backend/`.

## 1. Protection

| Feature | What it does | How it decides | Status |
|---|---|---|---|
| **Message shield** | Every SMS, WhatsApp and email notification is checked the moment it arrives | On-phone rules find signals (unknown sender, lookalike domain, urgency, money, "tell us the code", new-number claim, prize, secrecy, scareware, romance bait, remote-control app, hidden AI instructions) and add log-odds weights. ≥ 0 is doubt, ≥ 2 is scam. Measured: 88 % of 200 unseen scams flagged, 4 % false alarms (`eval/REPORT.md`) | ✅ 📱 🧪 |
| **Hard rules** | Two cases are a scam whatever any model says | A one-time code arriving during a call; a known brand on a lookalike domain (`correos-envio.top`) | ✅ 🧪 |
| **Insecure links** | A link without a padlock (`http://`, or a port other than 443) is always mentioned, even from family | Its own signal, weighted so it reaches "doubt" alone | ✅ 🧪 |
| **Second opinion** | Doubtful messages only, never the clear ones | **Muse Spark Open** (Baseten) votes scam / doubt / ok; the vote moves the score by at most ±2, so models tip the balance and rules decide | ✅ 🧪 |
| **Investigator** | If still doubtful, an agent looks things up before Peri speaks | **Muse Spark 1.3** with three read-only tools: domain age (RDAP), scam patterns (15 cards, es/en: family, parcel, bank, tax, tech support, prizes, utilities, subscriptions, romance, job tasks, second-hand buyers, fake police, loan fees) and "does the family include a…" (relations only, never names). At most 3 rounds and 6 lookups | ✅ 🧪 |
| **Peri's own sentence** | Peri never reads a model's words aloud | She speaks a sentence built from the signals, with no links or codes in it, so a message can't talk the model into saying something unsafe | 🧪 |
| **"I have a new number"** | A text from an unknown number claiming to be a child or grandchild | Peri finds the real relative in the contacts and offers to call them: *"If you say yes, I'll call Ana"* | ✅ 🧪 |
| **Calls from strangers** | Before pickup: a reminder not to give money or codes; after hanging up: *"Who did they say they were?"* → offers to call that person's real number | Android's call screening (never blocks, answers or listens) and call state | ✅ 🧪 |
| **Code during a call** | *"Please hang up right now: you got a code while you're on the phone…"* | Hard rule; the code is never read aloud | ✅ 🧪 |
| **Safety pause** | Opening a money app (bank, PayPal, Revolut…) or a remote-control app during a stranger's call, or within 30 minutes of a scam, gets a full-screen pause: call family, leave the app, or carry on | App watcher only while there is a risk; the person always decides | ✅ 🧪 |
| **"Are they guiding you?"** | Installing AnyDesk, TeamViewer and 13 other remote-control apps gets a warning and an offer to tell the family | Package list visible through `<queries>` only | ✅ 🧪 |
| **"Look at this picture"** | Show Peri a letter, an invoice, a QR code or a screenshot | **Muse Spark** (multimodal) reads it and names the risky things; **SAM 3.1** finds the QR code so the phone draws a red ring round it. Never stored | ✅ 🧪 |

## 2. The family, without spying

| Feature | What it does | Status |
|---|---|---|
| **Family approval** | Something risky (dictating a code, changing guardians) sends the guardian an SMS with a one-time link valid for 15 minutes. Three answers: "No" first, "Ask them to call me first" (Peri sends nothing and offers to call the guardian; a spoken "yes" calls), "Yes, I approve". Silence counts as no; only the link's hash is stored | ✅ (two emulators: Mum and "Ana hija") 🧪 |
| **Weekly summary** | Once a week, while sharing is on, each guardian gets an SMS with only the counts their page already shows ("I warned about 3 messages that were scams"); a quiet week says so too, and carrying on after a pause suggests a call | ✅ 🧪 |
| **Peri to Peri** | A guardian who has FriendlyPear gets the request in their own Peri: read aloud, a screen with the guardian art and the three answers, a notification if the app is closed. Trusted only from a saved contact and with a link to Peri's own server; the details come from the server. "Call them now" answers "call me first" and dials them | ✅ (two emulators, Spanish and English) |
| **Fake Peri** | A text that talks like Peri but links elsewhere is a firm scam with its own sentence; no model is asked | ✅ 🧪 |
| **Approving safely** | Web: a second "are you sure?" page with three checks. App: the same checks and the phone's own lock. Opening the family setup also needs the lock once there is a guardian | ✅ 🧪 |
| **Scam in progress** | If the person is on the phone with an unsaved number, or hung up on one in the last 30 minutes, the guardian's SMS starts with "URGENT" and the page says so, without the number, the call or the message | ✅ 🧪 |
| **Circle rules** | The first guardian is set at once; later changes need a current guardian's yes and wait 48 h, and the person can cancel | ✅ 🧪 |
| **Peri's news for guardians** | Optional. Each guardian gets a personal page with the last 7 days: "warned about a message that looked like a scam", "paused them before a money app". Only the kind of event travels, never text, numbers, names, calls, location or apps | ✅ 🧪 |
| **Family setup** | Guardians, relations, nicknames and "we agreed a family word" (the word itself is never stored), with search for long contact lists | ✅ 📱 |
| **Ask for help** | "Call my daughter, I need help" or "ask mum for help" → Peri calls, or texts if there's no answer | ✅ 🧪 |
| **Emergency** | "I've fallen" → 10-second countdown → SMS with location and a call to the guardian. Never dials an emergency number | 🧪 |

## 3. Peri, the companion

| Feature | What it does | Status |
|---|---|---|
| **Voice orders** | "call the boy", "put me through to my daughter", "tell Laura I'll be late", "read me my messages"… with aliases, nicknames and diminutives ("Pablito" → Pablo), in Spanish and English | ✅ 🧪 |
| **Peri's ears** | Android's recognizer by default; optionally **Muse Voice Transcribe**, better with older voices (each order is sent to be written down and never kept) | ✅ |
| **Peri's voice** | Uses Google's speech engine when the phone has it; Settings lists every voice the phone has for her language, plays a sample and remembers the choice, with a button to get more natural voices | ✅ |
| **Language** | English or Spanish, following the phone or chosen in Settings; she understands orders in both | ✅ |
| **Safe reading** | Links, emails, codes and unknown numbers are never read aloud; scams are not read at all, only Peri's warning | 🧪 |
| **Personality** | Greets you by the time of day (aloud only the first time each day); says sorry when she didn't catch you; thumbs up when you leave a risky app | ✅ 📱 |
| **Tapping Peri** | Where you touch her matters: her leaf (she winks and complains about her hair), her tummy (in turn: an idea with a tip, a pear joke she laughs at, a "did you know?" about scams with a thumbs up, a kind word with a heart and floating hearts) or her feet (she dances). Three quick taps: a spin, then spiral eyes and stars. Five: she gives up laughing and dances with sparkles. At night she says hello with a yawn and a drifting "z z z"; asked to read your messages, she holds up a letter. Each list is shuffled and goes round before anything repeats | ✅ |
| **Animation** | 9 moods in soft 3D clay, each with 13 frames: blinks, three mouth shapes, glances, steps, two hand gestures for talking, and a wink. Between frames she sways her weight, bobs, squashes when landing and nods with each word. Every 7–12 s she does something on her own: a hop, steps or a dance, a spin, a wink, a look around, a stretch, a wave. She drops in when the app opens, squishes like clay under your finger, glances towards where you touch, and sparkles when she's happy. Her frames are decoded at the size she's drawn and in the background, and let go while she's hidden (with Mini Peri out, the app went from 176 to 65 MB on the emulator). With "remove animations" on, she stays still | ✅ |
| **Mini Peri** | Only the pear, floating over other apps: tap to talk (a green ring pulses while she listens), drag (she glides to the nearest side), drop her on the "×" at the bottom to put her away, long-press to open the app. She comes out by herself beside the call screen during a stranger's call and leaves when it ends | ✅ 📱 |
| **Bubble** | What she says appears as she says it, with smaller letters for long sentences so it never covers her | ✅ |

## 4. Getting set up

| Feature | What it does | Status |
|---|---|---|
| **Peri's welcome** | Before any permission, Peri introduces herself in four short pages (who she is, she stops scams, the family never spies, you decide), animated, with the language choice at the top. Only the first page is said aloud; each has "Hear it". Seen again from Settings | ✅ |
| **Pairing a phone** | `bun pair` on the server shows a one-time code and QR, in the terminal and on a page with Peri, the steps and a live countdown that greys the QR out when it expires; the phone's camera opens "Connect this phone", which gets the phone its own key (Keystore-encrypted). Or type the code in Settings. QRs for another server are refused | ✅ 🧪 |
| **One permission at a time** | Each permission is asked the first time it's needed, after Peri says what it's for and what she won't do with it. A "no" is respected and never asked again on every start | ✅ 📱 |
| **Setup card** | One missing step at a time, the most important first; optional steps (texts, bank-app notice) can be hidden with "×". "Protecting you" needs only contacts, calls and one way to see messages | ✅ 📱 |
| **Unlock guide** | Installed from a file, Android 13+ locks message alerts and texts as "restricted settings" (spy apps use them). The guide walks through the only order Android accepts: try once → allow restricted settings in app info → turn it on, each step opening the exact screen and ticking itself off. One unlock covers both. The app no longer asks for texts at first start when it's locked, which avoided Android's frightening "denied" notice | ✅ (installed from Downloads like on a phone) |
| **Large text** | With the phone's large text on, the layout keeps Peri on screen: text grows up to 1.15×, the bubble shrinks its letters, buttons shrink their labels instead of cutting them | ✅ 📱 |

## 5. Measured on the emulator with real SMS

Sent with `adb emu sms send` to the app installed from Downloads, after the unlock guide (Oct 27):

| SMS | Peri | Time |
|---|---|---|
| Fake Correos fee, lookalike domain | *"Careful, this is a trap: it says it's from Correos, but it isn't…"* (hard rule) | at once |
| "Mum, new number, Bizum of €300" | *"…it claims to be family from a new number and asks for money… I'll call Ana right away"* | at once |
| The daughter, Sunday lunch | quiet | — |
| Google one-time code, no call | quiet | — |
| "Nephew" without battery asking for 200 EUR | doubtful → Spark Open +1.94 → *"Careful, this is a trap: it asks for money and it rushes you…"* | 3.8 s |
| A contact's wedding photos with `http://` | doubtful → models 0 → *"…its link has no padlock… Shall we check it together?"* | 9 s |
| A code while a stranger is on the phone | *"Please hang up right now…"* (hard rule) | at once |

Two bugs found this way and fixed: amounts written "200 EUR" or "me prestas" weren't seen as money, and the
link sentence said "it has a link" twice.

## 6. How to try it on the emulator

```
cd backend; bun src/main.ts                       # second opinion, investigator, pictures, approvals
cd android; ./gradlew :app:installDebug
adb shell appops set app.friendlypear SYSTEM_ALERT_WINDOW allow          # Mini Peri
adb shell cmd notification allow_listener app.friendlypear/app.friendlypear.services.MessageListener
adb emu sms send 611222333 "Correos: pague 1,99 EUR en correos-envio.top"
adb emu gsm call 911000111; adb emu gsm accept 911000111; adb emu sms send 22395 "Su codigo es 482913"
adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"   # a voice order, debug builds only
```

Installing with `adb` skips Android's restricted settings; to see the unlock guide, copy the APK to Downloads and
open it from the Files app.

## 7. Meta models

| Model | Where | When it's called |
|---|---|---|
| Muse Spark Open (Baseten) | Second opinion | Only doubtful messages |
| Muse Spark 1.3 | Investigator, "look at this picture" | Still doubtful after the second opinion; pictures the person shows |
| SAM 3.1 | Marks QR codes in pictures | With "look at this picture" |
| Muse Voice Transcribe | Peri's ears | Only if turned on in Settings |
| Muse Image | Every illustration of Peri (122 in the app; about 240 generated in all, ~$2.40) and the 8 designs she was chosen from | Offline, when making the app |
| Muse Spark 1.3 contributor | 1,306 synthetic training messages for the learned weights | Offline, never for evaluation |

Security and privacy in detail, with what is still open: [`SECURITY.md`](SECURITY.md).

## 8. Known limits

- The debug APK and the backend run on the developer's machine; a public HTTPS address (a tunnel) is needed for a
  phone outside the home network.
- Android shows "Play Protect" and restricted-settings screens for any app installed from a file. A store release
  avoids both.
- Romance scams with no link, and some fake-subscription emails, slip past the rules; see `eval/REPORT.md`.
- No iPhone yet (needs a Mac); the plan is in [`IPHONE.md`](IPHONE.md).
