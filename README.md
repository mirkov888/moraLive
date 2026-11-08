# Mora Live

Mora Live is a native-first Android hub for discovering and accessing live-streaming platforms.

## Current version: v0.3.2 — Native platform discovery

### Implemented foundation
- Kotlin + Jetpack Compose + Material 3 dark interface.
- Persistent platform favorites using Preferences DataStore.
- Native platform discovery and platform detail screens; official sites open only after the user taps the watch action.
- Platform search with category filters (current categories classify platform entries, not live streamer metadata).
- Persistent on-device platform watch history.
- Hybrid WebView with progress, reload, error state, and Android back navigation.
- WebView hardening and mobile-browser user-agent compatibility adjustments.
- Dedicated Account Center with official website login links for IDN Live, SHOWROOM, Twitch, and Kick. This is browser-based sign-in only; OAuth token connection and in-app account status are not yet active.
- AndroidX Media3 dependencies are present; native playback is not yet connected to any platform stream.

### Wots48-inspired product direction (Mora Live 1–5)
1. **Discover & streamer directory** — native streamer profiles, search, categories, platform labels, and verified LIVE status. Requires official feeds/API or approved data sources.
2. **Schedule & notifications** — streamer schedules, local reminders, and official live-status notifications. Local reminders and platform status sync are planned for v0.4.0.
3. **Multi-viewer** — multi-stream layout where platform terms and stream delivery permit it; not yet implemented.
4. **Oshi & favorites** — persistent favorite platforms are implemented; streamer-level Oshi lists, personal ranking, and categories are planned.
5. **News & updates** — official announcements, event updates, and platform news feed; awaiting supported feeds/API.

## Roadmap

### v0.2.0 — Foundation
- [x] Persistent platform favorites
- [x] Navigation and WebView stability improvements
- [x] Redesigned dashboard UI
- [x] Connection/error handling

### v0.3.0 — Live Experience
- [x] Platform search
- [x] Platform category filters
- [x] Platform-level watch history
- [ ] Streamer search and verified streamer catalog
- [ ] Streamer-level categories and profiles
- [ ] Fullscreen and PiP where supported
- [ ] Native Media3 playback for officially accessible streams

### v0.4.0 — Notification
- [ ] Notifications when favorite streamers go live (official source required)
- [ ] Local schedule reminders
- [ ] Live-status synchronization from official sources

### v1.0.0 — Stable
- [ ] Physical-device testing across supported Android versions
- [ ] Performance and battery optimization
- [ ] Crash reporting and resilient error handling
- [ ] Signed release APK with a persistent signing key

### v0.3.2 — Native Discovery
- [x] Native platform detail screen before opening external streaming sites
- [x] Keep official WebView behind an explicit watch action
- [x] Clear empty state explaining that verified streamer directory data is not yet connected

### v0.3.1 — Account Center
- [x] Dedicated Account Center navigation and native provider cards
- [x] Open official platform login pages for IDN Live, SHOWROOM, Twitch, and Kick
- [ ] OAuth callback and secure token storage
- [ ] Connected account profile, disconnect, and multi-account management
- [ ] Chat and gift integrations where official APIs permit

## Build locally

Requirements: Android Studio, JDK 17, and Android SDK 35.

```bash
gradle --no-daemon assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Platform integration and safety

Login, chat, gifts, streamer catalogs, live status, news, and native playback must use official APIs, documented OAuth flows, approved feeds, or platform-permitted interfaces. Mora Live must not collect platform passwords, scrape private endpoints, extract protected stream URLs, bypass DRM, or imitate authenticated sessions.

The current Account Center opens official login pages in the browser; it does not yet receive OAuth callbacks or mark accounts as connected. The current watch history records platform visits, not individual streamer/video playback.
