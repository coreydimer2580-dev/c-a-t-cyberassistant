# C@T deployment notes

These notes record deployment details supplied by the project owner in conversation. They have not been independently verified against the Oracle tenancy, Google Drive, Replit, or the live server. Treat them as a planning snapshot, not as proof of current server state.

## App endpoint

- Address: https://161-33-236-210.sslip.io
- The owner reports that Replit can open and read the running app at this address, but cannot update the server because no server login is available in the notes they reviewed.

## Reported Oracle host

- Service: Oracle Cloud Always Free
- Shape: `VM.Standard.E2.1.Micro`
- Region: `ap-sydney-1`
- Memory: approximately 954 MB
- Boot disk: approximately 47 GB
- Data disk: approximately 150 GB, mounted at `/mnt/catdata`

All capacities and configuration above are owner-reported and may change; verify them through an authorized Oracle account before planning or making changes.

## Reported model/provider plan

The notes reportedly name this preferred order for the app's AI backends:

1. Gemini
2. Groq
3. A small Ollama model running on the Oracle server

This is a recorded plan, not confirmation that each provider or model is configured or working in production.

## Access and secrets status

- No xAI API key was found in the notes the owner reviewed.
- No Oracle login, password, or server access credential was available in those notes.
- Do not invent credentials, assume access, or commit secrets to this repository.
- If an xAI key is needed for a Replit project, the owner should create it in their xAI account and add it directly to Replit's secret settings. Never paste the key into chat or source control.
- Obtain Oracle access through the owner's normal secure account process. Do not request private keys or passwords in a public issue or commit them to GitHub.

## Safe next steps

1. Keep the existing app and data unchanged until the target deployment and any duplicate app are positively identified.
2. Before an authorized server change, verify the host and mounted data disk, make and validate a restorable backup, and write down a rollback plan.
3. Review the current app implementation and deployment configuration before choosing a backend or assuming the reported provider order is already wired up.
4. Require the owner to authorize any production deployment or deletion separately. Repository build automation does not grant server access or authorize production changes.

See `CAT_BUILD_AND_RECOVERY.md` for the Android build and repository recovery process.
