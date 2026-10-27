# Security and privacy

FriendlyPear stands between an older person and people actively trying to rob them, so the app itself must be hard
to turn against them. This page lists each threat, what stops it, and what is still open. Everything marked ✅ is
in the code and covered by tests or by the two-phone test of Oct 27.

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
| 3 | Someone approves on the guardian's phone | In the app, a yes needs three checks **and the phone's own lock** (fingerprint, face or PIN). On the web page, a yes needs a second "are you sure?" step | ✅ |
| 3 | Someone removes the guardians | Opening the family setup needs the phone's lock once a guardian exists; any change to the guardians needs a current guardian's yes and waits 48 hours, and the person can cancel | ✅ |
| 4 | A leaked or previewed link | One-time, 15 minutes, only its SHA-256 hash is stored. Opening it (GET) never decides anything, so link previews are harmless. Silence never approves | ✅ |
| 4 | The page itself | No scripts; `Content-Security-Policy: default-src 'none'; img-src 'self'; form-action 'self'; frame-ancestors 'none'`, `no-referrer`, `no-store`, `nosniff`, `X-Frame-Options: DENY`, HSTS over HTTPS. Peri's pictures come from the same server | ✅ |
| 5 | Prompt injection | "Ignore previous instructions"-style text is its own signal. Models only vote, within ±2, on doubtful messages; hard rules can't be overridden; Peri never reads a model's words aloud | ✅ |
| 6 | A stolen or extracted device token | Each phone gets **its own key**, paired in person: `bun pair` on the server shows a one-time code and its QR (10 minutes, single use, only its hash kept); the phone's camera opens FriendlyPear, which asks before connecting. The server keeps only a hash of each key; the key sits on the phone encrypted by the Android Keystore, never backed up. Any phone can be disconnected alone. `ALLOW_SHARED_TOKEN=0` turns the shared test token off | ✅ |
| 6 | A QR that points Peri elsewhere | The phone only pairs with **its own server** (same scheme, host and port as its build); any other QR is refused with a warning | ✅ |
| 6 | Backups leaking keys or family setup | `allowBackup="false"` and data-extraction rules exclude everything from cloud backups and device transfers | ✅ |
| 6 | Floods and guessing | Per-IP limits on links, the API, pictures, voice and pairing (10 codes tried per 15 minutes); behind the tunnel the client IP is the one the proxy adds, so a forged header can't dodge them; an IP sending wrong device tokens is locked out for 15 minutes; body size limits; timing-safe token check | ✅ |

## What the family can and can't see

The guardian sees **what kind** of thing is asked (send a code, change the guardians), to whom (a first name), and
whether a stranger was on the line (yes or no). **Never** the code, the message, the phone number that called, the
location or the apps. Peri's news (optional) carries only the kind of event and its level. The person sees exactly
what the guardians see, and can stop sharing at any moment; every link dies at once.

## On the phone

- Contacts are read on the phone and never sent. Only doubtful messages leave it, plus what the person chooses (a
  picture shown to Peri, voice orders if Muse Voice Transcribe is turned on).
- The microphone stays closed during any call. Peri never accesses call audio and never blocks or answers calls.
- Permissions are asked one at a time with a reason; a "no" is respected. Installed from a file, Android locks
  message alerts and texts ("restricted settings"); Peri explains Android's own unlock in three steps instead of
  hiding it.
- The debug-only test receiver needs the `DUMP` permission (only `adb` has it). Clear-text HTTP is allowed only in
  debug builds.

## Still open

- **Debug builds still carry the shared test token**, so an unpaired test phone works out of the box. With
  `ALLOW_SHARED_TOKEN=0` it stops working and only paired phones get in (tested: the unpaired phone falls back to
  the on-phone rules, which still warn).
- **SMS is not encrypted.** The link travels by SMS, so it is short-lived, single-use and guarded by the "are you
  sure?" step and the phone lock. A spoofed sender number alone is not enough: the request must exist on the server.
- **Pending requests live in memory** and are lost if the server restarts (the request then simply expires: nothing
  is ever approved by itself).
- A public HTTPS address (a tunnel or a host) is needed for phones outside the developer's network.
