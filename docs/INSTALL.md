# Installing FriendlyPear on a real phone

FriendlyPear is not on Google Play yet, so it is installed from a file (an APK). Android protects people from
exactly the kind of app that reads texts and notifications, because that is what banking malware does. A scam guard
needs the same access for the opposite reason, so the phone may block it. This page says why, and the way that works.

## Why the phone may block it

- **Play Protect's enhanced fraud protection.** In a growing list of countries, Google blocks apps installed from a
  browser, a messaging app or a file manager when they ask for SMS, notification access or accessibility. FriendlyPear
  asks for the first two (to read scam texts and WhatsApp notifications), so the install can be refused outright.
- **Restricted settings (Android 13 and later).** An app installed from a file can't turn on notification access or
  accessibility until you allow it in its App info.
- **Developer verification (from 30 September 2026 in Brazil, Indonesia, Singapore and Thailand; worldwide later).**
  Apps from developers Google hasn't verified can't be installed from a file on certified phones in those countries.

Installing over USB with `adb` is exempt from all three. It is the way we recommend for the hackathon.

## Recommended: over USB (5 minutes, needs a computer)

1. On the phone: **Settings › About phone**, tap **Build number** seven times. Then **Settings › System › Developer
   options**, turn on **USB debugging**.
2. Connect the phone to the computer with a cable and accept **"Allow USB debugging?"** on the phone.
3. On the computer, with the Android SDK's platform tools (`adb`):

   ```bash
   adb devices
   adb install -r FriendlyPear.apk
   ```

   `adb devices` should list the phone as `device`. No cable? Android 11 and later can pair over Wi-Fi: Developer
   options › **Wireless debugging** › **Pair device with pairing code**, then `adb pair <ip>:<port>` and
   `adb connect <ip>:<port>`.
4. Open FriendlyPear. Peri asks for each permission the first time it's needed and says what it's for.

## Without a computer

1. Download the APK on the phone and open it. Android asks to allow installs from that app (the browser or the file
   manager): allow it for this once.
2. If Play Protect warns, it may offer **More details › Install anyway**. Where enhanced fraud protection is active,
   it won't: use USB.
3. If notification access is greyed out with **"Restricted setting"**, Peri's unlock guide opens the right screens:
   **App info › ⋮ › Allow restricted settings**, then turn it on.

## What works where

| | On the phone alone | With Peri's server |
|---|---|---|
| Scam check of texts and notifications (rules) | ✔ | ✔ |
| Code during a call, unknown callers, remote-control apps | ✔ | ✔ |
| Peri, her voice (Android's), Mini Peri, the chat, the diary | ✔ | ✔ |
| Second opinion and investigator (Meta models) for doubtful messages | | ✔ |
| "Look at a picture", Muse Voice Transcribe | | ✔ |
| The family: approvals, "was it you?", Peri to Peri | | ✔ |

The server runs on a computer (`cd backend; bun src/main.ts`). A phone reaches it on the same Wi-Fi with an APK built
for that address (`./gradlew :app:assembleDebug -Pfriendlypear.backendUrl=http://<computer-ip>:8711`), or anywhere
once it has a public HTTPS address.

## The release APK is the one to hand out

`./gradlew :app:assembleRelease` builds it: signed with our own key (kept outside the repository), shrunk by R8, and
without the shared test token, so it talks to Peri's server only once the phone is paired in person. The debug build
carries that token for testing and is never published.
