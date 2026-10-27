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
| 6 | Floods and guessing | Per-IP limits on links, the API, pictures and voice; an IP sending wrong device tokens is locked out for 15 minutes; body size limits; timing-safe token check | ✅ |

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

- **One shared device token in debug builds.** Anyone who unpacks the debug APK can call the API within the rate
  limits. The plan is per-device keys paired in person with a QR code (`emparejamiento-qr` in the roadmap).
- **SMS is not encrypted.** The link travels by SMS, so it is short-lived, single-use and guarded by the "are you
  sure?" step and the phone lock. A spoofed sender number alone is not enough: the request must exist on the server.
- **Pending requests live in memory** and are lost if the server restarts (the request then simply expires: nothing
  is ever approved by itself).
- A public HTTPS address (a tunnel or a host) is needed for phones outside the developer's network.
