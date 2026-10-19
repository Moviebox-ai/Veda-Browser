package com.aaryo.velocity.model

data class BrowserSettings(
    val defaultSearchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val adBlockGlobalEnabled: Boolean = true,
    val doNotTrackEnabled: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val desktopSiteDefault: Boolean = false,
    val clearDataOnExit: Boolean = false,
    val forceDarkWebContent: Boolean = false,
    val homepageUrl: String = "velocity://newtab",
    val textZoomPercent: Int = 100,
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val accentColorHex: String = "#2563EB", // Electric Blue
    val wallpaperTheme: String = "MOUNTAINS", // "MOUNTAINS", "FOREST", "CYBER", "MINIMAL"
    val toolbarStyle: String = "DEFAULT", // "DEFAULT", "COMPACT", "BOTTOM"
    val cornerRadiusDp: Int = 16,
    val activeWorkspace: String = "Personal" // "Personal", "Work", "Study", "Entertainment"
)

data class ShieldStats(
    val totalAdsBlocked: Long = 0,
    val totalTrackersBlocked: Long = 0,
    val totalBandwidthSavedMb: Float = 0f,
    val totalTimeSavedSeconds: Float = 0f
)
