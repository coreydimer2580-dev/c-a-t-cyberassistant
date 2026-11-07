# C@T build, continuity, and recovery runbook

This file records the current project goal and repeatable build/recovery steps so the work remains in the repository.

## Project source of truth

- Repository: `coreydimer2580-dev/c-a-t-cyberassistant`
- Android app module: `app/`
- Product behavior and current features: `README.md`
- Persistent Copilot guidance: `.github/copilot-instructions.md`
- APK build workflow: `.github/workflows/build-apk.yml`

Keep this runbook and the source code in GitHub. Do not treat a running website or server as the only copy of the project.

## Goal and scope recorded from the request

- Continue upgrading the existing C@T app, not an unidentified duplicate.
- Review Grok-4.7, Grokbot, or Grok app material only when the user supplies an authorized export or otherwise makes the material available. No conversation history was imported by writing this runbook.
- Incorporate only relevant, beneficial requirements and data, with sensitive information removed and privacy behavior preserved.
- Identify any duplicate app precisely before considering removal. Do not remove it without explicit confirmation, a verified backup, and a rollback plan.
- Oracle-server deployment is not part of the automatic Android build. It requires separately authorized access, a deployment plan, human review, and a verified backup. No credentials belong in GitHub files or chat.

## Build locally

Requirements: JDK 17+ and Android SDK platform 34, as documented in the project README and Android module configuration.

From the repository root:

```bash
./gradlew assembleRelease
```

Expected output:

```text
app/build/outputs/apk/release/app-release.apk
```

Run available tests before release when applicable:

```bash
./gradlew test
```

A successful local build does not by itself mean the APK has been installed, signed with a production key, or deployed.

## GitHub Actions build and artifact recovery

The existing `.github/workflows/build-apk.yml` builds on pushes to `main` and can also be started manually with `workflow_dispatch`.

1. Open the repository's **Actions** tab and select **Build C@T APK**.
2. Confirm the latest run completed successfully for the intended commit.
3. Download the `cat-apk` artifact from that run and retain it with the commit/release notes as appropriate.
4. If the run failed, inspect its logs and fix the build before distributing an APK.

The workflow creates a downloadable build artifact; it does not deploy to Oracle or publish a production release. The app's current Gradle release configuration uses the debug signing configuration, which is not suitable for a Play Store release. A production signing setup must be explicitly designed and stored securely outside source control.

## Safe change and restore process

1. Work on a Git branch or commit a known-good state before substantial changes.
2. Read `README.md`, this runbook, and the relevant source files.
3. Make one focused change at a time; run the build and relevant tests.
4. Review the diff for secrets, unrelated changes, privacy regressions, and destructive operations.
5. Keep the change and any updated documentation in GitHub. Record the commit and build run for a recovery point.
6. To restore, check out a known-good commit and rerun the documented build. Do not overwrite a live server or user data as part of source recovery.

## Important limits

- Repository instructions make future Copilot work more consistent, but they do not create an always-on autonomous agent or guarantee that Copilot will act without a user invoking it.
- The GitHub Actions workflow is build automation only; it is not continuous monitoring, data backup, or production deployment.
- No Oracle credentials, Grok conversation export, server backup, or deployment validation were available or used when this runbook was added.
