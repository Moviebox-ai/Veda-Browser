# Velocity Browser

**Velocity** is a fast, secure, private native Android web browser built with Kotlin and Jetpack Compose (Material 3).

---

## Architecture & Features

- **Multi-Tab Architecture & Switcher (`TabWebViewManager`)**:
  - One `WebView` instance per tab owned by `TabWebViewManager` in the ViewModel/Application scope.
  - Seamless attach/detach lifecycle with state restoration (`saveState`/`restoreState`).
  - Real-time thumbnail capture (downscaled canvas snapshot) displayed in the Tab Switcher grid.
  - Non-incognito open tabs persisted across app sessions.

- **Incognito Privacy & Multi-Profile Isolation**:
  - Leverages Android `androidx.webkit` Multi-Profile API (`WebViewFeature.MULTI_PROFILE`) using an isolated `"incognito"` profile when supported by the device WebView engine.
  - On platforms where multi-profile is unsupported, automatically flushes and clears all cookies, cache, and session web storage when the last incognito tab is closed.
  - Zero browsing history recording in private tabs.

- **Hardened Velocity Shield (Ad & Tracker Blocker)**:
  - Bundled EasyList and EasyPrivacy curated filter rules (`assets/easylist_subset.txt`) loaded and parsed on a background thread (`Dispatchers.IO`).
  - Safe default rule set protecting critical identity/login domains (`facebook.net`, `branch.io`, `segment.com`, etc.) to prevent broken sign-ins, with an optional strict mode.
  - Per-site allowlist support to toggle shields for individual sites.
  - Automatic `DNT: 1` ("Do Not Track") header and JavaScript runtime injection.

- **Robust Web Standards & Security**:
  - `shouldOverrideUrlLoading` validated via `SchemePolicy` and `IntentValidator` to route external schemes (`tel:`, `mailto:`, `sms:`, `geo:`, `intent:`) safely without component redirection vulnerabilities.
  - `onCreateWindow` opens popups and `target="_blank"` links in fresh tabs.
  - Full-screen HTML5 video playback via `onShowCustomView` / `onHideCustomView`.
  - Android Photo/File chooser via zero-permission contracts (`onShowFileChooser`).
  - Web runtime permissions (`CAMERA`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`) with Compose prompts.
  - Custom offline/error page handling connection failures.
  - Strict SSL error interception (`SslWarningDialog`), canceling connections by default.
  - Real X.509 SSL certificate inspection extracted directly from `SslCertificate`.

- **Real Download Manager**:
  - Native integration with Android `DownloadManager` and `MediaStore.Downloads`.
  - Real-time progress monitoring, system notifications, and tap-to-open actions via `Intent.ACTION_VIEW`.
  - Filename sanitization protecting against directory traversal and illegal characters.

- **Modern Asynchronous Storage**:
  - Powered by Jetpack `DataStore Preferences` with non-blocking I/O on `Dispatchers.IO`.
  - Debounced write operations.
  - Browsing history automatically capped at 5,000 items.

---

## Tech Stack

- **Package Name:** `com.aaryo.velocity`
- **Language:** Kotlin 2.2
- **UI Framework:** Jetpack Compose (Material 3)
- **Web Engine:** Android `WebView` & `androidx.webkit` 1.12.1
- **Persistence:** Jetpack DataStore Preferences 1.1.7
- **Target SDK:** Android 36 (minSdk 26)
- **Build System:** Gradle KTS, AGP 9.1.1, JDK 21
