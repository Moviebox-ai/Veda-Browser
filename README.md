# Velocity Browser for Android

Velocity Browser is a real, high-speed, private native Android web browser built on top of Android System WebView, Jetpack Compose, Room Database, and Kotlin Coroutines.

## Features & Security Architecture

- **Hardened Security & Privacy**:
  - Cleartext traffic strictly disabled by default (`usesCleartextTraffic = false`) with `NetworkSecurityConfig` trusting system CAs only (user CAs restricted to `debug-overrides`).
  - Strict HTTPS-First escalation upgrading HTTP navigations to HTTPS.
  - Multi-profile isolation for private tabs via AndroidX WebKit `ProfileStore` (`MULTI_PROFILE`), wiping cookies and site storage upon private session termination.
  - Search suggestions suppressed in private tabs to prevent query leakage to network providers.
  - Subresource SSL errors automatically canceled; main-frame SSL errors tracked per-tab with explicit warning dialog.
  - Top-level dangerous schemes (`javascript:`, `file:`, `content:`, `jar:`, `data:`, `blob:`) validated and blocked before tab creation.
  - `allowFileAccess = false`, `allowContentAccess = false`, and `mixedContentMode = MIXED_CONTENT_NEVER_ALLOW`.
  - Media playback requires user gesture (`mediaPlaybackRequiresUserGesture = true`).
  - Origin-scoped site data clearing via `WebStorage.deleteOrigin`.
  - FileProvider constrained to dedicated download directories.

- **Multi-Tab Architecture**:
  - `TabManager` and `TabWebViewManager` with multi-profile and `MutableContextWrapper` lifecycle handling.
  - Separate Standard and Private browsing tab segments.
  - Recently closed tab history stack with restoration.
  - Renderer crash recovery (`onRenderProcessGone`).

- **Smart Address Bar & Engine**:
  - Pure Kotlin `UrlResolver` with scheme validation, domain extraction, and HTTPS-first mode.
  - Real-time debounced search suggestions via OkHttp (standard tabs only).
  - Live connection security badges (Verified HTTPS, Insecure HTTP, SSL Certificate Error).
  - Configurable top or bottom address bar.

- **Storage & Downloads**:
  - Room Database with DAOs for bookmarks, browsing history, shortcuts, and downloads.
  - Single-connection streaming download engine with progress telemetry, MediaScanner integration, and saving to `Download/Velocity` folder in Android storage.
  - Reader Mode on-demand extraction with typography customization.

## Building and Running Tests

```bash
# Compile debug APK
gradle :app:assembleDebug

# Run Unit Tests
gradle :app:testDebugUnitTest
```
