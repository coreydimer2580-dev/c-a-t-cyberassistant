# C@T - Cyber AI Assistant

Version 1.7 — Autopilot + evolving memory. Kotlin, Jetpack Compose, Room. Neon cyan, magenta, and lime on black.

Opens on **Chat** (ChatGPT-style). Primary tabs: **Chat · Wheel · Memory · Settings**.

## AI product

- **Chat** — sticky composer, typing indicator, persona labels, thin system strip
- **Autopilot** — speaks replies (TTS en-AU) and **one** follow-up chip, then waits for you
- **Evolve** — ranks recent chat + saved notes into a short "best so far" Unsure memory
- **Online evolve** (optional) — public Wikipedia / DuckDuckGo Instant Answer only. Never browser history, OneDrive, or other apps
- **AI Wheel** — Offline C@T, Cloud GPT, Auto, Analyst, Coder, Coach, Creative
- **Group solver** — Analyst + Coder + Coach (and Cloud if mode allows) → merged verdict
- **Offline + Online** — Offline always works. Cloud uses your OpenAI-compatible endpoint
- **Memory** — Room notes on this phone (True / False / Unsure). No expiry

## Still true

- Free only; no paid gateways
- No root, custom ROM, satellite, radio/mesh, Wi‑Fi scanning
- No browser history or other-app scrapes
- `/sms` and `/call` open the phone's own apps only
- Australia/Perth, en-AU
- Fold-friendly wide layout (sidebar on inner display)

## Local build

Requires JDK 17+ and Android SDK platform 34.

```bash
./gradlew assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release.apk`
