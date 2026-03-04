# Kitabu (Flutter)

Kitabu is a Flutter notes app with local persistence and Android APK release automation.

## Features

- Create, edit, pin, archive, restore, and delete notes
- Duplicate and share individual notes
- Search by title, tags, and body
- Sort by updated time, created time, or title
- Favorites filter and pin filter
- Markdown preview mode
- Theme toggle (light/dark)
- JSON import and export
- Workspace stats (counts, words, reading time)
- Local persistence with `SharedPreferences`

## Run (local)

Prerequisites:

- Flutter SDK (stable)
- Android SDK (for emulator/device builds)

Commands:

```bash
flutter pub get
flutter run
```

## Build APK (local)

```bash
flutter create . --platforms=android --project-name kitabu --org com.kitabu
flutter pub get
flutter build apk --release
```

APK output:

`build/app/outputs/flutter-apk/app-release.apk`

## CI / Releases

- `.github/workflows/android-apk.yml`
  Builds release APK on push to `main` and manual dispatch.
- `.github/workflows/android-release.yml`
  Builds APK and publishes GitHub Release with attached `app-release.apk`.

Manual release:

1. GitHub Actions -> `Build And Release APK` -> `Run workflow`
2. Enter tag like `v1.0.6`
3. Workflow publishes release + APK asset

Detailed runbook: `RELEASE.md`
