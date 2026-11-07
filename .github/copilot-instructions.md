# Copilot instructions for C@T

These instructions apply to every Copilot-assisted change in this repository.

## Project goal

Maintain and improve the existing C@T Android app in this repository. Read `README.md` and `docs/CAT_BUILD_AND_RECOVERY.md` before making substantial changes. Keep the app Android-native and preserve the documented product behavior unless the user explicitly asks for a change.

## Work expectations

- Make focused, testable changes in this repository; explain what changed, what was tested, and any remaining manual steps.
- Build with the checked-in Gradle wrapper and the versions/configuration already used by the project. Do not upgrade dependencies or Android SDK targets as an incidental change.
- Do not claim a build, deployment, backup, or verification succeeded unless it actually did.
- Keep durable project decisions, setup steps, recovery steps, and known limitations in version control, especially the runbook.
- When implementation details are uncertain, inspect the current code and ask a focused question rather than inventing access or requirements.

## Privacy, security, and destructive actions

- Never ask users to paste passwords, API keys, signing keys, SSH keys, or server credentials into chat or commit them. Use documented secret stores and least-privilege access if a future, explicitly authorized workflow requires secrets.
- Do not scrape browser history, other apps, local terminals, devices, accounts, or private conversations. Only use conversation exports or data explicitly provided by the user and authorized for this project; minimize and redact sensitive data.
- Do not delete, overwrite, migrate, or replace another app, deployment, database, or user data without explicit identification, a verified backup, a rollback plan, and a separate confirmation immediately before the destructive step.
- Do not autonomously access or change the Oracle server or any live deployment. Treat deployment as a separately authorized, human-reviewed operation; do not put production credentials in this repository.
- Keep C@T's documented privacy guarantees and user-controlled offline/cloud behavior intact. Do not add hidden collection, background scanning, self-modifying updates, or unapproved data transmission.
- Keep security-related features defensive and limited to systems the user owns or is explicitly authorized to administer. Do not add exploit, intrusion, credential theft, persistence, or stealth functionality.

## Automation boundaries

GitHub Actions may build and publish a build artifact as defined by the checked-in workflow. CI is not authorization to deploy to production or delete anything. Require a human-reviewed, explicit request for live changes. Do not create unbounded loops, hidden background agents, or claims of an always-on Copilot; Copilot works when invoked and within the repository's configured GitHub features and permissions.
