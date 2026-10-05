# C@T - Cyber AI Assistant

Version 1.8 — locked Terminal, apart from Chat. Kotlin, Jetpack Compose, Room. Neon cyan, magenta, and lime on black.

Opens on **Chat**. Primary tabs: **Chat · Terminal · Wheel · Memory · Settings**.

## Terminal

- Own screen. Not mixed into Chat.
- PIN gate (4–8 digits). Unlock lasts until you tap Lock or the app process ends.
- PIN is a salted SHA-256 in encrypted preferences. The PIN itself is not stored.
- Transcript is AES-GCM in its own file. The key lives in encrypted preferences. It never writes Chat history.
- English AI (`C@T>`), offline or your cloud endpoint. Obvious command typos are auto-corrected.
- Not a system shell. No Termux, no self-update, no background web scan.

## AI product

- **Chat** — sticky composer, typing indicator, persona labels, thin system strip
- **Autopilot** — speaks replies (TTS en-AU) and **one** follow-up chip, then waits for you
- **Evolve** — ranks recent chat + saved notes into a short "best so far" Unsure memory
- **Online evolve** (optional, Chat only) — public Wikipedia / DuckDuckGo Instant Answer only. Never browser history, OneDrive, or other apps
- **AI Wheel** — Offline C@T, Cloud GPT, Auto, Analyst, Coder, Coach, Creative
- **Group solver** — Analyst + Coder + Coach (and Cloud if mode allows) → merged verdict
- **Memory** — Room notes on this phone (True / False / Unsure). No expiry

## Still true

- Free only; no paid gateways
- No root, custom ROM, satellite, radio/mesh, Wi‑Fi scanning
- No browser history or other-app scrapes
- No continuous crawler and no self-modifying updates
- `/sms` and `/call` open the phone's own apps only (Chat)
- Australia/Perth, en-AU
- Fold-friendly wide layout (sidebar on inner display)

## Local build

Requires JDK 17+ and Android SDK platform 34.

```bash
./gradlew assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release.apk`
