# Security and privacy

FriendlyPear stands between an older person and people actively trying to rob them, so the app itself must be hard
to turn against them. This page lists each threat, what stops it, and what is still open. Everything marked ✅ is
in the code and covered by tests or by the two-phone tests of Oct 27 and 28.

## Who we defend against

1. **A scammer on the phone** guiding the person step by step ("read me the code", "open your bank app").
2. **A scammer impersonating Peri or the family**: a fake "Peri needs your OK" text, a fake "Mum, new number".
3. **Someone holding the person's or the guardian's phone**: a visitor, a thief, a "technician".
4. **Someone who gets a family link**: a forwarded SMS, a messaging app's link preview.
5. **Text written to fool the AI** hidden inside a message.
6. **Abuse of the server**: floods, stolen device token, oversized uploads.

## What stops each one

| # | Threat | Mitigation | |
|---|---|---|---|
| 1 | Code dictated during a scam call | Hard rule: a code arriving during a call is a scam, whatever any model says; Peri says "hang up" | ✅ |
| 1 | "Tell me the code" after the call | Sending a code is high risk: it waits for a guardian's yes. The guardian's SMS says **URGENT** if a stranger called in the last 30 minutes | ✅ |
| 1 | "Open your bank app now" | Safety pause before money and remote-control apps during or after a stranger's call | ✅ |
| 2 | Fake Peri request | The guardian's own Peri only accepts links to **Peri's own server** (same scheme, host and port), from a **saved contact**, and reads the request from the server, never from the SMS. Anything that talks like Peri but links elsewhere is a firm scam: *"this message pretends to be from me"* | ✅ |
| 2 | Fake family member | "New number", secrecy ("don't tell dad"), romance and scareware signals; Peri offers to call the real relative at the saved number | ✅ |
| 2 | "Mum, it's me, new number" | **"Was it you?"**: Peri quietly asks the family's other phones whether any of them wrote from a new number. Each answers once with a tap, with its own key, and Peri says what they answered, or that nobody did. An answer never turns a warning into an OK: even "yes, it was me" ends with "call them on their usual number first". 15 minutes, at most 3 open questions per phone, only while Peri's news is shared | ✅ |
| 2 | The same trap reaching the family | When two or more family pears turn to attention with the same kind of trap within 30 minutes, every Peri warns that it is going around | ✅ |
| 1 | A scammer on the line, right now | A big *Hang up* button whenever a number that isn't in the contacts is on the line. The family sees "on a call with a number they don't know" (only that, only while the person shares Peri's news) and can ask that phone's Peri to hang up: one request at a time, two minutes at most, with each phone's own key; it never ends a call with a contact, and never behind the person's back: Peri says who asks and waits three seconds with *Keep talking* on screen | ✅ |
| 1 | The same scammer calling again | The person can block a scam's number from its card: its calls are turned away before they ring, its texts never read aloud. Kept on the phone, never backed up, undone in Settings | ✅ |
| 2 | A fake approval page | The guardian sees **the asking family member's pear, dressed as the family knows it** (taken from that phone when it asked), on the web page and in the app: a fake page can't copy it. Only the app's own accessories are drawn and names are escaped | ✅ |
| 3 | Someone approves on the guardian's phone | In the app, a yes needs three checks **and the phone's own lock** (fingerprint, face or PIN). On the web page, a yes needs a second "are you sure?" step | ✅ |
| 3 | A scammer on the guardian's own line | A guardian's yes waits while their phone is on a call with a number they don't know: their Peri says "hang up first, then decide", and the server holds the yes back too (the guardian's Peri sends its own key; who decided is kept with the request). "No" and "call them now" always work | ✅ |
| 1 | An app that controls the screen | Peri checks which apps have accessibility (what fake technicians and banking trojans ask for): one that isn't part of the phone or a known screen reader gets a spoken warning, a notification that opens the setting to switch it off, a diary line, and her pear in attention so the family sees it | ✅ |
| 3 | Someone removes the guardians | Opening the family setup needs the phone's lock once a guardian exists; any change to the guardians needs a current guardian's yes and waits 48 hours, and the person can cancel | ✅ |
| 4 | A leaked or previewed link | One-time, 15 minutes, only its SHA-256 hash is stored. Opening it (GET) never decides anything, so link previews are harmless. Silence never approves | ✅ |
| 4 | The page itself | No scripts; `Content-Security-Policy: default-src 'none'; img-src 'self'; form-action 'self'; frame-ancestors 'none'`, `no-referrer`, `no-store`, `nosniff`, `X-Frame-Options: DENY`, HSTS over HTTPS. Peri's pictures come from the same server | ✅ |
| 5 | Prompt injection | "Ignore previous instructions"-style text is its own signal. Models only vote, within ±2, on doubtful messages; hard rules can't be overridden; Peri never reads a model's words aloud | ✅ |
| 6 | A stolen or extracted device token | Each phone gets **its own key**, paired in person: `bun pair` on the server shows a one-time code and its QR (10 minutes, single use, only its hash kept); the phone's camera opens FriendlyPear, which asks before connecting. The server keeps only a hash of each key; the key sits on the phone encrypted by the Android Keystore, never backed up. Any phone can be disconnected alone. `ALLOW_SHARED_TOKEN=0` turns the shared test token off | ✅ |
| 6 | A QR that points Peri elsewhere | The phone only pairs with **its own server** (same scheme, host and port as its build); any other QR is refused with a warning | ✅ |
| 6 | A stolen key, or a family on one wifi | Limits per paired phone (120 requests a minute) as well as per IP, so a key used from many places still counts as one phone and one phone can't hold up the rest of the family; "hang up their call" at most 5 times in 10 minutes per phone | ✅ |
| 6 | Odd requests | A request whose address can't be read gets a 400, not a crash; ids are checked against their exact format before any lookup | ✅ |
| 6 | Backups leaking keys or family setup | `allowBackup="false"` and data-extraction rules exclude everything from cloud backups and device transfers | ✅ |
| 6 | Floods and guessing | Per-IP limits on links, the API, pictures, voice and pairing (10 codes tried per 15 minutes); behind the tunnel the client IP is the one the proxy adds, so a forged header can't dodge them; an IP sending wrong device tokens is locked out for 15 minutes; body size limits; timing-safe token check | ✅ |

## What the family can and can't see

The guardian sees **what kind** of thing is asked (send a code, change the guardians), to whom (a first name), and
whether a stranger was on the line (yes or no). **Never** the code, the message, the phone number that called, the
location or the apps. Peri's news (optional) carries only the kind of event and its level. The person sees exactly
what the guardians see, and can stop sharing at any moment; every link dies at once.

**The family's pears** (the Family section) travel only between the phones paired with the family's own server, and
only with a phone's own key, never the shared test token. Each pear is a name the person chose ("Mum", "Ana"), what
their Peri wears, their role (looked after, or looks after the family) and one word: calm or attention. Attention
is shared only while the person shares Peri's news, and an attention nobody refreshes fades back to calm after two
hours. Never a number, a message, a call, a place or an app; a disconnected phone's pear goes with it. With an
attention, and only then, goes the kind of trap in one word (parcel, link, code, family, remote, prize), so the family
can see the same trap going around.

**"Hang up their call"** carries who asked, and nothing about the call: the family only ever sees "on a call with a number
they don't know", never the number. The family's news reaches a phone with the app closed as notifications that say
no more than the Family section does; on the lock screen they only say there is family news.

**"Was it you?"** carries the asking pear's name and one word, `new_number`: never the message, the number or who it
claims to be. Only the phone that asked reads the answers, each phone answers once, and the question and its answers
are gone after 15 minutes or when that phone is disconnected.

## On the phone

- Contacts are read on the phone and never sent. Only doubtful messages leave it, plus what the person chooses (a
  picture shown to Peri, voice orders if Muse Voice Transcribe is turned on, a scam they ask to have written up as a
  report: the screen says what goes where before anything is sent).
- **What Peri can do** lists every permission and special access, what it's for and how to switch it off, and a log of
  what Peri did with them (calls, texts, location, pictures, voice, reports): only who and when, kept on the phone.
- What the person tells Peri about a call ("they wanted the code") is judged by rules on the phone and never sent.
- The last scam (for "report it") and the last junk website notice (to silence that site) are kept on the phone for
  three days, never backed up. `VIBRATE` and `ACCESS_NETWORK_STATE` are normal permissions Android grants at install:
  touches with warnings, and seeing whether Private DNS is on.
- Ending a call needs Android's `ANSWER_PHONE_CALLS`, asked together with seeing calls (same group, one dialog). Peri
  never answers a call, and ends one only on the person's tap or a guardian's request about a stranger's call.
- The microphone stays closed during any call. If Peri was already listening when a call starts (for example, the call she places after a "yes"), it closes at once and anything it heard is dropped; the app also stops listening when it leaves the screen. Peri never accesses call audio and never blocks or answers calls.
- Permissions are asked one at a time with a reason; a "no" is respected. Installed from a file, Android locks
  message alerts and texts ("restricted settings"); Peri explains Android's own unlock in three steps instead of
  hiding it.
- The debug-only test receiver needs the `DUMP` permission (only `adb` has it). Clear-text HTTP is allowed only in
  debug builds.

## Still open

- **Debug builds still carry the shared test token**, so an unpaired test phone works out of the box. Release builds never do (`buildTypes` in `android/app/build.gradle.kts`, and `eval/release_check.py` fails if it ever gets in). With
  `ALLOW_SHARED_TOKEN=0` it stops working and only paired phones get in (tested: the unpaired phone falls back to
  the on-phone rules, which still warn).
- **SMS is not encrypted.** The link travels by SMS, so it is short-lived, single-use and guarded by the "are you
  sure?" step and the phone lock. A spoofed sender number alone is not enough: the request must exist on the server.
- **Pending requests and "was it you?" questions live in memory** and are lost if the server restarts (a request then
  simply expires: nothing is ever approved by itself, and Peri's warning stands).
- A public HTTPS address (a tunnel or a host) is needed for phones outside the developer's network.
