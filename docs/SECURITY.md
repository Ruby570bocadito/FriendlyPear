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
7. **Scams worded to slip past the rules**: no link, no "urgent", a number to call, a casual family tone, spelling tricks.
8. **Another app on the phone** handing Peri something that isn't a picture.

## What stops each one

| # | Threat | Mitigation | |
|---|---|---|---|
| 1 | Code dictated during a scam call | Hard rule: a code arriving during a call is a scam, whatever any model says; Peri says "hang up" | ✅ |
| 1 | "Tell me the code" after the call | Sending a code is high risk: it waits for a guardian's yes. If a stranger called in the last 30 minutes, the guardian's SMS says **URGENT** and, with two guardians or more, **two of them must say yes**: each gets their own one-time link, so nobody can give both, and a single no stops it. The person hears who said yes as it happens | ✅ |
| 1 | "Open your bank app now" | Safety pause before money and remote-control apps during or after a stranger's call | ✅ |
| 2 | Fake Peri request | The guardian's own Peri only accepts links to **Peri's own server** (same scheme, host and port), from a **saved contact**, and reads the request from the server, never from the SMS. Anything that talks like Peri but links elsewhere is a firm scam: *"this message pretends to be from me"* | ✅ |
| 2 | Fake family member | "New number", secrecy ("don't tell dad"), romance and scareware signals; Peri offers to call the real relative at the saved number | ✅ |
| 2 | "Mum, it's me, new number" | **"Was it you?"**: Peri quietly asks the family's other phones whether any of them wrote from a new number. Each answers once with a tap, with its own key, and Peri says what they answered, or that nobody did. An answer never turns a warning into an OK: even "yes, it was me" ends with "call them on their usual number first". 15 minutes, at most 3 open questions per phone, only while Peri's news is shared | ✅ |
| 2 | The same trap reaching the family | When two or more family pears turn to attention with the same kind of trap within 30 minutes, every Peri warns that it is going around | ✅ |
| 1 | A scammer on the line, right now | A big *Hang up* button whenever a number that isn't in the contacts is on the line. The family sees "on a call with a number they don't know" (only that, only while the person shares Peri's news) and can ask that phone's Peri to hang up: one request at a time, two minutes at most, with each phone's own key; it never ends a call with a contact, and never behind the person's back: Peri says who asks and waits three seconds with *Keep talking* on screen | ✅ |
| 1 | The same scammer calling again | The person can block a scam's number from its card: its calls are turned away before they ring, its texts never read aloud. Kept on the phone, never backed up, undone in Settings | ✅ |
| 1 | The same scammer trying the next one in the family | A number blocked from a scam's card on one phone is turned away on every family phone: calls before they ring, texts never read aloud, a line in the diary. Never a saved contact on that phone, so nobody (not even with a stolen key) can cut a phone off from its own people. Only a fingerprint travels: HMAC-SHA256 of the number keyed with the family's salt, computed on the phone; the server never sees a number and never says whose block it is. Unblocking frees it for everyone; a disconnected phone's blocks go with it | ✅ |
| 1 | A SIM swap (someone takes the number to get the bank's codes) | Peri notices the signs on the phone: a different SIM, or the mobile line dead for 15 minutes while the internet works (no coverage at all, or airplane mode, doesn't count). It says what to do first, in a voice and a notification: call the bank and the phone company from another phone. Their pear shows it to the family, who are asked "was it you?" (did one of them change it); the family's alert never offers to call that number, which may be in the scammer's SIM now. Only a fingerprint of the SIMs in use is kept, sealed | ✅ |
| 2 | A fake approval page | The guardian sees **the asking family member's pear, dressed as the family knows it** (taken from that phone when it asked), on the web page and in the app: a fake page can't copy it. Only the app's own accessories are drawn and names are escaped | ✅ |
| 3 | Someone approves on the guardian's phone | In the app, a yes needs three checks **and the phone's own lock** (fingerprint, face or PIN). On the web page, a yes needs a second "are you sure?" step | ✅ |
| 3 | A guardian in danger themselves | A guardian's yes waits while their own phone is in danger: on a call with a number they don't know (their Peri says "hang up first, then decide"), with a SIM swap's signs, after giving something away, or within a quarter of an hour of a scam Peri stopped there ("sort that out first, then decide"). The server holds the yes back too (the guardian's Peri sends its own key; who decided is kept with the request). "No" and "call them now" always work | ✅ |
| 1 | An app that controls the screen | Peri checks which apps have accessibility (what fake technicians and banking trojans ask for): one that isn't part of the phone or a known screen reader gets a spoken warning, a notification that opens the setting to switch it off, a diary line, and their pear in attention so the family sees it | ✅ |
| 1 | Something already given away | First aid, never a reproach. The person tells Peri in their own words ("I gave them the code", "I sent a Bizum", "my data was leaked"), or picks what happened from a scam's card or from Learn. Peri says the most urgent step calmly and shows the rest in order, each with a button that does it where the phone can: the dialer left empty (the bank's number is the one on the card, never one from a message), the internet switch, the apps list, the report, a call to a guardian. Telling the family turns their pear to "needs a hand" for a day: only that, never what was given. After a card, a bank, money or a remote app, a reminder the next day to check the bank's movements. Understood by rules on the phone; the words never leave it | ✅ |
| 1 | A QR trap on a poster, a letter or a parking meter | In "Look at this picture" the phone itself reads every QR code (offline, light-on-dark ones too, and all of them, so a trap can't hide among harmless codes) and judges by rules where it would lead, without ever opening it: a brand's name on another domain, an address disguised with "@" or invisible letters, code that would run (`javascript:`, `intent:`, `data:`, `file:`), a text or a call to a pay number, a contact that calls itself the bank, the police or family, an app that lets someone control the phone, an open wifi, or words aimed at the AI. Its own red ring says "QR trap". The code's verdict can only make the picture's worse, never better, and stands when the model can't be reached; a trap also arms the safety pause for money apps. Peri says what the code would do, never its address or number | ✅ |
| 3 | Someone removes the guardians | Opening the family setup needs the phone's lock once a guardian exists; any change to the guardians needs the current guardians' yes (two of them, each with their own link, when there are two or more) and waits 48 hours, and the person can cancel. Peri says who approved it | ✅ |
| 4 | A leaked or previewed link | One-time, 15 minutes, only its SHA-256 hash is stored. Opening it (GET) never decides anything, so link previews are harmless. Silence never approves | ✅ |
| 4 | The page itself | No scripts; `Content-Security-Policy: default-src 'none'; img-src 'self'; form-action 'self'; frame-ancestors 'none'`, `no-referrer`, `no-store`, `nosniff`, `X-Frame-Options: DENY`, HSTS over HTTPS. Peri's pictures come from the same server | ✅ |
| 5 | Prompt injection | "Ignore previous instructions"-style text is its own signal. Models only vote, within ±2, on doubtful messages; hard rules can't be overridden; Peri never reads a model's words aloud. The written chat ("Ask Peri") can't be fed by a message: it only holds what the person writes and what Peri answers right then. Words in a QR code aimed at an assistant make it a trap, and a picture that tells the model its code is safe can't lower what the phone found in it | ✅ |
| 6 | A stolen or extracted device token | Each phone gets **its own key**, paired in person: `bun pair` on the server shows a one-time code and its QR (10 minutes, single use, only its hash kept); the phone's camera opens FriendlyPear, which asks before connecting. The server keeps only a hash of each key; the key sits on the phone encrypted by the Android Keystore, never backed up. Any phone can be disconnected alone. `ALLOW_SHARED_TOKEN=0` turns the shared test token off | ✅ |
| 6 | A QR that points Peri elsewhere | The phone only pairs with **its own server** (same scheme, host and port as its build); any other QR is refused with a warning | ✅ |
| 6 | A stolen key, or a family on one wifi | Limits per paired phone (120 requests a minute) as well as per IP, so a key used from many places still counts as one phone and one phone can't hold up the rest of the family; "hang up their call" at most 5 times in 10 minutes per phone | ✅ |
| 6 | Odd requests | A request whose address can't be read gets a 400, not a crash; ids are checked against their exact format before any lookup | ✅ |
| 6 | Backups leaking keys or family setup | `allowBackup="false"` and data-extraction rules exclude everything from cloud backups and device transfers | ✅ |
| 6 | Someone copies the server's files | Everything the server keeps on disk is sealed with AES-256-GCM under a key kept apart from the data (`DATA_KEY`, or a key file outside the data folder made on the first start): the phones' key hashes, the pears, the guardians' links and news, the blocked fingerprints, and the pending approvals and "was it you?" questions, which now survive a restart. A changed byte makes a file fail to open, and its store starts empty rather than trust it | ✅ |
| 6 | Old keys and tokens | Each phone changes its own key with the server once a month, and the old one stops working at once; `bun rotate` changes the admin token, with the old one accepted for one more day so nothing stops | ✅ |
| 6 | What the server writes down | Its log carries no names, numbers, messages, link tokens or browser details: a test drives a family request end to end and checks every line | ✅ |
| 6 | Someone copies the app's files | What Peri keeps about scams and people (the last scam's message and sender, the numbers blocked, who Peri called or texted, the diary, the investigator's notes, the family setup, a junk website's address) is sealed with AES-256-GCM under a key that never leaves Android's keystore: a copy of the files can't be read, and a changed byte makes a file fail to open instead of feeding Peri something false. What was given away after a scam is never stored, not even on the phone | ✅ |
| 6 | Floods and guessing | Per-IP limits on links, the API, pictures, voice and pairing (10 codes tried per 15 minutes); behind the tunnel the client IP is the one the proxy adds, so a forged header can't dodge them; an IP sending wrong device tokens is locked out for 15 minutes; body size limits; timing-safe token check | ✅ |
| 7 | Scams worded to slip past the rules | A **red team**: Muse Spark writes new scams of 15 kinds, in Spanish and English, made to dodge keyword filters; the rules judge them and a person reads every one that gets through. On a batch judged before anyone read it, the rules alone warned about 80 % (40 % with the previous day's rules). The misses became general rules, never copies of a message: a number to call written in the message, a company writing from a personal mobile or a Gmail address, money "sent by mistake", a buyer's fake "safe payment", moving the chat to Telegram, and `.co.uk` links, which weren't seen as links. False alarms on the hand-written held-out set didn't move ([`eval/REPORT.md`](../eval/REPORT.md)) | ✅ |
| 7 | Letters in disguise | "pаypal.com" with a Cyrillic а, an invisible character inside "urgent", an international name (xn--) that decodes to them: the shield reads them as the letters they imitate, so its words and the lookalike hard rule see through them, and the disguise itself is a warning sign. A Spanish ñ in an address or a family emoji is not a disguise | ✅ |
| 8 | Another app sharing an address that isn't a picture with "Look at this picture" | A shared picture is opened only from another app's own storage: never the contacts, the call log, texts, the calendar or FriendlyPear's own files, which Peri would otherwise read and send to be looked at. Test builds also take a file inside the app's own folder, release builds never. Anything else: Peri asks to pick a photo from the gallery | ✅ |

## What the family can and can't see

The guardian sees **what kind** of thing is asked (send a code, change the guardians), to whom (a first name),
whether a stranger was on the line (yes or no), and which other guardians already said yes (first names). **Never** the code, the message, the phone number that called, the
location or the apps. Peri's news (optional) carries only the kind of event and its level. The person sees exactly
what the guardians see, and can stop sharing at any moment; every link dies at once.

**The family's pears** (the Family section) travel only between the phones paired with the family's own server, and
only with a phone's own key, never the shared test token. Each pear is a name the person chose ("Mum", "Ana"), what
their Peri wears, their role (looked after, or looks after the family) and one word: calm or attention. Attention
is shared only while the person shares Peri's news, and an attention nobody refreshes fades back to calm after two
hours. Never a number, a message, a call, a place or an app; a disconnected phone's pear goes with it. With an
attention, and only then, goes the kind of trap in one word (parcel, link, code, family, remote, prize, callback, payment, official, bill, romance; or call, breach
or sim: a stranger on the line, a hand wanted after a scam, a SIM swap's signs), so the family can see the same trap
going around.

**"Hang up their call"** carries who asked, and nothing about the call: the family only ever sees "on a call with a number
they don't know", never the number. The family's news reaches a phone with the app closed as notifications that say
no more than the Family section does; on the lock screen they only say there is family news.

**Blocked numbers** travel only as fingerprints (HMAC-SHA256 under the family's salt, computed on the phone), between the phones paired with the family's server and with each phone's own key. No phone learns who in the family blocked a number, and a family block never applies to a saved contact.

**"Was it you?"** carries the asking pear's name and one word, `new_number` or `sim`: never the message, the number,
who it claims to be, or anything about the SIM. Only the phone that asked reads the answers, each phone answers once, and the question and its answers
are gone after 15 minutes or when that phone is disconnected.

## On the phone

- Contacts are read on the phone and never sent. Only doubtful messages leave it, plus what the person chooses (a
  picture shown to Peri, voice orders if Muse Voice Transcribe is turned on, a scam they ask to have written up as a
  report: the screen says what goes where before anything is sent).
- **What Peri can do** lists every permission and special access, what it's for and how to switch it off, and a log of
  what Peri did with them (calls, texts, location, pictures, voice, reports): only who and when, kept on the phone.
- What the person tells Peri about a call ("they wanted the code") is judged by rules on the phone and never sent.
- **"Ask Peri"**, the written chat, holds only two voices: what the person writes or taps, and what Peri answers to it
  right then. A message or a notification that arrives meanwhile never shows up in it, and messages aren't read out from
  it, so a scammer's words can't pass for Peri's. Her answers are written and reviewed in advance (19 situations, in both
  languages), never a model's words; the questions are understood on the phone and never leave it.
- The **security checkup** reads, on the phone, whether there's a screen lock, how old the last security update is,
  whether USB debugging is on, which apps can control the screen, whether a Private DNS is set, and Peri's own access to
  alerts and calls. It sends nothing; each fix opens Android's own setting.
- **QR codes in a picture** are read and judged on the phone (ZXing, Apache 2.0, offline). What a code holds is never
  opened, followed, sent or logged; only the picture goes to be looked at, as before. Security-tested with eleven trap
  posters with no network and six with the model: a lookalike parcel site, an address hiding behind "@", invisible
  right-to-left letters, `javascript:`, a pay SMS, a fake bank contact, a light-on-dark code, two codes on one page,
  AnyDesk from the store, and two that tell the AI the code is safe. All were traps, nothing was opened, and the logs
  held none of their addresses or numbers. `eval/qr/posters.py` makes the posters and `eval/qr/share.sh` shares them
  with the app.
- The last scam (for "report it") and the last junk website notice (to silence that site) are kept on the phone for
  three days, sealed with the keystore key, never backed up. `VIBRATE` and `ACCESS_NETWORK_STATE` are normal permissions Android grants at install:
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
- **The server's data key** must be backed up apart from the data: without it, the sealed files can't be opened and the
  family pairs its phones again (nothing is ever approved by itself meanwhile, and Peri's warnings stand).
- A public HTTPS address (a tunnel or a host) is needed for phones outside the developer's network.
- **A QR code the phone can't read** (tiny, blurred or cut off in the picture) is left to the model, which can ring it
  but can't say where it leads; Peri then says not to scan it if the model saw it as a risk.
- **Rules can always be outworded.** Each red team batch finds wording the rules don't know yet (80 % warned on the
  last one before its misses were read), and a courier's "small customs charge" from a personal number still gets past.
  Doubtful messages go to the models and risky actions to the family; the red team is meant to run every week, and for
  now runs by hand.
- **A blocked number's fingerprint can be reversed by trying every phone number**, by someone who has the server's files (the family's salt is kept there too). The server never receives a number and its files hold none, but a fingerprint isn't secret from whoever runs the server.
