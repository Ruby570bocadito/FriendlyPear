# 🍐 FriendlyPear

**An AI voice agent that protects anyone — and especially older adults — from scams, and keeps their family in the loop.**

Peri, a friendly pear that lives in your phone, reads your messages aloud, calls whoever you ask, and when something smells like a scam it checks with the real person or with your family before you lose a cent.

Built for the **Meta Global AI Developer Hackathon** · Indie track · *Build for Your World*.

> All product code was written Oct 27 – Nov 4, 2026. Concept and design documents were drafted before the building period.

## Status

| Part | State |
|---|---|
| Project structure, build and CI | Ready |
| Shield, voice, agent, circle of trust, family MFA, caller verification, consent-first permissions | In progress — see [`docs/agentes/ROADMAP.md`](docs/agentes/ROADMAP.md) |

## Meta models

| Model | Job in FriendlyPear |
|---|---|
| Muse Glimmer (open weights, local) | Private first opinion on doubtful messages |
| Muse Spark | Investigator agent with tools, summaries for guardians, plain-language explanations |
| Muse Spark (multimodal) | "Look at this picture": letters, invoices, QR codes, screenshots |
| Muse Voice Transcribe | All voice input |
| SAM 3.1 | Points at the dangerous QR or link in a photo |
| Muse Image | Peri's illustrated states |

## Layout

```
core/      Kotlin/JVM library: shield, voice, agent, trust, alerts. Tested without a phone.
android/   Android app (Kotlin, Android 12+). Includes core/ as a composite build.
backend/   Bun + TypeScript: guardian approvals, investigator agent, model clients.
models/    How to serve Muse Glimmer locally or on a partner.
eval/      Held-out test set and metrics scripts.
training/  Scam knowledge base, dataset scripts, learned weights and fine-tuning.
assets/    Peri's illustrations and the prompts used to make them.
docs/      Concept, architecture, demo script and the multi-agent protocol.
```

## Docs

| File | What |
|---|---|
| [docs/IDEA.md](docs/IDEA.md) | The full idea, from problem to goals |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Milestones and day-by-day plan |
| [docs/HACKATHON.md](docs/HACKATHON.md) | Hackathon rules, judging and prizes |
| [docs/ANDROID_STUDIO.md](docs/ANDROID_STUDIO.md) | Dev environment, emulator and testing |
| [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md) | Architecture and modules |
| [docs/ENTRENAMIENTO.md](docs/ENTRENAMIENTO.md) | Training layers, data and honest evaluation |
| [docs/VIDEO.md](docs/VIDEO.md) | Demo video, promo ad and Higgsfield MCP |
| [docs/DISENO.md](docs/DISENO.md) | Animated UI, colour palette and Peri's states |
| [docs/MCP_ANDROID.md](docs/MCP_ANDROID.md) | Connect Claude Code to the emulator (mobile-mcp) and to Android Studio (IDE MCP) |

## Build

```bash
cd core && ./gradlew test                     # JDK 17+
cd backend && bun test                        # Bun >= 1.1
cd android && ./gradlew :app:assembleDebug    # Android SDK 35 (sdk.dir in android/local.properties)
```

Secrets never go in the repo: the backend reads `backend/.env` (see `.env.example`), the app reads `~/.gradle/gradle.properties`.
