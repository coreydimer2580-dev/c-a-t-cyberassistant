# C@T - Cyber AI Assistant

C@T is an Android app shell (Kotlin, Jetpack Compose, Room, Navigation) with a neon cyberpunk UI. It is a normal installable APK for phones such as the Samsung Galaxy Z Fold 6. It is not a ROM, not firmware, and it does not scan the phone.

## What this build does

- Dashboard, Memory, Voice, World, and Settings tabs
- In-app feature toggles, memory stacks, voice profiles, and world states
- A local Room database (`cat-memory.db`) created on launch
- A paste-in privacy filter that redacts emails, SSNs, card-like numbers, phone numbers, and secret keywords from text you type

## What this build does not do

- It does not read your files, photos, messages, or tap history
- It does not investigate people or score truth vs lies
- The microphone is not used

## Phone install (Galaxy Z Fold 6)

1. Push this project to GitHub and run the **Build C@T APK** workflow (Actions tab), or build locally.
2. Download the APK artifact (`app-release.apk`).
3. On the Fold, open the APK from Files or Chrome.
4. If Android blocks it, allow installs from that app: Settings → Security and privacy → Install unknown apps.
5. The inner and cover screens both run the same universal APK. This build is signed with the debug keystore so it can be sideloaded. It is not a Play Store upload key.

## Local build

Requires JDK 17+ and Android SDK platform 34.

```bash
# local.properties should contain sdk.dir=/path/to/android-sdk
./gradlew assembleDebug
./gradlew assembleRelease
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`  
Release APK: `app/build/outputs/apk/release/app-release.apk`

## GitHub Actions

`.github/workflows/build-apk.yml` checks out the repo, installs JDK 17 and the Android SDK, runs `./gradlew assembleRelease`, and uploads the APK artifact.
