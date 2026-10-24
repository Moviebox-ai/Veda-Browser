package com.aaryo.vedabrowser.model

data class BrowserSettings(
    val defaultSearchEngine: SearchEngine = SearchEngine.GOOGLE,
    val adBlockGlobalEnabled: Boolean = true,
    val adBlockStrictMode: Boolean = false,
    val easyListEnabled: Boolean = true,
    val easyPrivacyEnabled: Boolean = true,
    val uBlockFiltersEnabled: Boolean = true,
    val indianFiltersEnabled: Boolean = true,
    val filterListsLastUpdated: Long = 0L,
    val allowlistedDomains: Set<String> = emptySet(),
    val doNotTrackEnabled: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val httpsOnlyMode: Boolean = true,
    val safeBrowsingEnabled: Boolean = true,
    val antiFingerprintingEnabled: Boolean = true,
    val desktopSiteDefault: Boolean = false,
    val clearDataOnExit: Boolean = false,
    val forceDarkWebContent: Boolean = false,
    val homepageUrl: String = "velocity://newtab",
    val textZoomPercent: Int = 100,
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val accentColorHex: String = "#14B8A6", // Teal matching Screen 10 mockup
    val wallpaperTheme: String = "MOUNTAINS", // "MOUNTAINS", "FOREST", "CYBER", "MINIMAL"
    val toolbarStyle: String = "DEFAULT", // "DEFAULT", "COMPACT", "BOTTOM"
    val cornerRadiusDp: Int = 16,
    val activeWorkspace: String = "Personal", // "Personal", "Work", "Study", "Entertainment"
    val openLinksInNewTab: Boolean = true
)

data class ShieldStats(
    val totalAdsBlocked: Long = 0,
    val totalTrackersBlocked: Long = 0,
    val totalBandwidthSavedMb: Float = 0f,
    val totalTimeSavedSeconds: Float = 0f
)
