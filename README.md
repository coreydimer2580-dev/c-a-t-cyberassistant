# C@T - Cyber AI Assistant

Version 1.18 — Gemini key-only preset: in Online (or Settings) tap **Use Gemini**, paste your own Gemini API key, Save. Built on 1.17 Autopilot speech ON by default + 1.16 + 1.15 ChatGPT-style **Online** tab (no login, no key: Offline English AI answers; optional cloud only with your own URL + key), plus the 1.14 offline Auto fixes. Kotlin, Jetpack Compose, Room. Neon cyan, magenta, and lime on black.

Opens on **Terminal**. Primary tabs: **Terminal · Chat · Online · Wheel · Memory · Settings**.

## Online

- ChatGPT-style bubbles with typing animation, Copy, and ✎ New.
- Works with no login and no key: answers come from the same Offline English AI as Chat.
- Optional cloud: tap the header to paste your own OpenAI-compatible URL + key. Labelled "Optional cloud" only when both are saved.
- **Use Gemini** (v1.18): fills Google's official OpenAI-compatible endpoint `https://generativelanguage.googleapis.com/v1beta/openai` and model `gemini-flash-latest`. You only paste your own key (Google AI Studio). No key ships in the app.
- Emulator/loopback addresses (10.0.2.2, localhost, 127.x) are refused and never contacted.
- If the cloud fails, is quiet, or there's no network, the reply falls back to offline. No API key ships in the app.

## Terminal

- Own screen. Not mixed into Chat.
- PIN gate (4–8 digits). Unlock lasts until you tap Lock or the app process ends.
- PIN is a salted SHA-256 in encrypted preferences. The PIN itself is not stored.
- Transcript is AES-GCM in its own file. The key lives in encrypted preferences. It never writes Chat history.
- English AI (`C@T>`). Default voice is Analyst. Coder, Coach, and Creative still apply when the Wheel names them.
- **Auto** on the Terminal strip sets Auto mode. If Private is on, a short dialog asks before turning Private off. If you keep Private, Auto stays offline.
- Evolve path on each send: **YOU → SoftCorrect → MEMORY → REPLY → Evolve note**. The rail lights one step at a time. PRIVATE lights when Private is on.
- After a reply, if a note is worth keeping, **EVOLVE** flashes and one line shows what was saved, including informal phrases you actually typed. Shared slang also ranks those notes higher.
- Replies rank saved Room memories that share your words (exact tokens and True tags first).
- Empty transcript shows four English example chips.
- Command typos and one-edit near-misses are repaired. Ordinary sentences are not.
- **Private** ON shows a large PRIVATE badge, a lock on the composer, forces offline answers (no cloud, no online search), and sets `FLAG_SECURE`.
- `/clear` asks first. `/clear yes` wipes this Terminal vault only. Chat stays.
- Not a system shell. No Termux, no packages, no self-update, no background web scan.

## AI product

- **Chat** — sticky composer, typing indicator, persona labels, thin system strip
- **Autopilot** — speech on **Chat only** (TTS en-AU) starts **ON** when unset; **one** follow-up chip, then waits. Tap Speak to turn off. Terminal does not speak and does not use the mic.
- **Evolve** — ranks recent chat + saved notes into a short "best so far" Unsure memory, and keeps a few of your own phrases (style)
- **Online evolve** (optional, Chat only, when you send) — public Wikipedia / DuckDuckGo Instant Answer only. Never while the app sleeps. Never browser history, OneDrive, or other apps. Never updates the app.
- **AI Wheel** — Offline C@T, Cloud GPT, Auto, Analyst, Coder, Coach, Creative
- **Group solver** — Analyst + Coder + Coach (and Cloud if mode allows) → merged verdict
- **Memory** — Room notes on this phone (True / False / Unsure). Style notes (how you talk) sit at the top. No expiry
- **Settings** — **Use Offline only** and **Try Cloud if set**. Cloud address stays behind an optional section.

## Still true

- Free only; no paid gateways
- No root, custom ROM, satellite, radio/mesh, Wi‑Fi scanning
- No browser history or other-app scrapes
- No continuous crawler and no self-modifying updates
- `/sms` and `/call` open the phone's own apps only (Chat)
- Australia/Perth, en-AU
- Fold-friendly wide layout (sidebar on inner display; Terminal path beside the transcript)

## Local build

Requires JDK 17+ and Android SDK platform 34.

```bash
./gradlew assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release.apk`
