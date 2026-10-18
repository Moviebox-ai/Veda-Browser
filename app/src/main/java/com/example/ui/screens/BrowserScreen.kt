package com.example.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Tab
import com.example.ui.components.AddressBar
import com.example.ui.components.BottomToolbar
import com.example.ui.components.BrowserMenuSheet
import com.example.ui.components.DownloadConfirmDialog
import com.example.ui.components.FindInPageBar
import com.example.ui.components.JsDialogHost
import com.example.ui.components.SiteInfoSheet
import com.example.ui.components.SslWarningDialog
import com.example.ui.theme.InsecureWarning
import com.example.ui.viewmodel.BrowserViewModel

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSpeedTest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val isAddressBarFocused by viewModel.isAddressBarFocused.collectAsStateWithLifecycle()

    val isBookmarked by viewModel.isCurrentUrlBookmarked.collectAsStateWithLifecycle()
    val isFindInPageVisible by viewModel.isFindInPageVisible.collectAsStateWithLifecycle()
    val findQuery by viewModel.findQuery.collectAsStateWithLifecycle()
    val findCurrentIndex by viewModel.findCurrentIndex.collectAsStateWithLifecycle()
    val findTotalCount by viewModel.findTotalCount.collectAsStateWithLifecycle()

    val isSiteInfoOpen by viewModel.isSiteInfoOpen.collectAsStateWithLifecycle()
    val sslErrorDialogVisible by viewModel.sslErrorDialogVisible.collectAsStateWithLifecycle()
    val activeSslError by viewModel.activeSslError.collectAsStateWithLifecycle()
    val customFullScreenView by viewModel.customFullScreenView.collectAsStateWithLifecycle()
    val isReaderModeActive by viewModel.isReaderModeActive.collectAsStateWithLifecycle()
    val readerArticle by viewModel.readerArticle.collectAsStateWithLifecycle()
    val pendingDownload by viewModel.pendingDownload.collectAsStateWithLifecycle()
    val activeJsDialog by viewModel.activeJsDialog.collectAsStateWithLifecycle()
    val activeHttpAuth by viewModel.activeHttpAuth.collectAsStateWithLifecycle()

    var showMenuSheet by remember { mutableStateOf(false) }

    // Predictive Back Handling with Strict Priority Rules
    BackHandler {
        when {
            customFullScreenView != null -> viewModel.onHideCustomView()
            isReaderModeActive -> viewModel.toggleReaderMode()
            isFindInPageVisible -> viewModel.dismissFindInPage()
            showMenuSheet -> showMenuSheet = false
            isSiteInfoOpen -> viewModel.isSiteInfoOpen.value = false
            activeTab?.canGoBack == true -> viewModel.goBack()
            tabs.size > 1 -> activeTab?.let { viewModel.closeTab(it.id) }
            else -> {
                // Return to home if on web page, or let system handle
                if (activeTab?.url != "velocity:home") {
                    viewModel.navigateTo("velocity:home")
                }
            }
        }
    }

    // Full screen HTML5 video playback takes over UI if active
    if (customFullScreenView != null) {
        AndroidView(
            factory = { customFullScreenView!! },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Reader Mode View if active
    if (isReaderModeActive) {
        ReaderModeScreen(
            article = readerArticle,
            onClose = { viewModel.toggleReaderMode() }
        )
        return
    }

    val currentTab = activeTab

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!settings.bottomToolbar) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    AddressBar(
                        currentTab = currentTab,
                        tabCount = tabs.size,
                        isLoading = currentTab?.isLoading == true,
                        progress = currentTab?.progress ?: 0,
                        onNavigate = { viewModel.navigateTo(it) },
                        onQueryChange = { viewModel.addressInput.value = it },
                        onFocusChange = { viewModel.isAddressBarFocused.value = it },
                        onReload = { viewModel.reload() },
                        onStop = { viewModel.stop() },
                        onOpenTabs = onOpenTabs,
                        onOpenMenu = { showMenuSheet = true },
                        onOpenSiteInfo = { viewModel.isSiteInfoOpen.value = true }
                    )

                    // Find in Page Bar overlay
                    AnimatedVisibility(visible = isFindInPageVisible) {
                        FindInPageBar(
                            query = findQuery,
                            currentIndex = findCurrentIndex,
                            totalCount = findTotalCount,
                            onQueryChange = { viewModel.updateFindQuery(it) },
                            onFindNext = { viewModel.findNext() },
                            onFindPrev = { viewModel.findPrev() },
                            onDismiss = { viewModel.dismissFindInPage() }
                        )
                    }

                    // Offline Warning Banner
                    AnimatedVisibility(visible = !isOnline) {
                        Surface(
                            color = InsecureWarning.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No internet connection. Waiting for network...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = if (settings.bottomToolbar) Modifier.imePadding() else Modifier
            ) {
                if (settings.bottomToolbar) {
                    AnimatedVisibility(visible = isFindInPageVisible) {
                        FindInPageBar(
                            query = findQuery,
                            currentIndex = findCurrentIndex,
                            totalCount = findTotalCount,
                            onQueryChange = { viewModel.updateFindQuery(it) },
                            onFindNext = { viewModel.findNext() },
                            onFindPrev = { viewModel.findPrev() },
                            onDismiss = { viewModel.dismissFindInPage() }
                        )
                    }

                    AddressBar(
                        currentTab = currentTab,
                        tabCount = tabs.size,
                        isLoading = currentTab?.isLoading == true,
                        progress = currentTab?.progress ?: 0,
                        onNavigate = { viewModel.navigateTo(it) },
                        onQueryChange = { viewModel.addressInput.value = it },
                        onFocusChange = { viewModel.isAddressBarFocused.value = it },
                        onReload = { viewModel.reload() },
                        onStop = { viewModel.stop() },
                        onOpenTabs = onOpenTabs,
                        onOpenMenu = { showMenuSheet = true },
                        onOpenSiteInfo = { viewModel.isSiteInfoOpen.value = true }
                    )
                }

                val isHome = currentTab == null || currentTab.url == "velocity:home" || currentTab.url.isEmpty()

                BottomToolbar(
                    isHomeScreen = isHome,
                    canGoBack = currentTab?.canGoBack == true,
                    canGoForward = currentTab?.canGoForward == true,
                    tabCount = tabs.size,
                    isPrivate = currentTab?.isPrivate == true,
                    onBack = { viewModel.goBack() },
                    onForward = { viewModel.goForward() },
                    onHome = { viewModel.navigateTo("velocity:home") },
                    onOpenBookmarks = onOpenBookmarks,
                    onOpenDownloads = onOpenDownloads,
                    onOpenTabs = onOpenTabs,
                    onOpenMenu = { showMenuSheet = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            if (currentTab != null) {
                if (currentTab.url == "velocity:home" || currentTab.url.isEmpty()) {
                    // Native Home Screen
                    val recentHistory by viewModel.history.collectAsStateWithLifecycle()
                    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()

                    HomeScreen(
                        isPrivate = currentTab.isPrivate,
                        searchEngine = settings.searchEngine,
                        shortcuts = shortcuts,
                        recentHistory = recentHistory,
                        blockedCount = currentTab.blockedRequestsCount,
                        onNavigate = { viewModel.navigateTo(it) },
                        onOpenBookmarks = onOpenBookmarks,
                        onOpenHistory = onOpenHistory,
                        onOpenDownloads = onOpenDownloads,
                        onOpenSpeedTest = onOpenSpeedTest,
                        onNewPrivateTab = { viewModel.openNewTab(isPrivate = true) }
                    )
                } else {
                    // Real WebView Container keyed to active tab
                    androidx.compose.runtime.key(currentTab.id) {
                        var lastLoadedUrl by remember(currentTab.id) { mutableStateOf<String?>(null) }

                        AndroidView(
                            factory = { ctx ->
                                val (webClient, chromeClient) = viewModel.createClientsForTab(currentTab.id, ctx)
                                val webView = viewModel.webViewManager.getOrCreateWebView(
                                    tabId = currentTab.id,
                                    context = ctx,
                                    isPrivate = currentTab.isPrivate,
                                    client = webClient,
                                    chromeClient = chromeClient
                                )

                                 // Set Download listener
                                webView.setDownloadListener { url, _, contentDisposition, mimeType, contentLength ->
                                    viewModel.requestDownloadConfirmation(url, contentDisposition, mimeType, contentLength)
                                }

                                // Long click handler to download photos, videos, and media files directly
                                webView.setOnLongClickListener {
                                    val hit = webView.hitTestResult
                                    val url = hit.extra
                                    if (!url.isNullOrBlank()) {
                                        when (hit.type) {
                                            android.webkit.WebView.HitTestResult.IMAGE_TYPE,
                                            android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                                                viewModel.requestDownloadConfirmation(
                                                    url = url,
                                                    contentDisposition = null,
                                                    mimeType = "image/*",
                                                    contentLength = 0L
                                                )
                                                true
                                            }
                                            android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                                                val lower = url.lowercase()
                                                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                                                    lower.endsWith(".png") || lower.endsWith(".webp") ||
                                                    lower.endsWith(".gif") || lower.endsWith(".mp4") ||
                                                    lower.endsWith(".webm") || lower.endsWith(".mkv") ||
                                                    lower.endsWith(".mp3") || lower.endsWith(".pdf") ||
                                                    lower.endsWith(".zip") || lower.endsWith(".apk")
                                                ) {
                                                    viewModel.requestDownloadConfirmation(
                                                        url = url,
                                                        contentDisposition = null,
                                                        mimeType = null,
                                                        contentLength = 0L
                                                    )
                                                    true
                                                } else false
                                            }
                                            else -> false
                                        }
                                    } else false
                                }

                                if (!currentTab.url.startsWith("velocity:") && currentTab.url.isNotEmpty()) {
                                    if (webView.url.isNullOrEmpty() || webView.url == "about:blank") {
                                        lastLoadedUrl = currentTab.url
                                        webView.loadUrl(currentTab.url)
                                    } else {
                                        lastLoadedUrl = webView.url
                                    }
                                }
                                webView
                            },
                            update = { webView ->
                                // Update download listener
                                webView.setDownloadListener { url, _, contentDisposition, mimeType, contentLength ->
                                    viewModel.requestDownloadConfirmation(url, contentDisposition, mimeType, contentLength)
                                }
                                webView.setOnLongClickListener {
                                    val hit = webView.hitTestResult
                                    val url = hit.extra
                                    if (!url.isNullOrBlank()) {
                                        when (hit.type) {
                                            android.webkit.WebView.HitTestResult.IMAGE_TYPE,
                                            android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                                                viewModel.requestDownloadConfirmation(
                                                    url = url,
                                                    contentDisposition = null,
                                                    mimeType = "image/*",
                                                    contentLength = 0L
                                                )
                                                true
                                            }
                                            android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                                                val lower = url.lowercase()
                                                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                                                    lower.endsWith(".png") || lower.endsWith(".webp") ||
                                                    lower.endsWith(".gif") || lower.endsWith(".mp4") ||
                                                    lower.endsWith(".webm") || lower.endsWith(".mkv") ||
                                                    lower.endsWith(".mp3") || lower.endsWith(".pdf") ||
                                                    lower.endsWith(".zip") || lower.endsWith(".apk")
                                                ) {
                                                    viewModel.requestDownloadConfirmation(
                                                        url = url,
                                                        contentDisposition = null,
                                                        mimeType = null,
                                                        contentLength = 0L
                                                    )
                                                    true
                                                } else false
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                // Update text zoom if setting changed
                                webView.settings.textZoom = settings.textZoom

                                // Only load if currentTab.url is a new destination that hasn't been requested yet
                                if (!currentTab.url.startsWith("velocity:") && currentTab.url.isNotEmpty()) {
                                    if (lastLoadedUrl == null) {
                                        lastLoadedUrl = currentTab.url
                                        if (webView.url.isNullOrEmpty() || webView.url == "about:blank") {
                                            webView.loadUrl(currentTab.url)
                                        }
                                    } else if (currentTab.url != lastLoadedUrl && currentTab.url != webView.url) {
                                        lastLoadedUrl = currentTab.url
                                        webView.loadUrl(currentTab.url)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("webview_${currentTab.id}")
                        )
                    }
                }
            }

            // Real-Time Search Suggestions Dropdown Overlay
            if (suggestions.isNotEmpty() && isAddressBarFocused) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(suggestions) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.isAddressBarFocused.value = false
                                        viewModel.navigateTo(suggestion)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.NorthWest,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showMenuSheet) {
        BrowserMenuSheet(
            currentTab = currentTab,
            isBookmarked = isBookmarked,
            onDismiss = { showMenuSheet = false },
            onNewTab = { viewModel.openNewTab(isPrivate = false) },
            onNewPrivateTab = { viewModel.openNewTab(isPrivate = true) },
            onToggleBookmark = { viewModel.toggleBookmark() },
            onOpenBookmarks = onOpenBookmarks,
            onOpenHistory = onOpenHistory,
            onOpenDownloads = onOpenDownloads,
            onFindInPage = { viewModel.startFindInPage() },
            onToggleDesktopMode = { viewModel.toggleDesktopMode() },
            onToggleReaderMode = { viewModel.toggleReaderMode() },
            onShare = {
                currentTab?.url?.let { url ->
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Link"))
                }
            },
            onCopyLink = {
                currentTab?.url?.let { url ->
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("URL", url))
                }
            },
            onOpenSpeedTest = onOpenSpeedTest,
            onOpenSettings = onOpenSettings,
            onClearData = {
                viewModel.clearBrowsingData(clearCookies = true, clearCache = true, clearStorage = true, clearHistory = true)
            }
        )
    }

    if (isSiteInfoOpen) {
        SiteInfoSheet(
            tab = currentTab,
            onDismiss = { viewModel.isSiteInfoOpen.value = false },
            onToggleDesktopMode = { viewModel.toggleDesktopMode() },
            onClearSiteData = {
                viewModel.clearSiteDataForActiveTab()
            }
        )
    }

    if (sslErrorDialogVisible) {
        SslWarningDialog(
            errorMessage = activeSslError,
            onBackToSafety = { viewModel.dismissSslWarning() },
            onProceedAnyway = { viewModel.proceedThroughSslWarning() }
        )
    }

    pendingDownload?.let { download ->
        DownloadConfirmDialog(
            pending = download,
            onConfirm = { viewModel.confirmDownload(context) },
            onDismiss = { viewModel.dismissDownload() }
        )
    }

    JsDialogHost(
        dialogRequest = activeJsDialog,
        httpAuthRequest = activeHttpAuth,
        onDismissJsDialog = { viewModel.dismissJsDialog() },
        onDismissHttpAuth = { viewModel.dismissHttpAuth() }
    )
}
