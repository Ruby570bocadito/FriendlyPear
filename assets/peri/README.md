# Peri

A friendly round green pear with a leaf on top. She never scolds and celebrates when you ask before acting (`docs/PERI_VOZ.md`).

All illustrations are made with **Muse Image** (`muse-image-1.0`) by [`generate.ts`](generate.ts): one base Peri from the description in `docs/DISENO.md`, then each state as an **edit of that base**, so she stays the same character. The exact prompt of every image is in [`prompts.json`](prompts.json). Cost: $0.01 per image; the whole set, plus one redo of "calm", was $0.11.

| State | When | Image |
|---|---|---|
| calm | Nothing going on | `peri_calm.webp` |
| listening | Microphone open | `peri_listening.webp` |
| thinking | Waiting for the second opinion | `peri_thinking.webp` |
| worried | Doubtful message, unknown caller, emergency countdown | `peri_worried.webp` |
| protective | Scam, code during a call, remote-control app, bank app during a stranger's call | `peri_protective.webp` |
| calling | Calling family | `peri_calling.webp` |
| waiting | Waiting for a guardian's approval | `peri_waiting.webp` |
| happy | Approved, message sent, "I'm fine" | `peri_happy.webp` |
| sleeping | Night, nothing going on | `peri_sleeping.webp` |

The app uses 768 px copies in `android/app/src/main/res/drawable-nodpi/`, with a cross-fade between states and a slow breath (none with "remove animations" on).

```bash
cd backend; bun ../assets/peri/generate.ts
```

Regenerate a single state from the saved base with `bun ../assets/peri/generate.ts calm`.
