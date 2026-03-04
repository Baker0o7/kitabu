# Kitabu

Kitabu is a beautiful, responsive notes taking app with local-first storage.

## Features

- Create, edit, pin, and delete notes
- Archive and restore notes
- Search across title, content, and tags
- Markdown-like preview mode (`#`, `##`, `###`, lists, bold, italic, inline code)
- Autosaves to browser `localStorage`
- Theme switcher (`Sunrise` / `Midnight`) with persistence
- Export notes to JSON and import backups
- Keyboard shortcuts:
  - `Primary + N`: new note (`Cmd` on Mac/iOS, `Ctrl` elsewhere)
  - `Primary + D`: delete active note
  - `Primary + Shift + A`: archive/restore active note

Import behavior:

- Import merges by note id.
- If both local and imported note exist, the newer `updatedAt` version wins.
- Very large import files are rejected for safety.

## Run

Open [`index.html`](./index.html) in a browser.

For a local server, from this folder run:

```bash
python3 -m http.server 8080
```

Then open `http://localhost:8080`.

## Android APK

This project uses Capacitor for Android packaging.

Local commands:

```bash
npm install
npm run cap:sync
npm run apk:debug
```

GitHub Actions workflow:

- `.github/workflows/android-apk.yml` builds `app-debug.apk` on pushes to `main` and on manual dispatch.
- `.github/workflows/android-release.yml` builds APK and publishes a GitHub Release with `app-debug.apk` attached.

### Publish Release APK

1. GitHub Actions -> `Build And Release APK` -> `Run workflow`.
2. Set a tag like `v1.0.0`.
3. The workflow creates a Release and uploads the APK asset automatically.
