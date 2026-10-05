# C@T - Cyber AI Assistant

Version 1.6 — AI-first. Kotlin, Jetpack Compose, Room. Neon cyan, magenta, and lime on black.

Opens on **Chat** (ChatGPT-style). Primary tabs: **Chat · Wheel · Memory · Settings**. Tools (/call, /sms, notes) sit under Settings → More tools.

## AI product

- **Chat** — sticky composer, typing indicator, persona labels on replies, New chat, system strip (Online/Offline, mode, active AI, Group toggle)
- **AI Wheel** — circular snap wheel: Offline C@T, Cloud GPT, Auto, Analyst, Coder, Coach, Creative. Selecting a spoke sets who answers next
- **Group solver** — one question → Analyst + Coder + Coach (and Cloud if mode allows) → merged group verdict. Works fully offline
- **Offline + Online** — Offline engine always works without Wi‑Fi. Cloud uses your free OpenAI-compatible endpoint. Auto falls back offline on failure
- **Memory** — Room notes on this phone (True / False / Unsure). Feeds the AI. No expiry

## Still true

- Free only; no paid gateways
- No root, custom ROM, satellite, radio/mesh, Wi‑Fi scanning
- `/sms` and `/call` open the phone's own apps only
- Australia/Perth, en-AU
- Fold-friendly wide layout (sidebar on inner display)

## Local build

Requires JDK 17+ and Android SDK platform 34.

```bash
./gradlew assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release.apk`
