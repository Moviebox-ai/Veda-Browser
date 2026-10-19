package com.aaryo.velocity.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.aaryo.velocity.security.CertificateInfo
import com.aaryo.velocity.ui.components.BottomNavigationBar
import com.aaryo.velocity.ui.components.FindInPageBar
import com.aaryo.velocity.ui.components.Omnibox
import com.aaryo.velocity.ui.components.WebViewContainer
import com.aaryo.velocity.ui.dialogs.ClearDataDialog
import com.aaryo.velocity.ui.dialogs.InstallUserscriptDialog
import com.aaryo.velocity.ui.dialogs.ShieldDialog
import com.aaryo.velocity.ui.dialogs.SslDialog
import com.aaryo.velocity.ui.dialogs.SslWarningDialog
import com.aaryo.velocity.ui.dialogs.WebPermissionDialog
import com.aaryo.velocity.ui.dialogs.WebPermissionState
import com.aaryo.velocity.ui.screens.ExtensionDetailScreen
import com.aaryo.velocity.ui.screens.ExtensionsScreen
import com.aaryo.velocity.viewmodel.BrowserViewModel
import com.aaryo.velocity.viewmodel.SheetType
import com.aaryo.velocity.viewmodel.UriHelper

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
    val sslWarningState by viewModel.sslWarningState.collectAsState()
    val currentCertificateInfo by viewModel.currentCertificateInfo.collectAsState()
    val webPermissionState by viewModel.webPermissionState.collectAsState()
    val customViewState by viewModel.customViewState.collectAsState()
    val isFindInPageVisible by viewModel.isFindInPageVisible.collectAsState()
    val findInPageQuery by viewModel.findInPageQuery.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val extensions by viewModel.extensions.collectAsState()
    val extensionsGlobalEnabled by viewModel.extensionsGlobalEnabled.collectAsState()
    val selectedExtensionForDetail by viewModel.selectedExtensionForDetail.collectAsState()
    val pendingInstallScript by viewModel.pendingInstallScript.collectAsState()

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
    // If single tab and on new-tab page (with no overlays active), disable handler to allow system exit
    val isBackHandlerEnabled = !(tabs.size <= 1 && activeTab?.isNewTab == true && activeSheet == SheetType.NONE && !isFindInPageVisible && customViewState == null)

    BackHandler(enabled = isBackHandlerEnabled) {
        when {
            customViewState != null -> {
                viewModel.hideCustomView()
            }
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
                    onOpenExtensions = { viewModel.openSheet(SheetType.EXTENSIONS) },
                    onMenuAction = { action ->
                        when (action) {
                            "NEW_TAB" -> viewModel.createNewTab(isIncognito = false)
                            "NEW_INCOGNITO_TAB" -> viewModel.createNewTab(isIncognito = true)
                            "BOOKMARKS" -> viewModel.openSheet(SheetType.BOOKMARKS)
                            "HISTORY" -> viewModel.openSheet(SheetType.HISTORY)
                            "DOWNLOADS" -> viewModel.openSheet(SheetType.DOWNLOADS)
                            "EXTENSIONS" -> viewModel.openSheet(SheetType.EXTENSIONS)
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
                                settings = settings,
                                tabWebViewManager = viewModel.tabWebViewManager,
                                extensions = extensions,
                                isExtensionsGlobalEnabled = extensionsGlobalEnabled,
                                gmBridge = viewModel.gmBridge,
                                onPageStarted = { viewModel.onPageStarted(activeTab.id, it) },
                                onPageFinished = { url, title, certInfo ->
                                    viewModel.onPageFinished(activeTab.id, url, title, certInfo)
                                },
                                onProgressChanged = { viewModel.onProgressChanged(activeTab.id, it) },
                                onTitleReceived = { viewModel.onTitleReceived(activeTab.id, it) },
                                onFaviconReceived = { viewModel.onFaviconReceived(activeTab.id, it) },
                                onNavigationStateChanged = { canBack, canFwd ->
                                    viewModel.onNavigationStateChanged(activeTab.id, canBack, canFwd)
                                },
                                onTrackerBlocked = { viewModel.onTrackerBlocked(activeTab.id) },
                                onExtensionsCountChanged = { count ->
                                    viewModel.onExtensionsCountUpdated(activeTab.id, count)
                                },
                                onInterceptUserscript = { url ->
                                    viewModel.fetchAndPromptUserscript(url)
                                },
                                onDownloadStart = { url, disposition, mime, ua ->
                                    viewModel.startDownload(url, disposition, mime, ua)
                                },
                                onOpenNewTabRequested = { targetUrl ->
                                    viewModel.createNewTab(url = if (targetUrl.isNotEmpty()) targetUrl else "velocity://newtab")
                                },
                                onShowCustomView = { view, callback ->
                                    viewModel.setCustomView(view, callback)
                                },
                                onHideCustomView = {
                                    viewModel.hideCustomView()
                                },
                                onSslErrorOccurred = { warningState ->
                                    viewModel.setSslWarning(warningState)
                                },
                                onPermissionRequested = { permState ->
                                    viewModel.setWebPermission(permState)
                                },
                                findInPageQuery = findInPageQuery,
                                onWebViewReady = { currentWebView = it },
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

            // Fullscreen Custom View (HTML5 Video overlay)
            customViewState?.let { customView ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { customView.view },
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
                        onOpenDownload = { viewModel.openDownloadedFile(it) },
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
                    val info = currentCertificateInfo ?: CertificateInfo(
                        domain = activeTab?.url ?: "Website",
                        issuedTo = activeTab?.title ?: "Standard Connection",
                        issuedBy = if (activeTab?.isSecure == true) "TLS Verified" else "Unencrypted",
                        validFrom = "Current",
                        validTo = "Current",
                        isSecure = activeTab?.isSecure == true
                    )
                    SslDialog(
                        info = info,
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.CLEAR_DATA -> {
                    ClearDataDialog(
                        onClear = { cache, cookies, historyToClear ->
                            viewModel.clearBrowsingData(cache, cookies, historyToClear)
                        },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.EXTENSIONS -> {
                    ExtensionsScreen(
                        extensions = extensions,
                        isGlobalEnabled = extensionsGlobalEnabled,
                        onToggleGlobal = { viewModel.toggleExtensionsGlobal(it) },
                        onToggleExtension = { id, enabled -> viewModel.toggleExtension(id, enabled) },
                        onDeleteExtension = { viewModel.deleteExtension(it) },
                        onSaveExtension = { viewModel.saveExtension(it) },
                        onInstallFromUrl = { viewModel.installFromUrl(it) },
                        onSelectExtension = { ext ->
                            viewModel.selectExtensionForDetail(ext)
                            viewModel.openSheet(SheetType.EXTENSION_DETAIL)
                        },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.EXTENSION_DETAIL -> {
                    selectedExtensionForDetail?.let { ext ->
                        val currentSiteHost = activeTab?.url?.let { UriHelper.extractDomain(it) }
                        ExtensionDetailScreen(
                            extension = ext,
                            currentSiteHost = currentSiteHost,
                            onSaveExtension = { updated ->
                                viewModel.saveExtension(updated)
                                viewModel.selectExtensionForDetail(updated)
                            },
                            onDismiss = {
                                viewModel.openSheet(SheetType.EXTENSIONS)
                            }
                        )
                    }
                }
                else -> { /* No active sheet */ }
            }

            // Userscript Install Confirmation Dialog
            pendingInstallScript?.let { scriptToInstall ->
                InstallUserscriptDialog(
                    extension = scriptToInstall,
                    onInstall = {
                        viewModel.saveExtension(scriptToInstall)
                        viewModel.dismissInstallDialog()
                        viewModel.showToast("Installed ${scriptToInstall.name}")
                    },
                    onDismiss = { viewModel.dismissInstallDialog() }
                )
            }

            // SSL Security Warning Dialog (Cancel by default for untrusted certs)
            sslWarningState?.let { warning ->
                SslWarningDialog(
                    warning = warning,
                    onProceed = { viewModel.setSslWarning(null) },
                    onCancel = { viewModel.setSslWarning(null) }
                )
            }

            // Web Permissions Request Dialog (Camera, Mic, Geolocation)
            webPermissionState?.let { perm ->
                WebPermissionDialog(
                    permission = perm,
                    onAllow = {
                        when (perm) {
                            is WebPermissionState.ResourcePermission -> perm.request.grant(perm.resources)
                            is WebPermissionState.GeolocationPermission -> perm.callback.invoke(perm.origin, true, false)
                        }
                        viewModel.setWebPermission(null)
                    },
                    onDeny = {
                        when (perm) {
                            is WebPermissionState.ResourcePermission -> perm.request.deny()
                            is WebPermissionState.GeolocationPermission -> perm.callback.invoke(perm.origin, false, false)
                        }
                        viewModel.setWebPermission(null)
                    }
                )
            }
        }
    }
}
