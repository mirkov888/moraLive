# Mora Live

Mora Live is an Android companion app for accessing IDN Live and SHOWROOM from one interface.

## Current implementation
- Kotlin + Jetpack Compose + Material 3 dark UI.
- Home screen with official platform entry points.
- WebView-based hybrid fallback for both platform websites.
- Basic favorites UI prototype.
- Notification-center placeholder.
- AndroidX Media3 dependencies reserved for approved native playback.
- GitHub Actions workflow to assemble and publish a debug APK artifact.

## Build locally
Requirements: Android Studio with JDK 17 and Android SDK 35.

```bash
gradle --no-daemon assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Platform integration and safety
The current player opens official platform websites in WebView. Native playback requires a platform-approved stream/API and will only be enabled when that interface is available. Online-status notifications likewise require an official API, webhook, or supported notification feed.

This project does not scrape private endpoints, extract protected stream URLs, bypass DRM, or imitate authenticated sessions.

## CI
Pushes and pull requests to `main` run the Android debug build. The generated APK is uploaded as the `mora-live-debug-apk` workflow artifact.
