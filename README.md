# Kitabu

Kitabu is a beautiful, responsive notes taking app with local-first storage.

## Features

- Create, edit, pin, and delete notes
- Search across title, content, and tags
- Markdown-like preview mode (`#`, `##`, `###`, lists, bold, italic, inline code)
- Autosaves to browser `localStorage`
- Keyboard shortcuts:
  - `Ctrl/Cmd + N`: new note
  - `Ctrl/Cmd + D`: delete active note

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
