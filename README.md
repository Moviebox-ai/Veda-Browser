# Velocity Browser

**Velocity** is a fast, secure, private native Android web browser built with Kotlin and Jetpack Compose. Engineered for performance and privacy, Velocity features a hardware-accelerated ad & tracker blocking engine (Velocity Shield), multi-tab management, isolated incognito browsing, bookmarks, and an integrated download manager.

---

## Key Features

- **Multi-Tab Architecture & Switcher**:
  - Seamlessly open, switch, and close multiple tabs.
  - Dedicated private (Incognito) tab mode with isolated sessions, no browsing history logging, and zero cookie persistence.
  - Interactive tab switcher grid with live previews and badge counters.

- **Supersonic Browsing Engine**:
  - Powered by Android's native `WebView` with hardware acceleration.
  - Full navigation controls: Back, Forward, Reload, and Home.
  - Real-time animated page loading progress bar.
  - SSL/TLS security indicators (padlock badge and detailed certificate info modal).
  - Desktop Site mode toggle on demand or by default.

- **Velocity Shield (Hardened Privacy & Content Protection)**:
  - Built-in ad and telemetry tracker blocker that intercepts invasive network requests.
  - Transmits "Do Not Track" (DNT) header with page visits.
  - Real-time privacy metrics: ads/trackers blocked, bandwidth saved, and estimated time saved.
  - One-tap Clear Browsing Data tool (cache, cookies, history).

- **Smart Omnibox & Search Engine Integration**:
  - Unified address and search bar supporting direct URL input and query dispatch.
  - Configurable search providers: DuckDuckGo (default), Google, Bing, Brave Search, and Ecosia.

- **Bookmarks & Speed Dial**:
  - Speed dial favorites on the supersonic start page for instant navigation.
  - Bookmark manager with add, search, and delete capabilities.

- **Integrated Download Manager**:
  - Captures file downloads directly from web pages.
  - Real-time download progress tracking, status indicators, and history management.

- **In-Page Tools**:
  - Find in Page with forward/backward match jumping.
  - Native Android share sheet integration.

---

## Tech Stack & Architecture

- **Language:** Kotlin 2.2
- **UI Framework:** Jetpack Compose (Material 3 Design System)
- **Architecture:** Clean Architecture / MVVM with StateFlow
- **Web Engine:** Android `WebView`, `WebChromeClient`, `WebViewClient`
- **Data Persistence:** Local JSON/SharedPreferences storage with Gson
- **Build System:** Gradle (Kotlin DSL), Android SDK 36 (AGP 9.1)
