package com.aaryo.vedabrowser.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaryo.vedabrowser.data.BrowserRepository
import com.aaryo.vedabrowser.data.ExtensionRepository
import com.aaryo.vedabrowser.download.RealDownloadManager
import com.aaryo.vedabrowser.extension.BuiltInExtensions
import com.aaryo.vedabrowser.extension.ExtensionInjector
import com.aaryo.vedabrowser.extension.GmBridge
import com.aaryo.vedabrowser.extension.UserscriptParser
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.aaryo.vedabrowser.R
import com.aaryo.vedabrowser.model.Bookmark
import com.aaryo.vedabrowser.model.BrowserSettings
import com.aaryo.vedabrowser.model.DownloadItem
import com.aaryo.vedabrowser.model.Extension
import com.aaryo.vedabrowser.model.HistoryItem
import com.aaryo.vedabrowser.model.QuickDialShortcut
import com.aaryo.vedabrowser.model.SavedCredential
import com.aaryo.vedabrowser.model.ShieldStats
import com.aaryo.vedabrowser.model.Tab
import com.aaryo.vedabrowser.privacy.AdBlocker
import com.aaryo.vedabrowser.privacy.FilterListUpdateWorker
import com.aaryo.vedabrowser.security.CertificateInfo
import java.net.URLEncoder
import com.aaryo.vedabrowser.ui.dialogs.SslWarningState
import com.aaryo.vedabrowser.ui.dialogs.WebPermissionState
import com.aaryo.vedabrowser.webview.TabWebViewManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

enum class SheetType {
    NONE,
    TAB_SWITCHER,
    BOOKMARKS,
    HISTORY,
    DOWNLOADS,
    SETTINGS,
    SHIELD_INFO,
    SSL_CERTIFICATE,
    CLEAR_DATA,
    ALLOWLIST_MANAGER,
    EXTENSIONS,
    EXTENSION_DETAIL,
    MENU,
    AI_ASSISTANT,
    APPEARANCE,
    PASSWORDS
}

data class CustomViewState(
    val view: View,
    val callback: WebChromeClient.CustomViewCallback
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    val repository = BrowserRepository(application)
    val tabWebViewManager = TabWebViewManager()
    val realDownloadManager = RealDownloadManager(application)

    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _activeSheet = MutableStateFlow<SheetType>(SheetType.NONE)
    val activeSheet: StateFlow<SheetType> = _activeSheet.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _settings = MutableStateFlow<BrowserSettings>(BrowserSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    private val _shieldStats = MutableStateFlow<ShieldStats>(ShieldStats())
    val shieldStats: StateFlow<ShieldStats> = _shieldStats.asStateFlow()

    private val _isUpdatingFilterLists = MutableStateFlow(false)
    val isUpdatingFilterLists: StateFlow<Boolean> = _isUpdatingFilterLists.asStateFlow()

    // Dialog & overlay states
    private val _sslWarningState = MutableStateFlow<SslWarningState?>(null)
    val sslWarningState: StateFlow<SslWarningState?> = _sslWarningState.asStateFlow()

    private val _currentCertificateInfo = MutableStateFlow<CertificateInfo?>(null)
    val currentCertificateInfo: StateFlow<CertificateInfo?> = _currentCertificateInfo.asStateFlow()

    private val _webPermissionState = MutableStateFlow<WebPermissionState?>(null)
    val webPermissionState: StateFlow<WebPermissionState?> = _webPermissionState.asStateFlow()

    private val _customViewState = MutableStateFlow<CustomViewState?>(null)
    val customViewState: StateFlow<CustomViewState?> = _customViewState.asStateFlow()

    // Find in page state
    private val _isFindInPageVisible = MutableStateFlow(false)
    val isFindInPageVisible: StateFlow<Boolean> = _isFindInPageVisible.asStateFlow()

    private val _findInPageQuery = MutableStateFlow("")
    val findInPageQuery: StateFlow<String> = _findInPageQuery.asStateFlow()

    // Extension & userscript states
    val extensionRepository = ExtensionRepository(application)

    private val _credentials = MutableStateFlow<List<SavedCredential>>(emptyList())
    val credentials: StateFlow<List<SavedCredential>> = _credentials.asStateFlow()

    private val _quickDials = MutableStateFlow<List<QuickDialShortcut>>(emptyList())
    val quickDials: StateFlow<List<QuickDialShortcut>> = _quickDials.asStateFlow()

    private val _extensions = MutableStateFlow<List<Extension>>(emptyList())
    val extensions: StateFlow<List<Extension>> = _extensions.asStateFlow()

    private val _extensionsGlobalEnabled = MutableStateFlow(true)
    val extensionsGlobalEnabled: StateFlow<Boolean> = _extensionsGlobalEnabled.asStateFlow()

    private val _selectedExtensionForDetail = MutableStateFlow<Extension?>(null)
    val selectedExtensionForDetail: StateFlow<Extension?> = _selectedExtensionForDetail.asStateFlow()

    private val _pendingInstallScript = MutableStateFlow<Extension?>(null)
    val pendingInstallScript: StateFlow<Extension?> = _pendingInstallScript.asStateFlow()

    val gmBridge: GmBridge by lazy {
        GmBridge(
            context = application,
            onOpenInTab = { url -> createNewTab(url) },
            onGetStorage = { extId, key -> extensionRepository.getGmValue(extId, key) },
            onSetStorage = { extId, key, value -> extensionRepository.setGmValue(extId, key, value) },
            onDeleteStorage = { extId, key -> extensionRepository.deleteGmValue(extId, key) },
            getExtensionById = { id -> _extensions.value.find { it.id == id } }
        )
    }

    // Toast/Snackbar notifications
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // Schedule weekly automatic background filter list updates
        FilterListUpdateWorker.scheduleWeeklyUpdate(application)

        // Observe flows from repository
        viewModelScope.launch {
            repository.bookmarksFlow.collect { _bookmarks.value = it }
        }
        viewModelScope.launch {
            repository.historyFlow.collect { _history.value = it }
        }
        viewModelScope.launch {
            repository.downloadsFlow.collect { _downloads.value = it }
        }
        viewModelScope.launch {
            repository.settingsFlow.collect {
                val oldGlobal = _settings.value.adBlockGlobalEnabled
                val oldStrict = _settings.value.adBlockStrictMode
                _settings.value = it

                AdBlocker.isStrictMode = it.adBlockStrictMode
                AdBlocker.syncAllowlist(it.allowlistedDomains)
                tabWebViewManager.applySettingsToAll(it)

                if (oldGlobal != it.adBlockGlobalEnabled || oldStrict != it.adBlockStrictMode) {
                    _tabs.value = _tabs.value.map { tab ->
                        val domain = AdBlocker.normalizeDomain(tab.url)
                        tab.copy(adBlockEnabled = !AdBlocker.isAllowlisted(domain) && it.adBlockGlobalEnabled)
                    }
                    repository.requestSaveTabs(_tabs.value)
                }
            }
        }
        viewModelScope.launch {
            repository.shieldStatsFlow.collect { _shieldStats.value = it }
        }
        viewModelScope.launch {
            repository.credentialsFlow.collect { _credentials.value = it }
        }
        viewModelScope.launch {
            repository.quickDialsFlow.collect { _quickDials.value = it }
        }
        viewModelScope.launch {
            extensionRepository.extensionsFlow.collect { list ->
                _extensions.value = list
                refreshActiveTabExtensionsCount()
            }
        }
        viewModelScope.launch {
            extensionRepository.globalEnabledFlow.collect { enabled ->
                _extensionsGlobalEnabled.value = enabled
                refreshActiveTabExtensionsCount()
            }
        }

        // Initialize bundled AdBlocker list in background with settings
        viewModelScope.launch {
            val initialSettings = repository.settingsFlow.first()
            AdBlocker.initialize(application, initialSettings)
        }

        // Restore open tabs from disk on launch
        viewModelScope.launch {
            val initialSettings = repository.settingsFlow.first()
            val persistedTabs = repository.getInitialTabs()
            if (persistedTabs.isNotEmpty()) {
                val filtered = persistedTabs.filterNot {
                    it.id in listOf("tab_google", "tab_youtube", "tab_news", "tab_openai")
                }
                val restored = if (filtered.isEmpty()) {
                    listOf(
                        Tab(
                            id = "tab_home",
                            title = "New Tab",
                            url = "velocity://newtab",
                            adBlockEnabled = initialSettings.adBlockGlobalEnabled,
                            isDesktopMode = initialSettings.desktopSiteDefault
                        )
                    )
                } else {
                    filtered.map {
                        val domain = AdBlocker.normalizeDomain(it.url)
                        Tab(
                            id = it.id,
                            title = it.title,
                            url = it.url,
                            adBlockEnabled = !AdBlocker.isAllowlisted(domain) && initialSettings.adBlockGlobalEnabled,
                            isDesktopMode = initialSettings.desktopSiteDefault
                        )
                    }
                }
                _tabs.value = restored
                val homeTab = restored.find { it.isNewTab }
                _activeTabId.value = homeTab?.id ?: restored.first().id
                repository.requestSaveTabs(restored)
            } else {
                val initialTab = Tab(
                    id = "tab_home",
                    title = "New Tab",
                    url = "velocity://newtab",
                    adBlockEnabled = initialSettings.adBlockGlobalEnabled,
                    isDesktopMode = initialSettings.desktopSiteDefault
                )
                _tabs.value = listOf(initialTab)
                _activeTabId.value = initialTab.id
            }
        }
    }

    fun getActiveTab(): Tab? {
        return _tabs.value.find { it.id == _activeTabId.value }
    }

    fun openSheet(sheet: SheetType) {
        if (sheet == SheetType.TAB_SWITCHER) {
            captureCurrentTabThumbnail()
        }
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = SheetType.NONE
    }

    fun createNewTab(url: String = "velocity://newtab", isIncognito: Boolean = false) {
        captureCurrentTabThumbnail()

        val formattedUrl = if (url == "velocity://newtab" || url.isBlank()) {
            "velocity://newtab"
        } else {
            formatUrl(url)
        }

        val domain = AdBlocker.normalizeDomain(formattedUrl)
        val newTab = Tab(
            title = if (isIncognito) "Private Tab" else if (formattedUrl == "velocity://newtab") "New Tab" else "Loading…",
            url = formattedUrl,
            isLoading = formattedUrl != "velocity://newtab",
            progress = if (formattedUrl != "velocity://newtab") 10 else 0,
            isIncognito = isIncognito,
            adBlockEnabled = !AdBlocker.isAllowlisted(domain) && _settings.value.adBlockGlobalEnabled,
            isDesktopMode = _settings.value.desktopSiteDefault
        )
        val updatedList = _tabs.value + newTab
        _tabs.value = updatedList
        _activeTabId.value = newTab.id
        closeSheet()

        repository.requestSaveTabs(updatedList)
    }

    fun selectTab(tabId: String) {
        if (_activeTabId.value != tabId) {
            captureCurrentTabThumbnail()
            _activeTabId.value = tabId
        }
        closeSheet()
    }

    private fun captureCurrentTabThumbnail() {
        val currentId = _activeTabId.value
        if (currentId.isNotEmpty()) {
            val thumbnail = tabWebViewManager.captureThumbnail(currentId)
            if (thumbnail != null) {
                updateTab(currentId) { it.copy(screenshot = thumbnail) }
            }
        }
    }

    fun closeTab(tabId: String) {
        tabWebViewManager.destroyWebView(tabId)

        val currentList = _tabs.value
        val index = currentList.indexOfFirst { it.id == tabId }
        if (index == -1) return

        val newList = currentList.toMutableList().also { it.removeAt(index) }

        if (newList.isEmpty()) {
            val freshTab = Tab(
                title = "New Tab",
                url = "velocity://newtab",
                adBlockEnabled = _settings.value.adBlockGlobalEnabled
            )
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
            repository.requestSaveTabs(listOf(freshTab))
        } else {
            _tabs.value = newList
            if (_activeTabId.value == tabId) {
                val newActiveIndex = if (index >= newList.size) newList.size - 1 else index
                _activeTabId.value = newList[newActiveIndex].id
            }
            repository.requestSaveTabs(newList)
        }
    }

    fun closeAllTabs(onlyIncognito: Boolean = false) {
        if (onlyIncognito) {
            val incognitoIds = _tabs.value.filter { it.isIncognito }.map { it.id }.toSet()
            tabWebViewManager.destroyAll(incognitoIds)

            val regular = _tabs.value.filterNot { it.isIncognito }
            if (regular.isEmpty()) {
                val fresh = Tab()
                _tabs.value = listOf(fresh)
                _activeTabId.value = fresh.id
            } else {
                _tabs.value = regular
                if (getActiveTab()?.isIncognito == true) {
                    _activeTabId.value = regular.first().id
                }
            }
            repository.requestSaveTabs(_tabs.value)
        } else {
            tabWebViewManager.destroyAll()
            val freshTab = Tab()
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
            repository.requestSaveTabs(listOf(freshTab))
        }
    }

    fun updateTab(tabId: String, update: (Tab) -> Tab) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) update(tab) else tab
        }
    }

    fun navigateTo(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty()) return

        val targetUrl = formatUrl(input)
        val currentActiveId = _activeTabId.value
        val domain = AdBlocker.normalizeDomain(targetUrl)

        updateTab(currentActiveId) { tab ->
            tab.copy(
                url = targetUrl,
                title = "Loading…",
                isLoading = true,
                progress = 10,
                adBlockEnabled = !AdBlocker.isAllowlisted(domain) && _settings.value.adBlockGlobalEnabled,
                blockedAdsCount = 0,
                blockedTrackersCount = 0
            )
        }
    }

    private fun formatUrl(input: String): String {
        if (input == "velocity://newtab") return input

        val isUrl = input.startsWith("http://") ||
                input.startsWith("https://") ||
                input.startsWith("file://") ||
                (input.contains(".") && !input.contains(" ") && input.length > 3)

        return if (isUrl) {
            if (input.startsWith("http://") || input.startsWith("https://") || input.startsWith("file://")) {
                input
            } else {
                "https://$input"
            }
        } else {
            _settings.value.defaultSearchEngine.buildQueryUrl(input)
        }
    }

    fun onPageStarted(tabId: String, url: String) {
        val isSecure = url.startsWith("https://")
        val domain = AdBlocker.normalizeDomain(url)
        updateTab(tabId) { tab ->
            tab.copy(
                url = url,
                isLoading = true,
                progress = 20,
                isSecure = isSecure,
                adBlockEnabled = !AdBlocker.isAllowlisted(domain) && _settings.value.adBlockGlobalEnabled
            )
        }
    }

    fun onPageFinished(tabId: String, url: String, title: String?, certInfo: CertificateInfo?) {
        val pageTitle = if (!title.isNullOrBlank() && title != url) title else (UriHelper.extractDomain(url) ?: "Web Page")
        val domain = AdBlocker.normalizeDomain(url)
        updateTab(tabId) { tab ->
            tab.copy(
                url = url,
                title = pageTitle,
                isLoading = false,
                progress = 100,
                isSecure = certInfo?.isSecure ?: url.startsWith("https://"),
                adBlockEnabled = !AdBlocker.isAllowlisted(domain) && _settings.value.adBlockGlobalEnabled
            )
        }

        if (certInfo != null && _activeTabId.value == tabId) {
            _currentCertificateInfo.value = certInfo
        }

        val tab = _tabs.value.find { it.id == tabId }
        if (tab?.isIncognito == false && url != "velocity://newtab") {
            viewModelScope.launch {
                repository.addHistoryItem(HistoryItem(title = pageTitle, url = url))
                repository.requestSaveTabs(_tabs.value)
            }
        }
    }

    fun onProgressChanged(tabId: String, progress: Int) {
        updateTab(tabId) { tab ->
            tab.copy(
                progress = progress,
                isLoading = progress < 100
            )
        }
    }

    fun onTitleReceived(tabId: String, title: String) {
        if (title.isNotBlank()) {
            updateTab(tabId) { it.copy(title = title) }
        }
    }

    fun onFaviconReceived(tabId: String, icon: Bitmap) {
        updateTab(tabId) { it.copy(favicon = icon) }
    }

    fun onNavigationStateChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        updateTab(tabId) { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    fun onTrackerBlocked(tabId: String, isTracker: Boolean = false) {
        updateTab(tabId) { tab ->
            if (isTracker) {
                tab.copy(blockedTrackersCount = tab.blockedTrackersCount + 1)
            } else {
                tab.copy(blockedAdsCount = tab.blockedAdsCount + 1)
            }
        }
        viewModelScope.launch {
            if (isTracker) {
                repository.incrementShieldStats(trackersCount = 1)
            } else {
                repository.incrementShieldStats(adsCount = 1)
            }
        }
    }

    fun toggleDesktopMode(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        val newDesktopMode = !tab.isDesktopMode
        updateTab(tabId) { it.copy(isDesktopMode = newDesktopMode) }

        val webView = tabWebViewManager.getWebView(tabId)
        if (webView != null) {
            tabWebViewManager.applyWebSettings(
                webView = webView,
                settings = _settings.value,
                isDesktopMode = newDesktopMode,
                isIncognito = tab.isIncognito
            )

            val currentUrl = webView.url ?: tab.url
            if (!tab.isNewTab && currentUrl.isNotEmpty()) {
                val targetUrl = if (newDesktopMode) {
                    convertMobileUrlToDesktop(currentUrl)
                } else {
                    convertDesktopUrlToMobile(currentUrl)
                }

                if (targetUrl != currentUrl) {
                    webView.loadUrl(targetUrl)
                } else {
                    webView.reload()
                }
            }
        }
        val statusMsg = if (newDesktopMode) "Desktop site enabled" else "Mobile site enabled"
        showToast(statusMsg)
    }

    fun convertMobileUrlToDesktop(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host ?: return url
            val newHost = when {
                host.equals("m.youtube.com", ignoreCase = true) -> "www.youtube.com"
                host.equals("m.facebook.com", ignoreCase = true) -> "www.facebook.com"
                host.equals("mobile.twitter.com", ignoreCase = true) -> "twitter.com"
                host.equals("m.imdb.com", ignoreCase = true) -> "www.imdb.com"
                host.endsWith(".m.wikipedia.org", ignoreCase = true) -> host.replace(".m.wikipedia.org", ".wikipedia.org")
                host.equals("m.wikipedia.org", ignoreCase = true) -> "wikipedia.org"
                host.startsWith("m.", ignoreCase = true) -> host.removePrefix("m.")
                host.startsWith("mobile.", ignoreCase = true) -> host.removePrefix("mobile.")
                else -> host
            }
            if (newHost != host) {
                java.net.URI(uri.scheme, uri.userInfo, newHost, uri.port, uri.rawPath, uri.rawQuery, uri.rawFragment).toString()
            } else {
                url
            }
        } catch (_: Exception) {
            url
        }
    }

    fun convertDesktopUrlToMobile(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host ?: return url
            val newHost = when {
                host.equals("www.youtube.com", ignoreCase = true) -> "m.youtube.com"
                host.equals("www.facebook.com", ignoreCase = true) -> "m.facebook.com"
                host.equals("www.imdb.com", ignoreCase = true) -> "m.imdb.com"
                host.endsWith(".wikipedia.org", ignoreCase = true) && !host.contains(".m.") -> {
                    host.replace(".wikipedia.org", ".m.wikipedia.org")
                }
                else -> host
            }
            if (newHost != host) {
                java.net.URI(uri.scheme, uri.userInfo, newHost, uri.port, uri.rawPath, uri.rawQuery, uri.rawFragment).toString()
            } else {
                url
            }
        } catch (_: Exception) {
            url
        }
    }

    /**
     * Per-site shield toggle:
     * - Adds or removes the specific domain from the persistent DataStore allowlist.
     * - Keeps other sites protected.
     * - Updates all open tabs for this domain immediately.
     */
    fun toggleTabAdBlock(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        val domain = AdBlocker.normalizeDomain(tab.url)
        if (domain.isEmpty() || domain == "velocity" || domain == "localhost") return

        val isCurrentlyAllowlisted = AdBlocker.isAllowlisted(domain)
        viewModelScope.launch {
            if (isCurrentlyAllowlisted) {
                repository.removeAllowlistedDomain(domain)
                AdBlocker.removeFromAllowlist(domain)
                showToast("Shield enabled for $domain")
            } else {
                repository.addAllowlistedDomain(domain)
                AdBlocker.addToAllowlist(domain)
                showToast("Shield disabled for $domain")
            }

            val isGlobalEnabled = _settings.value.adBlockGlobalEnabled
            _tabs.value = _tabs.value.map { t ->
                val d = AdBlocker.normalizeDomain(t.url)
                if (d == domain) {
                    t.copy(adBlockEnabled = !AdBlocker.isAllowlisted(d) && isGlobalEnabled)
                } else {
                    t
                }
            }
            repository.requestSaveTabs(_tabs.value)
        }
    }

    fun removeAllowlistedDomain(domain: String) {
        val clean = AdBlocker.normalizeDomain(domain)
        if (clean.isEmpty()) return

        viewModelScope.launch {
            repository.removeAllowlistedDomain(clean)
            AdBlocker.removeFromAllowlist(clean)

            val isGlobalEnabled = _settings.value.adBlockGlobalEnabled
            _tabs.value = _tabs.value.map { t ->
                val d = AdBlocker.normalizeDomain(t.url)
                if (d == clean) {
                    t.copy(adBlockEnabled = !AdBlocker.isAllowlisted(d) && isGlobalEnabled)
                } else {
                    t
                }
            }
            repository.requestSaveTabs(_tabs.value)
            showToast("Removed $clean from allowlist")
        }
    }

    fun updateFilterListsNow() {
        if (_isUpdatingFilterLists.value) return

        viewModelScope.launch {
            _isUpdatingFilterLists.value = true
            showToast("Updating filter lists...")
            try {
                val updated = FilterListUpdateWorker.performDirectUpdate(getApplication())
                if (updated) {
                    showToast("Filter lists updated successfully")
                } else {
                    showToast("Filter lists are up to date")
                }
            } catch (e: Exception) {
                showToast("Could not update lists: ${e.localizedMessage}")
            } finally {
                _isUpdatingFilterLists.value = false
            }
        }
    }

    fun toggleBookmarkCurrentTab() {
        val active = getActiveTab() ?: return
        if (active.isNewTab) return

        val existing = _bookmarks.value.find { it.url == active.url }
        viewModelScope.launch {
            if (existing != null) {
                repository.removeBookmark(existing.id)
                showToast("Bookmark removed")
            } else {
                repository.addBookmark(Bookmark(title = active.title, url = active.url))
                showToast("Added to Bookmarks")
            }
        }
    }

    fun removeBookmark(id: String) {
        viewModelScope.launch {
            repository.removeBookmark(id)
            showToast("Bookmark deleted")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            showToast("Browsing history cleared")
        }
    }

    fun removeHistoryItem(id: String) {
        viewModelScope.launch {
            repository.removeHistoryItem(id)
        }
    }

    fun clearBrowsingData(clearCache: Boolean, clearCookies: Boolean, clearHistory: Boolean) {
        viewModelScope.launch {
            if (clearHistory) {
                repository.clearHistory()
            }
            tabWebViewManager.clearDataOnExit(clearCache, clearCookies)
            showToast("Browsing data cleared successfully")
        }
    }

    fun startDownload(url: String, contentDisposition: String, mimeType: String, userAgent: String) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        realDownloadManager.enqueueDownload(
            url = url,
            rawFileName = fileName,
            mimeType = mimeType,
            userAgent = userAgent,
            onDownloadStarted = { item ->
                viewModelScope.launch { repository.addOrUpdateDownload(item) }
                showToast("Downloading: ${item.fileName}")
            },
            onProgressUpdate = { item ->
                viewModelScope.launch { repository.addOrUpdateDownload(item) }
            }
        )
    }

    fun openDownloadedFile(item: DownloadItem) {
        realDownloadManager.openDownloadedFile(item)
    }

    fun clearDownloads() {
        viewModelScope.launch {
            repository.clearDownloads()
            showToast("Download history cleared")
        }
    }

    fun updateSettings(newSettings: BrowserSettings) {
        val globalAdBlockChanged = _settings.value.adBlockGlobalEnabled != newSettings.adBlockGlobalEnabled
        val strictModeChanged = _settings.value.adBlockStrictMode != newSettings.adBlockStrictMode
        val filterTogglesChanged = _settings.value.easyListEnabled != newSettings.easyListEnabled ||
                _settings.value.easyPrivacyEnabled != newSettings.easyPrivacyEnabled ||
                _settings.value.uBlockFiltersEnabled != newSettings.uBlockFiltersEnabled ||
                _settings.value.indianFiltersEnabled != newSettings.indianFiltersEnabled
        val desktopDefaultChanged = _settings.value.desktopSiteDefault != newSettings.desktopSiteDefault

        _settings.value = newSettings
        AdBlocker.isStrictMode = newSettings.adBlockStrictMode
        AdBlocker.setGlobalEnabled(newSettings.adBlockGlobalEnabled)

        if (globalAdBlockChanged || strictModeChanged) {
            _tabs.value = _tabs.value.map { tab ->
                val d = AdBlocker.normalizeDomain(tab.url)
                tab.copy(adBlockEnabled = !AdBlocker.isAllowlisted(d) && newSettings.adBlockGlobalEnabled)
            }
            repository.requestSaveTabs(_tabs.value)
        }

        viewModelScope.launch {
            repository.saveSettings(newSettings)
            tabWebViewManager.applySettingsToAll(newSettings)
            if (filterTogglesChanged) {
                AdBlocker.loadRules(getApplication(), newSettings, forceReload = true)
            }
            if (desktopDefaultChanged) {
                _tabs.value = _tabs.value.map { tab ->
                    tab.copy(isDesktopMode = newSettings.desktopSiteDefault)
                }
                val activeTab = getActiveTab()
                if (activeTab != null && !activeTab.isNewTab) {
                    val webView = tabWebViewManager.getWebView(activeTab.id)
                    webView?.reload()
                }
            }
            showToast("Settings saved")
        }
    }

    fun setSslWarning(state: SslWarningState?) {
        _sslWarningState.value = state
    }

    fun setWebPermission(state: WebPermissionState?) {
        _webPermissionState.value = state
    }

    fun setCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        _customViewState.value = CustomViewState(view, callback)
    }

    fun hideCustomView() {
        _customViewState.value?.callback?.onCustomViewHidden()
        _customViewState.value = null
    }

    fun setFindInPageVisible(visible: Boolean) {
        _isFindInPageVisible.value = visible
        if (!visible) {
            _findInPageQuery.value = ""
        }
    }

    fun setFindInPageQuery(query: String) {
        _findInPageQuery.value = query
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun refreshActiveTabExtensionsCount() {
        val active = getActiveTab() ?: return
        if (active.isNewTab || active.url.isBlank()) return
        val count = ExtensionInjector.countMatchingExtensions(
            targetUrl = active.url,
            extensions = _extensions.value,
            isIncognito = active.isIncognito,
            isGlobalEnabled = _extensionsGlobalEnabled.value
        )
        updateTab(active.id) { it.copy(activeExtensionsCount = count) }
    }

    fun onExtensionsCountUpdated(tabId: String, count: Int) {
        updateTab(tabId) { it.copy(activeExtensionsCount = count) }
    }

    fun toggleExtension(id: String, enabled: Boolean) {
        viewModelScope.launch {
            extensionRepository.toggleExtension(id, enabled)
        }
    }

    fun toggleExtensionsGlobal(enabled: Boolean) {
        viewModelScope.launch {
            extensionRepository.setGlobalEnabled(enabled)
            showToast(if (enabled) "Extensions enabled" else "All extensions disabled")
        }
    }

    fun deleteExtension(id: String) {
        viewModelScope.launch {
            extensionRepository.deleteExtension(id)
            showToast("Extension deleted")
        }
    }

    fun saveExtension(extension: Extension) {
        viewModelScope.launch {
            extensionRepository.saveExtension(extension)
            refreshActiveTabExtensionsCount()
        }
    }

    fun selectExtensionForDetail(extension: Extension) {
        _selectedExtensionForDetail.value = extension
    }

    fun fetchAndPromptUserscript(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (response.isSuccessful && !body.isNullOrBlank()) {
                        val parsed = UserscriptParser.parse(body).copy(updateUrl = url)
                        _pendingInstallScript.value = parsed
                    } else {
                        showToast("Failed to fetch userscript from $url")
                    }
                }
            } catch (e: Exception) {
                showToast("Error downloading userscript: ${e.localizedMessage}")
            }
        }
    }

    fun installFromUrl(url: String) {
        fetchAndPromptUserscript(url)
    }

    fun setPendingInstallScript(script: Extension?) {
        _pendingInstallScript.value = script
    }

    fun dismissInstallDialog() {
        _pendingInstallScript.value = null
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun onActivityStop() {
        tabWebViewManager.saveAllStates()
    }

    fun addCredential(credential: SavedCredential) {
        viewModelScope.launch {
            repository.addCredential(credential)
            showToast("Password saved for ${credential.website}")
        }
    }

    fun removeCredential(id: String) {
        viewModelScope.launch {
            repository.removeCredential(id)
            showToast("Password deleted")
        }
    }

    fun addQuickDial(shortcut: QuickDialShortcut) {
        viewModelScope.launch {
            repository.addQuickDial(shortcut)
            showToast("Shortcut added: ${shortcut.title}")
        }
    }

    fun removeQuickDial(id: String) {
        viewModelScope.launch {
            repository.removeQuickDial(id)
            showToast("Shortcut removed")
        }
    }

    fun addShortcutToHomeScreen(title: String, url: String) {
        val app = getApplication<Application>()
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(app)) {
            showToast("Home screen shortcuts not supported on this launcher")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                setClass(app, com.aaryo.vedabrowser.MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pinShortcutInfo = ShortcutInfoCompat.Builder(app, "shortcut_${System.currentTimeMillis()}")
                .setIcon(IconCompat.createWithResource(app, R.mipmap.ic_launcher))
                .setShortLabel(if (title.isNotBlank()) title.take(15) else "Shortcut")
                .setLongLabel(title)
                .setIntent(intent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(app, pinShortcutInfo, null)
            showToast("Shortcut '$title' added to Home Screen")
        } catch (e: Exception) {
            showToast("Failed to create shortcut: ${e.localizedMessage}")
        }
    }

    fun translateCurrentPage() {
        val tab = getActiveTab()
        if (tab == null || tab.isNewTab || tab.url.isBlank()) {
            showToast("No webpage to translate")
            return
        }
        try {
            val encodedUrl = URLEncoder.encode(tab.url, "UTF-8")
            val targetUrl = "https://translate.google.com/translate?sl=auto&tl=hi&u=$encodedUrl"
            navigateTo(targetUrl)
            showToast("Translating page to Hindi...")
        } catch (_: Exception) {
            showToast("Could not translate page")
        }
    }

    fun navigateToHome() {
        val home = _settings.value.homepageUrl.trim()
        if (home.isNotBlank() && home != "velocity://newtab") {
            navigateTo(home)
        } else {
            navigateTo("velocity://newtab")
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_settings.value.clearDataOnExit) {
            tabWebViewManager.clearDataOnExit(clearCache = true, clearCookies = true)
        }
    }
}

object UriHelper {
    fun extractDomain(url: String): String? {
        val normalized = AdBlocker.normalizeDomain(url)
        return normalized.ifBlank { null }
    }
}
