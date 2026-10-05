# C@T - Cyber AI Assistant

Version 1.2. Kotlin, Jetpack Compose, Room, and Navigation. Neon cyberpunk UI for phones such as the Samsung Galaxy Z Fold 6 (cover screen stays one column; the inner display, 600dp and wider, uses multiple columns). This is a normal APK. It is not a ROM and it does not scan the phone.

## What this build does

- Chat is the copilot: bubbles, Room history, clear thread, regenerate last reply
- Offline mode (default) routes greetings, help, remember/recall, summarize, settings, and fold tips, and uses recent chat plus local notes
- Cloud mode posts the filtered thread to an OpenAI-compatible `/chat/completions` URL. Auto tries cloud, then offline. Failures show an error chip and still answer offline
- API key is stored in encrypted preferences when the security library is available
- Dashboard shows mode, last reply snippet, and memory count. Launch opens chat
- Memory notes and chat live in the local Room database `cat-memory.db`
- Paste-in filter still redacts emails, SSNs, card-like numbers, phone numbers, and secret keywords before save or cloud send

## What this build does not do

- It does not read files, photos, messages, or tap history
- It does not investigate people or score truth vs lies
- The microphone and shared storage are not used

## Install from GitHub Releases

After version `v1.1` is published, the APK URL looks like:

`https://github.com/coreydimer2580-dev/c-a-t-cyberassistant/releases/download/v1.2/app-release.apk`

Example for this account:

`https://github.com/coreydimer2580-dev/c-a-t-cyberassistant/releases/download/v1.2/app-release.apk`

Latest-release pattern (only works if that release has an asset named `app-release.apk`):

`https://github.com/coreydimer2580-dev/c-a-t-cyberassistant/releases/latest/download/app-release.apk`

On the Fold 6, open that link in Chrome, download the APK, and allow installs if Android asks (Settings, Security and privacy, Install unknown apps). The same APK runs on the cover and the inner display. It is signed with the debug keystore for sideload, not a Play Store upload key.

You can also download the APK from the **Build C@T APK** Actions artifact without creating a Release.

## Local build

Requires JDK 17+ and Android SDK platform 34.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`  
Release APK: `app/build/outputs/apk/release/app-release.apk`

## GitHub Actions

`.github/workflows/build-apk.yml` runs on push to `main` and on `workflow_dispatch`. It runs `./gradlew assembleRelease` and uploads `app/build/outputs/apk/release/*.apk`.
