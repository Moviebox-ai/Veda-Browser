# Velocity Browser for Android

Velocity Browser is a real, high-speed, private native Android web browser built on top of Android System WebView, Jetpack Compose, Room Database, and Kotlin Coroutines.

## Features

- **Hardened Security & Privacy**:
  - Cleartext traffic disabled by default with strict `NetworkSecurityConfig`.
  - SSL errors always intercepted and canceled (`handler.cancel()`) with a dedicated security warning interstitial.
  - Safe Browsing enabled with threat mitigation.
  - Tracking and ad request blocker in `shouldInterceptRequest`.
  - `allowFileAccess = false`, `allowContentAccess = false`, and `allowUniversalAccessFromFileURLs = false` to prevent file exfiltration.
  - Safe Intent URI parser clearing component/selector and stripping `FLAG_GRANT_*` permission flags.

- **Multi-Tab Architecture**:
  - `TabManager` and `TabWebViewManager` with tab isolation.
  - Grid tab switcher with separate Standard vs Private segments.
  - Recently closed tab history stack with instant restoration.
  - Graceful renderer crash recovery (`onRenderProcessGone`).

- **Smart Address Bar & Engine**:
  - Pure Kotlin `UrlResolver` with scheme validation, domain extraction, and HTTPS-first escalation.
  - Real-time debounced search suggestions via OkHttp.
  - Live connection security badges (Verified HTTPS, Insecure HTTP, SSL Certificate Error).
  - Configurable top or bottom address bar.

- **Storage & Offline First**:
  - Room Database with DAOs for bookmarks, browsing history, shortcuts, and downloads.
  - Real-time `NetworkMonitor` with offline banners.
  - Download Manager with progress tracking and secure `FileProvider` launch.
  - Reader Mode on-demand extraction with typography customization.

## Building and Running Tests

```bash
# Compile debug APK
gradle :app:assembleDebug

# Run Unit Tests
gradle :app:testDebugUnitTest
```
