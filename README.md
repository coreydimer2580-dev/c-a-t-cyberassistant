# C@T - Cyber AI Assistant

Version 1.4. Kotlin, Jetpack Compose, Room, and Navigation. Neon cyan, magenta, and lime on black. The cover screen keeps a tab row; the inner display (600dp and wider) uses a sidebar. Chat is a bottom composer with a streaming-style reply. Saved memory shows live and stays in Room with no expiry. Offline answers use those notes. Locale is en-AU. Clocks use Australia/Perth first. This is a normal APK. It is not a ROM and it does not scan the phone.

## What this build does

- Live memory: `/remember` and the Memory tab write Room notes that stay until you clear them (no expiry). They appear on screen as they are saved. Offline replies use matching notes.
- Offline copilot is the default. Chat does not wait on Wi-Fi. Commands include `/help`, `/remember`, `/recall`, `/tools`, `/time`, `/todo`, `/hash`, `/b64`, `/json`, `/convert`, and `/pass`
- Cloud and Auto may use a network when one is available, then fall back to the offline reply. No paid API is required. Settings has an emulator host preset of `http://10.0.2.2:11434/v1` for a free local Ollama server (blank API key)
- Tools tab (all on device): route log (typed label, from, to, note, Australia/Perth time), notes, clipboard scrubber, passphrase strength, unit converter, Australia/Perth world clock, passphrase generator, Base64, JSON pretty, SHA-256, timer, checklist
- `/call` and `/sms`, and the Tools buttons, open the phone's own dialer (`ACTION_DIAL`, `tel:`) or SMS app (`ACTION_SENDTO`, `sms:`). C@T does not send the message or place the call. There is no carrier-free network and no paid SMS gateway
- API key is stored in encrypted preferences when the security library is available
- Paste-in filter still redacts emails, SSNs, card-like numbers, phone numbers, and secret keywords before save or an optional cloud send

## What this build does not do

- It does not read files, photos, messages, call logs, or tap history
- It does not send SMS or place calls itself
- It does not root the phone or install a custom ROM
- It does not scan Wi-Fi, join a Bluetooth mesh, or use radio or frequency hopping. Route log lines are typed by you
- It does not investigate people or score truth vs lies

## Local build

Requires JDK 17+ and Android SDK platform 34.

```bash
./gradlew assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release.apk`
