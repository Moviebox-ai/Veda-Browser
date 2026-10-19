package com.example.velocity.ui.screens

import android.content.Intent
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.velocity.ui.components.BottomNavigationBar
import com.example.velocity.ui.components.FindInPageBar
import com.example.velocity.ui.components.Omnibox
import com.example.velocity.ui.components.WebViewContainer
import com.example.velocity.ui.dialogs.ClearDataDialog
import com.example.velocity.ui.dialogs.ShieldDialog
import com.example.velocity.ui.dialogs.SslDialog
import com.example.velocity.viewmodel.BrowserViewModel
import com.example.velocity.viewmodel.SheetType

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val shieldStats by viewModel.shieldStats.collectAsState()
    val isFindInPageVisible by viewModel.isFindInPageVisible.collectAsState()
    val findInPageQuery by viewModel.findInPageQuery.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentWebView by remember { mutableStateOf<WebView?>(null) }

    val activeTab = tabs.find { it.id == activeTabId }
    val isCurrentBookmarked = activeTab?.let { tab ->
        bookmarks.any { it.url == tab.url }
    } ?: false

    // Handle Toast snackbars
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissToast()
        }
    }

    // Hardware/System Back button navigation
    BackHandler(enabled = true) {
        when {
            activeSheet != SheetType.NONE -> {
                viewModel.closeSheet()
            }
            isFindInPageVisible -> {
                viewModel.setFindInPageVisible(false)
            }
            currentWebView?.canGoBack() == true && activeTab?.isNewTab == false -> {
                currentWebView?.goBack()
            }
            activeTab?.isNewTab == false -> {
                viewModel.navigateTo("velocity://newtab")
            }
            tabs.size > 1 -> {
                viewModel.closeTab(activeTabId)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (activeTab?.isIncognito == true) Color(0xFF0D0A1A) else MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Omnibox / Address Bar (with status bar padding)
                Omnibox(
                    tab = activeTab,
                    tabCount = tabs.size,
                    isBookmarked = isCurrentBookmarked,
                    onNavigate = { viewModel.navigateTo(it) },
                    onReload = { currentWebView?.reload() },
                    onOpenTabSwitcher = { viewModel.openSheet(SheetType.TAB_SWITCHER) },
                    onToggleBookmark = { viewModel.toggleBookmarkCurrentTab() },
                    onOpenShield = { viewModel.openSheet(SheetType.SHIELD_INFO) },
                    onOpenSsl = { viewModel.openSheet(SheetType.SSL_CERTIFICATE) },
                    onMenuAction = { action ->
                        when (action) {
                            "NEW_TAB" -> viewModel.createNewTab(isIncognito = false)
                            "NEW_INCOGNITO_TAB" -> viewModel.createNewTab(isIncognito = true)
                            "BOOKMARKS" -> viewModel.openSheet(SheetType.BOOKMARKS)
                            "HISTORY" -> viewModel.openSheet(SheetType.HISTORY)
                            "DOWNLOADS" -> viewModel.openSheet(SheetType.DOWNLOADS)
                            "TOGGLE_DESKTOP" -> activeTab?.id?.let { viewModel.toggleDesktopMode(it) }
                            "FIND_IN_PAGE" -> viewModel.setFindInPageVisible(true)
                            "SHIELD" -> viewModel.openSheet(SheetType.SHIELD_INFO)
                            "SETTINGS" -> viewModel.openSheet(SheetType.SETTINGS)
                            "SHARE" -> {
                                activeTab?.url?.let { url ->
                                    val sendIntent = Intent().apply {
                                        this.action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, url)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Web Page"))
                                }
                            }
                        }
                    },
                    modifier = Modifier.statusBarsPadding()
                )

                // Optional Find In Page floating bar
                AnimatedVisibility(
                    visible = isFindInPageVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FindInPageBar(
                        query = findInPageQuery,
                        onQueryChange = { viewModel.setFindInPageQuery(it) },
                        onFindNext = { currentWebView?.findNext(true) },
                        onFindPrevious = { currentWebView?.findNext(false) },
                        onClose = { viewModel.setFindInPageVisible(false) }
                    )
                }

                // Main Web/Start Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (activeTab != null) {
                        if (activeTab.isNewTab) {
                            NewTabPage(
                                isIncognito = activeTab.isIncognito,
                                shieldStats = shieldStats,
                                onNavigate = { viewModel.navigateTo(it) },
                                onOpenBookmarks = { viewModel.openSheet(SheetType.BOOKMARKS) },
                                onOpenHistory = { viewModel.openSheet(SheetType.HISTORY) },
                                onOpenDownloads = { viewModel.openSheet(SheetType.DOWNLOADS) },
                                onOpenSettings = { viewModel.openSheet(SheetType.SETTINGS) },
                                onOpenShield = { viewModel.openSheet(SheetType.SHIELD_INFO) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            WebViewContainer(
                                tab = activeTab,
                                onPageStarted = { viewModel.onPageStarted(activeTab.id, it) },
                                onPageFinished = { url, title -> viewModel.onPageFinished(activeTab.id, url, title) },
                                onProgressChanged = { viewModel.onProgressChanged(activeTab.id, it) },
                                onTitleReceived = { viewModel.onTitleReceived(activeTab.id, it) },
                                onFaviconReceived = { viewModel.onFaviconReceived(activeTab.id, it) },
                                onNavigationStateChanged = { canBack, canFwd ->
                                    viewModel.onNavigationStateChanged(activeTab.id, canBack, canFwd)
                                },
                                onTrackerBlocked = { viewModel.onTrackerBlocked(activeTab.id) },
                                onDownloadStart = { url, fileName, mime, length ->
                                    viewModel.addDownloadItem(fileName, url, mime, length)
                                },
                                findInPageQuery = findInPageQuery,
                                onWebViewCreated = { currentWebView = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Bottom Navigation Bar
                BottomNavigationBar(
                    tab = activeTab,
                    onGoBack = { currentWebView?.goBack() },
                    onGoForward = { currentWebView?.goForward() },
                    onGoHome = { viewModel.navigateTo("velocity://newtab") },
                    onOpenShield = { viewModel.openSheet(SheetType.SHIELD_INFO) },
                    onOpenTabs = { viewModel.openSheet(SheetType.TAB_SWITCHER) }
                )
            }

            // Secondary Sheet Overlays
            when (activeSheet) {
                SheetType.TAB_SWITCHER -> {
                    TabSwitcherScreen(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = { viewModel.selectTab(it) },
                        onCloseTab = { viewModel.closeTab(it) },
                        onCreateTab = { viewModel.createNewTab(isIncognito = it) },
                        onCloseAll = { viewModel.closeAllTabs(onlyIncognito = it) },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.BOOKMARKS -> {
                    BookmarksScreen(
                        bookmarks = bookmarks,
                        onSelectBookmark = {
                            viewModel.navigateTo(it)
                            viewModel.closeSheet()
                        },
                        onDeleteBookmark = { viewModel.removeBookmark(it) },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.HISTORY -> {
                    HistoryScreen(
                        history = history,
                        onSelectUrl = {
                            viewModel.navigateTo(it)
                            viewModel.closeSheet()
                        },
                        onDeleteItem = { viewModel.removeHistoryItem(it) },
                        onClearAll = { viewModel.clearHistory() },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.DOWNLOADS -> {
                    DownloadsScreen(
                        downloads = downloads,
                        onAddSampleDownload = {
                            viewModel.addDownloadItem(
                                fileName = "velocity_speed_report.pdf",
                                url = "https://example.com/velocity_speed_report.pdf",
                                mimeType = "application/pdf",
                                totalBytes = 1024 * 1024 * 3
                            )
                        },
                        onClearAll = { viewModel.clearDownloads() },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onOpenClearData = { viewModel.openSheet(SheetType.CLEAR_DATA) },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.SHIELD_INFO -> {
                    ShieldDialog(
                        tab = activeTab,
                        shieldStats = shieldStats,
                        onToggleTabShield = { activeTab?.id?.let { viewModel.toggleTabAdBlock(it) } },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.SSL_CERTIFICATE -> {
                    SslDialog(
                        tab = activeTab,
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.CLEAR_DATA -> {
                    ClearDataDialog(
                        onClear = { cache, cookies, historyToClear ->
                            if (cache) currentWebView?.clearCache(true)
                            if (cookies) android.webkit.CookieManager.getInstance().removeAllCookies(null)
                            viewModel.clearBrowsingData(cache, cookies, historyToClear)
                        },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                else -> { /* No active sheet */ }
            }
        }
    }
}
