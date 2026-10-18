package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.VelocityApplication
import com.example.data.local.preferences.BrowserSettings
import com.example.domain.model.Bookmark
import com.example.domain.model.HistoryItem
import com.example.domain.model.Shortcut
import com.example.domain.model.Tab
import com.example.util.AppLogger
import com.example.util.UrlResolver
import com.example.webview.ReaderArticle
import com.example.webview.ReaderModeExtractor
import com.example.webview.VelocityChromeClient
import com.example.webview.VelocityWebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BrowserViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app = application as VelocityApplication
    val tabManager = app.tabManager
    val webViewManager = app.webViewManager
    val repository = app.browserRepository
    val downloadRepo = app.downloadRepository
    val suggestionService = app.suggestionService
    val networkMonitor = app.networkMonitor
    val speedTestManager = app.speedTestManager
    val speedTestState = speedTestManager.state

    var onShowFileChooserHandler: ((android.webkit.ValueCallback<Array<android.net.Uri>>?, android.webkit.WebChromeClient.FileChooserParams?) -> Boolean)? = null
    var onRequestNotificationPermission: (() -> Unit)? = null

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val settings: StateFlow<BrowserSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = BrowserSettings()
    )

    val tabs: StateFlow<List<Tab>> = tabManager.tabs

    val activeTabId: StateFlow<String?> = tabManager.activeTabId

    val activeTab: StateFlow<Tab?> = tabManager.tabs.map { list ->
        list.find { it.id == tabManager.activeTabId.value }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, tabManager.activeTab)

    init {
        viewModelScope.launch {
            settings.collect { currentSettings ->
                webViewManager.contentBlocker.isEnabled = currentSettings.contentBlockerEnabled
            }
        }
    }

    // Address Bar State
    val addressInput = MutableStateFlow("")
    val isAddressBarFocused = MutableStateFlow(false)

    // Autocomplete & Suggestions
    val suggestions: StateFlow<List<String>> = addressInput
        .debounce(200)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            val trimmed = query.trim()
            val isPrivate = activeTab.value?.isPrivate == true
            // Privacy rule: NEVER query suggestions service in private tabs
            if (!isPrivate && trimmed.length >= 2 && isAddressBarFocused.value && 
                !trimmed.startsWith("http://", ignoreCase = true) && 
                !trimmed.startsWith("https://", ignoreCase = true) && 
                !trimmed.startsWith("velocity:", ignoreCase = true)) {
                flow {
                    try {
                        emit(suggestionService.getSuggestions(trimmed, settings.value.searchEngine))
                    } catch (_: Throwable) {
                        emit(emptyList())
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarks and History for active tab
    val isCurrentUrlBookmarked: StateFlow<Boolean> = activeTab.flatMapLatest { tab ->
        if (tab != null && !tab.isInternal) {
            repository.isBookmarked(tab.url)
        } else {
            flowOf(false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val bookmarks: StateFlow<List<Bookmark>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortcuts: StateFlow<List<Shortcut>> = repository.getAllShortcuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Find In Page
    val isFindInPageVisible = MutableStateFlow(false)
    val findQuery = MutableStateFlow("")
    val findCurrentIndex = MutableStateFlow(0)
    val findTotalCount = MutableStateFlow(0)

    // Site Info & Dialogs
    val isSiteInfoOpen = MutableStateFlow(false)

    // Per-tab SSL Error Tracking
    data class PendingSslError(
        val tabId: String,
        val errorMessage: String,
        val failingUrl: String,
        val proceed: () -> Unit,
        val cancel: () -> Unit
    )
    val pendingSslErrors = ConcurrentHashMap<String, PendingSslError>()

    val sslErrorDialogVisible = MutableStateFlow(false)
    val activeSslError = MutableStateFlow<String?>(null)
    val sslProceedCallback = MutableStateFlow<(() -> Unit)?>(null)
    val sslCancelCallback = MutableStateFlow<(() -> Unit)?>(null)
    val navigationEvent = MutableStateFlow<String?>(null)

    // Full screen HTML5 video custom view
    val customFullScreenView = MutableStateFlow<View?>(null)
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Reader Mode
    val isReaderModeActive = MutableStateFlow(false)
    val readerArticle = MutableStateFlow<ReaderArticle?>(null)
    val readerLoading = MutableStateFlow(false)

    // Download confirmation dialog state
    data class PendingDownload(
        val url: String,
        val contentDisposition: String?,
        val mimeType: String?,
        val contentLength: Long,
        val filename: String
    )
    val pendingDownload = MutableStateFlow<PendingDownload?>(null)

    // JavaScript Dialogs & HTTP Authentication
    sealed class JsDialogRequest {
        abstract val message: String
        abstract val onConfirm: (String) -> Unit
        abstract val onCancel: () -> Unit

        data class Alert(
            override val message: String,
            override val onConfirm: (String) -> Unit,
            override val onCancel: () -> Unit
        ) : JsDialogRequest()

        data class Confirm(
            override val message: String,
            override val onConfirm: (String) -> Unit,
            override val onCancel: () -> Unit
        ) : JsDialogRequest()

        data class Prompt(
            override val message: String,
            val defaultValue: String,
            override val onConfirm: (String) -> Unit,
            override val onCancel: () -> Unit
        ) : JsDialogRequest()

        data class BeforeUnload(
            override val message: String,
            override val onConfirm: (String) -> Unit,
            override val onCancel: () -> Unit
        ) : JsDialogRequest()
    }

    data class HttpAuthRequest(
        val host: String,
        val realm: String,
        val onSubmit: (String, String) -> Unit,
        val onCancel: () -> Unit
    )

    val activeJsDialog = MutableStateFlow<JsDialogRequest?>(null)
    val activeHttpAuth = MutableStateFlow<HttpAuthRequest?>(null)

    fun dismissJsDialog() {
        activeJsDialog.value?.onCancel?.invoke()
        activeJsDialog.value = null
    }

    fun dismissHttpAuth() {
        activeHttpAuth.value?.onCancel?.invoke()
        activeHttpAuth.value = null
    }

    fun navigateTo(input: String) {
        val current = activeTab.value ?: return
        val currentEngine = settings.value.searchEngine
        val resolved = UrlResolver.resolve(input, currentEngine)

        if (resolved.type == UrlResolver.ResolvedType.BLOCKED_SCHEME) {
            AppLogger.w("BrowserViewModel", "Navigation blocked: ${resolved.error}")
            return
        }

        // HTTPS-First Escalation: upgrade http:// to https:// when enabled
        var targetUrl = resolved.url
        if (settings.value.httpsFirst && targetUrl.startsWith("http://", ignoreCase = true)) {
            targetUrl = "https://" + targetUrl.removePrefix("http://")
        }

        tabManager.updateTab(current.id) {
            it.copy(
                url = targetUrl,
                title = if (targetUrl.startsWith("velocity:")) "New Tab" else targetUrl,
                isLoading = true,
                sslError = null
            )
        }

        val webView = webViewManager.getWebView(current.id)
        if (webView != null && !targetUrl.startsWith("velocity:")) {
            webView.loadUrl(targetUrl)
        }
    }

    fun goBack() {
        val tab = activeTab.value ?: return
        val webView = webViewManager.getWebView(tab.id)
        if (webView?.canGoBack() == true) {
            webView.goBack()
        }
    }

    fun goForward() {
        val tab = activeTab.value ?: return
        val webView = webViewManager.getWebView(tab.id)
        if (webView?.canGoForward() == true) {
            webView.goForward()
        }
    }

    fun reload() {
        val tab = activeTab.value ?: return
        val webView = webViewManager.getWebView(tab.id)
        webView?.reload()
    }

    fun stop() {
        val tab = activeTab.value ?: return
        val webView = webViewManager.getWebView(tab.id)
        webView?.stopLoading()
        tabManager.updateTab(tab.id) { it.copy(isLoading = false) }
    }

    fun openNewTab(url: String = "velocity:home", isPrivate: Boolean = false) {
        val currentEngine = settings.value.searchEngine
        val resolved = UrlResolver.resolve(url, currentEngine, settings.value.httpsFirst)
        val safeUrl = if (resolved.type == UrlResolver.ResolvedType.BLOCKED_SCHEME) {
            AppLogger.w("BrowserViewModel", "Blocked dangerous scheme in openNewTab: ${resolved.error}")
            "velocity:home"
        } else {
            resolved.url
        }

        val newTab = tabManager.createTab(url = safeUrl, isPrivate = isPrivate, select = true)
        addressInput.value = ""
        isAddressBarFocused.value = false
        if (!safeUrl.startsWith("velocity:") && safeUrl != "about:blank") {
            navigateTo(safeUrl)
        }
    }

    fun selectTab(tabId: String) {
        activeJsDialog.value?.onCancel?.invoke()
        activeJsDialog.value = null
        activeHttpAuth.value?.onCancel?.invoke()
        activeHttpAuth.value = null

        tabManager.selectTab(tabId)
        val tab = tabManager.tabs.value.find { it.id == tabId }
        addressInput.value = tab?.url?.takeIf { !it.startsWith("velocity:") } ?: ""
        isAddressBarFocused.value = false

        // Synchronize SSL error dialog state for the newly focused tab
        val pending = pendingSslErrors[tabId]
        if (pending != null) {
            activeSslError.value = pending.errorMessage
            sslProceedCallback.value = pending.proceed
            sslCancelCallback.value = pending.cancel
            sslErrorDialogVisible.value = true
        } else {
            sslErrorDialogVisible.value = false
        }
    }

    fun closeTab(tabId: String) {
        val closedTab = tabManager.tabs.value.find { it.id == tabId }
        if (activeTabId.value == tabId) {
            activeJsDialog.value?.onCancel?.invoke()
            activeJsDialog.value = null
            activeHttpAuth.value?.onCancel?.invoke()
            activeHttpAuth.value = null
        }
        tabManager.closeTab(tabId)
        webViewManager.destroyTab(tabId)
        pendingSslErrors.remove(tabId)?.cancel?.invoke()

        if (closedTab?.isPrivate == true) {
            val remainingPrivate = tabManager.tabs.value.any { it.isPrivate }
            if (!remainingPrivate) {
                webViewManager.cleanupPrivateProfile()
            }
        }
    }

    fun closeOtherTabs(tabId: String) {
        tabManager.closeOtherTabs(tabId)
    }

    fun closeAllTabs(privateOnly: Boolean = false) {
        tabManager.closeAllTabs(isPrivateOnly = privateOnly)
        val remainingPrivate = tabManager.tabs.value.any { it.isPrivate }
        if (!remainingPrivate) {
            webViewManager.cleanupPrivateProfile()
        }
    }

    fun reopenClosedTab() {
        tabManager.reopenLastClosedTab()
    }

    fun toggleBookmark() {
        val tab = activeTab.value ?: return
        if (tab.isInternal) return

        viewModelScope.launch(Dispatchers.IO) {
            val bookmarked = isCurrentUrlBookmarked.first()
            if (bookmarked) {
                repository.deleteBookmarkByUrl(tab.url)
            } else {
                repository.addBookmark(tab.title.ifEmpty { tab.url }, tab.url)
            }
        }
    }

    fun toggleDesktopMode() {
        val tab = activeTab.value ?: return
        val newMode = !tab.desktopMode
        tabManager.updateTab(tab.id) { it.copy(desktopMode = newMode) }
        webViewManager.setDesktopMode(tab.id, newMode)
    }

    fun toggleReaderMode() {
        val tab = activeTab.value ?: return
        val webView = webViewManager.getWebView(tab.id) ?: return

        if (isReaderModeActive.value) {
            isReaderModeActive.value = false
            readerArticle.value = null
            return
        }

        readerLoading.value = true
        viewModelScope.launch(Dispatchers.Main) {
            val article = ReaderModeExtractor.extract(webView)
            readerLoading.value = false
            if (article != null) {
                readerArticle.value = article
                isReaderModeActive.value = true
            } else {
                isReaderModeActive.value = false
            }
        }
    }

    // Find in Page
    fun startFindInPage() {
        isFindInPageVisible.value = true
        findQuery.value = ""
        findCurrentIndex.value = 0
        findTotalCount.value = 0
    }

    fun dismissFindInPage() {
        isFindInPageVisible.value = false
        findQuery.value = ""
        val tab = activeTab.value ?: return
        webViewManager.getWebView(tab.id)?.clearMatches()
    }

    fun updateFindQuery(query: String) {
        findQuery.value = query
        val tab = activeTab.value ?: return
        val webView = webViewManager.getWebView(tab.id) ?: return

        if (query.isBlank()) {
            webView.clearMatches()
            findCurrentIndex.value = 0
            findTotalCount.value = 0
            return
        }

        webView.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            findCurrentIndex.value = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
            findTotalCount.value = numberOfMatches
        }
        webView.findAllAsync(query)
    }

    fun findNext() {
        val tab = activeTab.value ?: return
        webViewManager.getWebView(tab.id)?.findNext(true)
    }

    fun findPrev() {
        val tab = activeTab.value ?: return
        webViewManager.getWebView(tab.id)?.findNext(false)
    }

    // Full screen Video Handling
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        customFullScreenView.value = view
        customViewCallback = callback
    }

    fun onHideCustomView() {
        customFullScreenView.value = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    // Downloads
    fun requestDownloadConfirmation(url: String, contentDisposition: String?, mimeType: String?, contentLength: Long) {
        val suggested = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
        pendingDownload.value = PendingDownload(
            url = url,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            contentLength = contentLength,
            filename = suggested
        )
    }

    fun confirmDownload(context: android.content.Context) {
        val pending = pendingDownload.value ?: return
        onRequestNotificationPermission?.invoke()
        viewModelScope.launch(Dispatchers.IO) {
            downloadRepo.enqueueDownload(
                context = context,
                url = pending.url,
                contentDisposition = pending.contentDisposition,
                mimeType = pending.mimeType,
                contentLength = pending.contentLength
            )
        }
        pendingDownload.value = null
    }

    fun dismissDownload() {
        pendingDownload.value = null
    }

    fun proceedThroughSslWarning() {
        val currentTabId = activeTabId.value
        val pending = pendingSslErrors.remove(currentTabId)
        pending?.proceed?.invoke()
        sslProceedCallback.value = null
        sslCancelCallback.value = null
        sslErrorDialogVisible.value = false
    }

    fun dismissSslWarning() {
        val currentTabId = activeTabId.value
        val pending = pendingSslErrors.remove(currentTabId)
        pending?.cancel?.invoke()
        sslProceedCallback.value = null
        sslCancelCallback.value = null
        sslErrorDialogVisible.value = false
        navigateTo("velocity:home")
    }

    // Create WebView client wiring for a given tab
    fun createClientsForTab(tabId: String, context: android.content.Context): Pair<VelocityWebViewClient, VelocityChromeClient> {
        val webClient = VelocityWebViewClient(
            context = context,
            contentBlocker = webViewManager.contentBlocker,
            onPageStartedCallback = { url ->
                tabManager.updateTab(tabId) { it.copy(url = url, isLoading = true) }
            },
            onPageFinishedCallback = { url, canBack, canForward, isSecure ->
                val tab = tabManager.tabs.value.find { it.id == tabId }
                tabManager.updateTab(tabId) {
                    it.copy(
                        url = url,
                        isLoading = false,
                        progress = 100,
                        canGoBack = canBack,
                        canGoForward = canForward,
                        isSecure = isSecure
                    )
                }
                // Record in history strictly if non-private and history saving is enabled
                if (tab?.isPrivate == false && settings.value.saveHistory && !url.startsWith("velocity:")) {
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.recordVisit(tab.title.ifEmpty { url }, url)
                    }
                }
            },
            onSslErrorCallback = { errorMsg, failingUrl, proceed, cancel ->
                val errorItem = PendingSslError(tabId, errorMsg, failingUrl, proceed, cancel)
                pendingSslErrors[tabId] = errorItem
                if (activeTabId.value == tabId) {
                    activeSslError.value = errorMsg
                    sslProceedCallback.value = proceed
                    sslCancelCallback.value = cancel
                    sslErrorDialogVisible.value = true
                }
                tabManager.updateTab(tabId) {
                    it.copy(sslError = errorMsg, isSecure = false, isLoading = false)
                }
            },
            onRenderProcessCrashedCallback = {
                // Recover renderer gracefully
                val tab = tabManager.tabs.value.find { it.id == tabId }
                webViewManager.destroyTab(tabId)
                if (tab != null && !tab.url.startsWith("velocity:")) {
                    navigateTo(tab.url)
                }
            },
            onInternalNavigation = { internalUrl ->
                navigateTo(internalUrl)
            },
            onHttpAuthRequestCallback = { host, realm, proceed, cancel ->
                if (activeTabId.value == tabId) {
                    activeHttpAuth.value = HttpAuthRequest(
                        host = host,
                        realm = realm,
                        onSubmit = { user, pass -> proceed(user, pass) },
                        onCancel = { cancel() }
                    )
                } else {
                    cancel()
                }
            }
        )

        val chromeClient = VelocityChromeClient(
            onProgressChangedCallback = { progress ->
                tabManager.updateTab(tabId) {
                    it.copy(
                        progress = progress,
                        isLoading = progress < 100,
                        blockedRequestsCount = webViewManager.contentBlocker.totalBlockedCount
                    )
                }
            },
            onTitleReceivedCallback = { title ->
                tabManager.updateTab(tabId) { it.copy(title = title) }
            },
            onIconReceivedCallback = { bitmap ->
                tabManager.updateTab(tabId) { it.copy(favicon = bitmap) }
            },
            onShowCustomViewCallback = { view, callback ->
                onShowCustomView(view, callback)
            },
            onHideCustomViewCallback = {
                onHideCustomView()
            },
            onShowFileChooserCallback = { callback, params ->
                onShowFileChooserHandler?.invoke(callback, params) ?: false
            },
            onJsAlertCallback = { msg, res ->
                if (activeTabId.value == tabId) {
                    activeJsDialog.value = JsDialogRequest.Alert(
                        message = msg,
                        onConfirm = { res.confirm() },
                        onCancel = { res.cancel() }
                    )
                } else {
                    res.cancel()
                }
            },
            onJsConfirmCallback = { msg, res ->
                if (activeTabId.value == tabId) {
                    activeJsDialog.value = JsDialogRequest.Confirm(
                        message = msg,
                        onConfirm = { res.confirm() },
                        onCancel = { res.cancel() }
                    )
                } else {
                    res.cancel()
                }
            },
            onJsPromptCallback = { msg, def, res ->
                if (activeTabId.value == tabId) {
                    activeJsDialog.value = JsDialogRequest.Prompt(
                        message = msg,
                        defaultValue = def,
                        onConfirm = { input -> res.confirm(input) },
                        onCancel = { res.cancel() }
                    )
                } else {
                    res.cancel()
                }
            },
            onJsBeforeUnloadCallback = { msg, res ->
                if (activeTabId.value == tabId) {
                    activeJsDialog.value = JsDialogRequest.BeforeUnload(
                        message = msg,
                        onConfirm = { res.confirm() },
                        onCancel = { res.cancel() }
                    )
                } else {
                    res.cancel()
                }
            }
        )

        return Pair(webClient, chromeClient)
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            speedTestManager.runSpeedTest()
        }
    }

    fun resetSpeedTest() {
        speedTestManager.reset()
    }

    fun pauseDownload(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadRepo.pauseDownload(id)
        }
    }

    fun resumeDownload(context: android.content.Context, id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadRepo.resumeDownload(context, id)
        }
    }

    fun clearBrowsingData(clearCookies: Boolean, clearCache: Boolean, clearStorage: Boolean, clearHistory: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (clearHistory) {
                repository.clearHistory()
            }
            withContext(Dispatchers.Main) {
                webViewManager.clearBrowsingData(clearCookies, clearCache, clearStorage)
            }
        }
    }

    fun clearSiteDataForActiveTab() {
        val tab = activeTab.value ?: return
        val origin = try {
            val uri = android.net.Uri.parse(tab.url)
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: return
            val port = if (uri.port != -1) ":${uri.port}" else ""
            "$scheme://$host$port"
        } catch (_: Exception) {
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            webViewManager.clearSiteData(origin)
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBookmark(title, url)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmark(bookmark)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    fun cancelDownload(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadRepo.cancelDownload(id)
        }
    }

    fun deleteDownload(context: android.content.Context, item: com.example.domain.model.DownloadItem) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadRepo.deleteDownload(context, item)
        }
    }

    fun setSearchEngine(engineId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSearchEngine(engineId)
        }
    }

    fun setContentBlocker(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setContentBlocker(enabled)
        }
    }

    fun setHttpsFirst(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setHttpsFirst(enabled)
        }
    }

    fun setBottomToolbar(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setBottomToolbar(enabled)
        }
    }

    fun setDarkThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDarkThemeMode(mode)
        }
    }

    fun setSaveHistory(save: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSaveHistory(save)
        }
    }

    fun setTextZoom(zoom: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setTextZoom(zoom)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BrowserViewModel(application) as T
        }
    }
}
