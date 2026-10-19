package com.aaryo.velocity.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaryo.velocity.data.BrowserRepository
import com.aaryo.velocity.download.RealDownloadManager
import com.aaryo.velocity.model.Bookmark
import com.aaryo.velocity.model.BrowserSettings
import com.aaryo.velocity.model.DownloadItem
import com.aaryo.velocity.model.HistoryItem
import com.aaryo.velocity.model.ShieldStats
import com.aaryo.velocity.model.Tab
import com.aaryo.velocity.privacy.AdBlocker
import com.aaryo.velocity.security.CertificateInfo
import com.aaryo.velocity.ui.dialogs.SslWarningState
import com.aaryo.velocity.ui.dialogs.WebPermissionState
import com.aaryo.velocity.webview.TabWebViewManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    ALLOWLIST_MANAGER
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

    // Toast/Snackbar notifications
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // Initialize bundled AdBlocker list in background
        AdBlocker.initialize(application)

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
                _settings.value = it
                tabWebViewManager.applySettingsToAll(it)
            }
        }
        viewModelScope.launch {
            repository.shieldStatsFlow.collect { _shieldStats.value = it }
        }

        // Restore open tabs from disk on launch
        viewModelScope.launch {
            val persistedTabs = repository.getInitialTabs()
            if (persistedTabs.isNotEmpty()) {
                val restored = persistedTabs.map {
                    Tab(
                        id = it.id,
                        title = it.title,
                        url = it.url,
                        adBlockEnabled = _settings.value.adBlockGlobalEnabled,
                        isDesktopMode = _settings.value.desktopSiteDefault
                    )
                }
                _tabs.value = restored
                _activeTabId.value = restored.first().id
            } else {
                val initialTab = Tab(
                    title = "New Tab",
                    url = "velocity://newtab",
                    adBlockEnabled = _settings.value.adBlockGlobalEnabled,
                    isDesktopMode = _settings.value.desktopSiteDefault
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
        // Capture thumbnail of current tab when opening tab switcher
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

        val newTab = Tab(
            title = if (isIncognito) "Private Tab" else "New Tab",
            url = url,
            isIncognito = isIncognito,
            adBlockEnabled = _settings.value.adBlockGlobalEnabled,
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

        updateTab(currentActiveId) { tab ->
            tab.copy(
                url = targetUrl,
                title = "Loading…",
                isLoading = true,
                progress = 10
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
        updateTab(tabId) { tab ->
            tab.copy(
                url = url,
                isLoading = true,
                progress = 20,
                isSecure = isSecure
            )
        }
    }

    fun onPageFinished(tabId: String, url: String, title: String?, certInfo: CertificateInfo?) {
        val pageTitle = if (!title.isNullOrBlank() && title != url) title else (UriHelper.extractDomain(url) ?: "Web Page")
        updateTab(tabId) { tab ->
            tab.copy(
                url = url,
                title = pageTitle,
                isLoading = false,
                progress = 100,
                isSecure = certInfo?.isSecure ?: url.startsWith("https://")
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

    fun onTrackerBlocked(tabId: String) {
        updateTab(tabId) { tab ->
            tab.copy(blockedTrackersCount = tab.blockedTrackersCount + 1)
        }
        viewModelScope.launch {
            repository.incrementShieldStats(1)
        }
    }

    fun toggleDesktopMode(tabId: String) {
        updateTab(tabId) { tab ->
            tab.copy(isDesktopMode = !tab.isDesktopMode)
        }
    }

    fun toggleTabAdBlock(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        val newState = !tab.adBlockEnabled
        updateTab(tabId) { it.copy(adBlockEnabled = newState) }

        // Also add or remove from AdBlocker allowlist
        val domain = UriHelper.extractDomain(tab.url)
        if (!domain.isNullOrEmpty()) {
            if (!newState) {
                AdBlocker.addToAllowlist(domain)
                showToast("Shield disabled for $domain")
            } else {
                AdBlocker.removeFromAllowlist(domain)
                showToast("Shield enabled for $domain")
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
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            tabWebViewManager.applySettingsToAll(newSettings)
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

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun onActivityStop() {
        tabWebViewManager.saveAllStates()
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
        return try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host
            if (host.isNullOrEmpty()) null else host.removePrefix("www.")
        } catch (_: Exception) {
            null
        }
    }
}
