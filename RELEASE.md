# Kitabu APK Release Runbook

## Prerequisites

- Code is pushed to your GitHub repository default branch (`main`).
- Workflows exist:
  - `.github/workflows/android-apk.yml`
  - `.github/workflows/android-release.yml`

## Fast release from GitHub UI

1. Open your repo on GitHub.
2. Go to `Actions`.
3. Open workflow `Build And Release APK`.
4. Click `Run workflow`.
5. Enter:
   - `tag`: example `v1.0.7`
   - `name`: optional (or leave blank)
   - `prerelease`: set as needed
6. Wait for run to finish.
7. Open `Releases` and download `app-release.apk`.

## APK artifact only (no GitHub Release)

1. Open workflow `Build Android APK`.
2. Click `Run workflow`.
3. After completion, download artifact `kitabu-release-apk`.

## Notes

- The workflows run `flutter create . --platforms=android` automatically, so an `android/` folder is generated during CI if missing.
- Java 17 + Flutter stable are set in CI.
