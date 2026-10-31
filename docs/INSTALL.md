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
- **Developer verification (from 30 September 2026 in Brazil, Indonesia, Singapore and Thailand; worldwide in 2027).**
  An app whose developer hasn't registered it with Google then installs only over USB or through Android's slower
  "advanced flow" (developer options and a one-day wait). FriendlyPear is getting registered: see below.

Installing over USB with `adb` is exempt from all three. It is the way we recommend for the hackathon.

## Recommended: over USB (5 minutes, needs a computer)

1. On the phone: **Settings › About phone**, tap **Build number** seven times. Then **Settings › System › Developer
   options**, turn on **USB debugging**.
2. Connect the phone to the computer with a cable and accept **"Allow USB debugging?"** on the phone.
3. Download `FriendlyPear.apk` from the [latest release](https://github.com/Ruby570bocadito/FriendlyPear/releases/latest)
   (its SHA-256 is listed there). Then, on the computer, with the Android SDK's platform tools (`adb`):

   ```bash
   adb devices
   adb install -r FriendlyPear.apk
   ```

   `adb devices` should list the phone as `device`. No cable? Android 11 and later can pair over Wi-Fi: Developer
   options › **Wireless debugging** › **Pair device with pairing code**, then `adb pair <ip>:<port>` and
   `adb connect <ip>:<port>`.
4. Open FriendlyPear. Peri asks for each permission the first time it's needed and says what it's for.

## Without a computer

1. Download the APK on the phone from the [latest release](https://github.com/Ruby570bocadito/FriendlyPear/releases/latest) and open it. Android asks to allow installs from that app (the browser or the file
   manager): allow it for this once.
2. If Play Protect warns, it may offer **More details › Install anyway**. Where enhanced fraud protection is active,
   it won't: use USB.
3. If notification access is greyed out with **"Restricted setting"**, Peri's unlock guide opens the right screens:
   **App info › ⋮ › Allow restricted settings**, then turn it on.

## After installing: Check my phone

On a real phone Android puts a few things in Peri's way. Open **Check my phone** on Peri's home: it finds each one and
opens the right screen, with one clear step.

- **Battery saving** can put her to sleep, and the family's alerts arrive late: Android's own one-tap question lets
  her run (if her battery was set to *Restricted*, the app's page opens instead).
- **Showing the pause over other apps**: during a suspicious call she puts a pause in front of a bank app, and Mini
  Peri floats over the others. Android asks first.
- **The call filter**: to warn before you pick up a number you don't know, on phones that offer it.
- **Codes hidden on Android 15**: codes in notifications are hidden from apps like Peri; with texts allowed she sees
  a code arriving by SMS during a call.
- **Restricted settings** for apps installed from a file: the unlock guide walks through Android's own three steps.

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

## Registering FriendlyPear with Android

So the release APK keeps installing normally once developer verification reaches every country, FriendlyPear is
registered in Google's Android Developer Console:

1. A **limited distribution account** is enough to begin: free, only an email, for up to 20 devices (made for
   students, hobbyists and, here, a family). Handing it out widely needs a full account, with the developer's identity
   checked.
2. Register the package **`app.friendlypear`** with the certificate the release APK is signed with, SHA-256
   `28:56:88:7A:36:D2:38:CD:7E:B7:06:D3:86:2D:DC:D7:7A:98:71:9B:3E:B7:B0:AE:CA:DD:24:38:6F:B3:CC:BF`. It's public: anyone can read it from the APK with
   `apksigner verify --print-certs FriendlyPear.apk`. The key itself never leaves the developer's computer.
3. From then on, Android Studio shows the registration status each time it builds a signed APK.

Until 2027 nothing changes for installs in Spain, and installing over USB with `adb` never needs a registration.
Sources: the Android Developers Blog,
[March 2026](https://android-developers.googleblog.com/2026/03/android-developer-verification-rolling-out-to-all-developers.html)
and [June 2026](https://android-developers.googleblog.com/2026/06/android-developer-verification.html).
