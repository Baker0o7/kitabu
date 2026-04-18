# Kitabu - React Native Notes App

A beautiful, feature-rich notes app built with React Native and Expo. Kitabu offers a clean, minimalist interface for organizing your thoughts, ideas, and daily notes.

![Kitabu Screenshot](assets/icon.png)

## Features

- **Markdown Editor** - Rich text editing with live preview
- **Templates** - 8 built-in templates for quick note creation
- **Tags & Folders** - Organize notes with tags and nested folders
- **Dark Theme** - Beautiful dark UI with customizable accent colors
- **Search** - Full-text search across all notes
- **Export/Import** - Backup and restore your data
- **Multiple Views** - Grid, List, and Compact view modes
- **Favorites & Archive** - Pin important notes, archive old ones

## Quick Start

```bash
# Clone the repository
git clone https://github.com/Baker0o7/KITABU.git
cd KITABU

# Install dependencies
npm install

# Start the development server
npx expo start
```

## Automatic APK Builds

This repository includes GitHub Actions workflows for automatic APK building.

### Setup Instructions

1. **Create an Expo Account**
   - Go to [expo.dev](https://expo.dev) and create a free account

2. **Generate an Expo Token**
   - Go to [expo.dev/settings/access-tokens](https://expo.dev/settings/access-tokens)
   - Click "Create Token"
   - Copy the token

3. **Add Token to GitHub Secrets**
   - Go to your GitHub repository
   - Navigate to **Settings > Secrets and variables > Actions**
   - Click "New repository secret"
   - Name: `EXPO_TOKEN`
   - Value: Your Expo token from step 2
   - Click "Add secret"

### Build Workflows

#### 1. Automatic Build on Push
Every push to `master` or `main` branch triggers an automatic APK build.

#### 2. Manual Build
Go to **Actions > Release APK > Run workflow** to manually trigger a build.

#### 3. Build Types
- **Preview** - Debug APK for testing (faster build)
- **Production** - Signed APK for release

### Downloading Your APK

After the build completes:

1. Check the GitHub Actions logs for the build URL
2. Or go to [expo.dev](https://expo.dev) and navigate to your project builds
3. Download the APK directly to your device

## Building Locally

### Option 1: EAS Build (Recommended)

```bash
# Install EAS CLI
npm install -g eas-cli

# Login to Expo
eas login

# Build Preview APK
eas build --platform android --profile preview

# Build Production APK
eas build --platform android --profile production
```

### Option 2: Local Gradle Build

```bash
# Prebuild the Android project
npx expo prebuild --platform android

# Build with Gradle
cd android
./gradlew assembleRelease

# APK location:
# android/app/build/outputs/apk/release/app-release.apk
```

## Project Structure

```
KITABU/
├── src/
│   ├── components/     # Reusable UI components
│   ├── screens/        # App screens
│   ├── navigation/     # Navigation configuration
│   ├── store/          # State management (Zustand)
│   ├── lib/            # Utilities and storage
│   ├── theme/          # Colors and theming
│   └── types/          # TypeScript types
├── assets/             # Images and icons
├── .github/
│   └── workflows/      # GitHub Actions
└── package.json
```

## Technologies

- React Native
- Expo SDK 52
- TypeScript
- Zustand (State Management)
- React Navigation
- React Native Markdown Display
- AsyncStorage

## License

MIT License - feel free to use this project for personal or commercial purposes.

---

Made with ❤️ by Kitabu
