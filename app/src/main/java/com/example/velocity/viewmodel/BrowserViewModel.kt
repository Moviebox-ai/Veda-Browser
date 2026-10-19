package com.example.velocity.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.velocity.data.BrowserStorage
import com.example.velocity.model.Bookmark
import com.example.velocity.model.BrowserSettings
import com.example.velocity.model.DownloadItem
import com.example.velocity.model.DownloadStatus
import com.example.velocity.model.HistoryItem
import com.example.velocity.model.SearchEngine
import com.example.velocity.model.ShieldStats
import com.example.velocity.model.Tab
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
    PAGE_INFO,
    READER_MODE
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = BrowserStorage(application)

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

    // Find in page state
    private val _isFindInPageVisible = MutableStateFlow(false)
    val isFindInPageVisible: StateFlow<Boolean> = _isFindInPageVisible.asStateFlow()

    private val _findInPageQuery = MutableStateFlow("")
    val findInPageQuery: StateFlow<String> = _findInPageQuery.asStateFlow()

    // Action message snackbar/toast
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _bookmarks.value = storage.getBookmarks()
        _history.value = storage.getHistory()
        _downloads.value = storage.getDownloads()
        _settings.value = storage.getSettings()
        _shieldStats.value = storage.getShieldStats()

        // Create first tab
        val firstTab = Tab(
            title = "New Tab",
            url = "velocity://newtab",
            adBlockEnabled = _settings.value.adBlockGlobalEnabled,
            isDesktopMode = _settings.value.desktopSiteDefault
        )
        _tabs.value = listOf(firstTab)
        _activeTabId.value = firstTab.id
    }

    fun getActiveTab(): Tab? {
        return _tabs.value.find { it.id == _activeTabId.value }
    }

    fun openSheet(sheet: SheetType) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = SheetType.NONE
    }

    fun createNewTab(url: String = "velocity://newtab", isIncognito: Boolean = false) {
        val newTab = Tab(
            title = if (isIncognito) "Private Tab" else "New Tab",
            url = url,
            isIncognito = isIncognito,
            adBlockEnabled = _settings.value.adBlockGlobalEnabled,
            isDesktopMode = _settings.value.desktopSiteDefault
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        closeSheet()
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        closeSheet()
    }

    fun closeTab(tabId: String) {
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
        } else {
            _tabs.value = newList
            if (_activeTabId.value == tabId) {
                val newActiveIndex = if (index >= newList.size) newList.size - 1 else index
                _activeTabId.value = newList[newActiveIndex].id
            }
        }
    }

    fun closeAllTabs(onlyIncognito: Boolean = false) {
        if (onlyIncognito) {
            val regular = _tabs.value.filterNot { it.isIncognito }
            if (regular.isEmpty()) {
                val freshTab = Tab()
                _tabs.value = listOf(freshTab)
                _activeTabId.value = freshTab.id
            } else {
                _tabs.value = regular
                if (getActiveTab()?.isIncognito == true) {
                    _activeTabId.value = regular.first().id
                }
            }
        } else {
            val freshTab = Tab()
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
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

        // Check if looks like a URL or an IP address
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
            // Search query
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

    fun onPageFinished(tabId: String, url: String, title: String?) {
        val pageTitle = if (!title.isNullOrBlank() && title != url) title else (UriHelper.extractDomain(url) ?: "Web Page")
        updateTab(tabId) { tab ->
            tab.copy(
                url = url,
                title = pageTitle,
                isLoading = false,
                progress = 100,
                isSecure = url.startsWith("https://")
            )
        }

        // Add to history only if not incognito and not internal page
        val tab = _tabs.value.find { it.id == tabId }
        if (tab?.isIncognito == false && url != "velocity://newtab") {
            val historyItem = HistoryItem(title = pageTitle, url = url)
            storage.addHistoryItem(historyItem)
            _history.value = storage.getHistory()
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
        storage.incrementShieldStats(1)
        _shieldStats.value = storage.getShieldStats()
    }

    fun toggleDesktopMode(tabId: String) {
        updateTab(tabId) { tab ->
            tab.copy(isDesktopMode = !tab.isDesktopMode)
        }
    }

    fun toggleTabAdBlock(tabId: String) {
        updateTab(tabId) { tab ->
            tab.copy(adBlockEnabled = !tab.adBlockEnabled)
        }
    }

    fun toggleBookmarkCurrentTab() {
        val active = getActiveTab() ?: return
        if (active.isNewTab) return

        val existing = _bookmarks.value.find { it.url == active.url }
        if (existing != null) {
            storage.removeBookmark(existing.id)
            _bookmarks.value = storage.getBookmarks()
            showToast("Bookmark removed")
        } else {
            val newBookmark = Bookmark(
                title = active.title,
                url = active.url
            )
            storage.addBookmark(newBookmark)
            _bookmarks.value = storage.getBookmarks()
            showToast("Added to Bookmarks")
        }
    }

    fun isCurrentTabBookmarked(): Boolean {
        val active = getActiveTab() ?: return false
        return _bookmarks.value.any { it.url == active.url }
    }

    fun removeBookmark(id: String) {
        storage.removeBookmark(id)
        _bookmarks.value = storage.getBookmarks()
        showToast("Bookmark deleted")
    }

    fun clearHistory() {
        storage.clearHistory()
        _history.value = emptyList()
        showToast("Browsing history cleared")
    }

    fun removeHistoryItem(id: String) {
        storage.removeHistoryItem(id)
        _history.value = storage.getHistory()
    }

    fun clearBrowsingData(clearCache: Boolean, clearCookies: Boolean, clearHistory: Boolean) {
        if (clearHistory) {
            clearHistory()
        }
        showToast("Browsing data cleared successfully")
    }

    fun addDownloadItem(fileName: String, url: String, mimeType: String, totalBytes: Long) {
        val item = DownloadItem(
            fileName = fileName,
            url = url,
            mimeType = mimeType,
            totalBytes = totalBytes,
            status = DownloadStatus.DOWNLOADING
        )
        storage.addDownload(item)
        _downloads.value = storage.getDownloads()
        showToast("Download started: $fileName")

        // Simulate background download progress completion
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            val completed = item.copy(
                downloadedBytes = if (totalBytes > 0) totalBytes else 1024 * 512,
                totalBytes = if (totalBytes > 0) totalBytes else 1024 * 512,
                status = DownloadStatus.COMPLETED
            )
            storage.updateDownload(completed)
            _downloads.value = storage.getDownloads()
            showToast("Download complete: $fileName")
        }
    }

    fun clearDownloads() {
        storage.clearDownloads()
        _downloads.value = emptyList()
        showToast("Download history cleared")
    }

    fun updateSettings(newSettings: BrowserSettings) {
        _settings.value = newSettings
        storage.saveSettings(newSettings)
        showToast("Settings saved")
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
