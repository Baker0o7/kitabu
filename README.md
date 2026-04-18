# Kitabu - Notes App

A beautiful React Native notes app with markdown support, templates, and more.

## 🚀 Quick Start

```bash
git clone https://github.com/Baker0o7/KITABU.git
cd KITABU
npm install
npx expo start
```

## 📱 Automatic APK Builds

### Setup (One-time)

1. **Create Expo Account**
   - Go to [expo.dev/signup](https://expo.dev/signup)

2. **Get Access Token**
   - Visit [expo.dev/settings/access-tokens](https://expo.dev/settings/access-tokens)
   - Click "Create" → Copy token

3. **Add to GitHub**
   - Go to `Settings → Secrets and variables → Actions`
   - Click "New repository secret"
   - Name: `EXPO_TOKEN`
   - Value: Your copied token

### Build Triggers

| Action | Result |
|--------|--------|
| Push to `master` | ⚡ Auto builds APK |
| Push tag `v1.0.0` | 🎯 Creates release |
| Manual trigger | 🔧 Choose build type |

### Download APK

After build completes (~10 min):

1. **Email**: Check your inbox for Expo download link
2. **Dashboard**: Visit [expo.dev](https://expo.dev) → Projects → Kitabu → Builds

## 📝 Manual Build

```bash
# Install EAS CLI
npm install -g eas-cli

# Login
eas login

# Build
npx eas build --platform android --profile preview
```

## 📁 Workflows

- **EAS Build** (`.github/workflows/eas-build.yml`) - Main build workflow
- **Build APK** (`.github/workflows/build-apk.yml`) - Detailed build with notifications  
- **Release** (`.github/workflows/release-apk.yml`) - Tag-based releases

## 📝 Features

- 📝 Markdown editor with preview
- 📄 8 built-in templates
- 🏷️ Tags & folders
- 🌙 Dark theme
- 🔍 Full-text search
- 💾 Export/Import
- ⭐ Favorites & Archive

## 📞 Tech Stack

- React Native + Expo SDK 52
- TypeScript
- Zustand (state)
- React Navigation
- Markdown renderer

## 📄 License

MIT

---
Made with ❤️ by Kitabu
