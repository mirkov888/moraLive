# Mora Live

Mora Live is an Android companion app for accessing IDN Live and SHOWROOM from one interface.

## Current version: 0.2.0-dev

- Kotlin + Jetpack Compose + Material 3 dark interface.
- Redesigned home dashboard with platform search.
- Persistent platform favorites backed by Preferences DataStore.
- Favorites can be opened or removed from the favorites screen.
- Hybrid WebView with page progress, reload, main-frame error state, and Android back navigation.
- WebView hardening: file/content access disabled and mixed content blocked.
- Notification center explains the current official-integration requirement.
- AndroidX Media3 dependencies reserved for approved native playback.

## Build locally

Requirements: Android Studio with JDK 17 and Android SDK 35.

```bash
gradle --no-daemon assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Platform integration and safety

The player opens official platform websites in WebView. Native playback requires a platform-approved stream/API and will only be enabled when that interface is available. Online-status notifications likewise require an official API, webhook, or supported notification feed.

This project does not scrape private endpoints, extract protected stream URLs, bypass DRM, or imitate authenticated sessions.

## CI

Pushes and pull requests to `main` run the Android debug build. The generated APK is uploaded as the `mora-live-debug-apk` workflow artifact.
