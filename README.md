# Mora Live

Android starter project for a hybrid IDN Live + SHOWROOM companion app.

## Stack
- Kotlin + Jetpack Compose + Material 3
- AndroidX Media3 (native playback foundation)
- AndroidX WebKit (WebView foundation)
- DataStore dependency reserved for persistent preferences

## Current prototype
- Home screen with IDN Live and SHOWROOM entries
- Favorites UI prototype
- Notification center placeholder
- Dark Material 3 UI

## Important integration notes
This starter intentionally does not scrape private endpoints, extract protected stream URLs, bypass DRM, or imitate authenticated sessions. WebView navigation and native playback must be wired to platform-approved URLs/APIs. Online-status notifications require official APIs, webhooks, or another permitted source.

## Build
Open in Android Studio (JDK 17), sync Gradle, then run the `app` configuration.
