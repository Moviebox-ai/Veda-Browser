package com.aaryo.vedabrowser.ui.screens

import android.content.Intent
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import com.aaryo.vedabrowser.model.DownloadItem
import com.aaryo.vedabrowser.ui.dialogs.StoragePermissionDialog
import com.aaryo.vedabrowser.util.StoragePermissionHelper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.aaryo.vedabrowser.MainActivity
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aaryo.vedabrowser.security.CertificateInfo
import com.aaryo.vedabrowser.ui.components.DownloadBanner
import com.aaryo.vedabrowser.ui.components.FindInPageBar
import com.aaryo.vedabrowser.ui.components.Omnibox
import com.aaryo.vedabrowser.ui.components.VelocityMenuSheet
import com.aaryo.vedabrowser.ui.components.WebViewContainer
import com.aaryo.vedabrowser.ui.dialogs.AiAssistantDialog
import com.aaryo.vedabrowser.ui.dialogs.ChromiumPerformanceHudDialog
import com.aaryo.vedabrowser.ui.dialogs.ClearDataDialog
import com.aaryo.vedabrowser.ui.dialogs.InstallUserscriptDialog
import com.aaryo.vedabrowser.ui.dialogs.MediaSnifferBottomSheet
import com.aaryo.vedabrowser.ui.dialogs.ShieldDialog
import com.aaryo.vedabrowser.ui.dialogs.SslDialog
import com.aaryo.vedabrowser.ui.dialogs.SslWarningDialog
import com.aaryo.vedabrowser.ui.dialogs.WebPermissionDialog
import com.aaryo.vedabrowser.ui.dialogs.WebPermissionState
import com.aaryo.vedabrowser.ui.screens.AppearanceScreen
import com.aaryo.vedabrowser.ui.screens.ExtensionDetailScreen
import com.aaryo.vedabrowser.ui.screens.ExtensionsScreen
import com.aaryo.vedabrowser.ui.screens.PasswordsScreen
import com.aaryo.vedabrowser.viewmodel.BrowserViewModel
import com.aaryo.vedabrowser.viewmodel.SheetType
import com.aaryo.vedabrowser.viewmodel.UriHelper

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
    val downloadBanner by viewModel.downloadBanner.collectAsState()
    val nonResumablePrompt by viewModel.nonResumableDownloadPrompt.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val shieldStats by viewModel.shieldStats.collectAsState()
    val sslWarningState by viewModel.sslWarningState.collectAsState()
    val currentCertificateInfo by viewModel.currentCertificateInfo.collectAsState()
    val webPermissionState by viewModel.webPermissionState.collectAsState()
    val customViewState by viewModel.customViewState.collectAsState()
    val isFindInPageVisible by viewModel.isFindInPageVisible.collectAsState()
    val findInPageQuery by viewModel.findInPageQuery.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val isInPipMode by viewModel.isInPipMode.collectAsState()
    val showStoragePermissionDialog by viewModel.showStoragePermissionDialog.collectAsState()

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true ||
            permissions.values.any { it }
        viewModel.onStoragePermissionResult(isGranted)
    }

    val requestStorageAccess: () -> Unit = {
        if (StoragePermissionHelper.isStoragePermissionRequired() &&
            !StoragePermissionHelper.hasStoragePermission(context)
        ) {
            viewModel.setShowStoragePermissionDialog(true)
        } else {
            viewModel.onStoragePermissionResult(true)
        }
    }

    val extensions by viewModel.extensions.collectAsState()
    val extensionsGlobalEnabled by viewModel.extensionsGlobalEnabled.collectAsState()
    val selectedExtensionForDetail by viewModel.selectedExtensionForDetail.collectAsState()
    val pendingInstallScript by viewModel.pendingInstallScript.collectAsState()
    val credentials by viewModel.credentials.collectAsState()
    val quickDials by viewModel.quickDials.collectAsState()

    var showSplash by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    var currentWebView by remember { mutableStateOf<WebView?>(null) }

    val activeTab = tabs.find { it.id == activeTabId }
    val sniffedMediaByTab by viewModel.sniffedMediaByTab.collectAsState()
    val currentTabMedia = remember(activeTabId, sniffedMediaByTab) {
        sniffedMediaByTab[activeTabId] ?: emptyList()
    }
    val isCurrentBookmarked = activeTab?.let { tab ->
        bookmarks.any { it.url == tab.url }
    } ?: false

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
        return
    }

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
                if (tabs.size > 1) {
                    viewModel.closeTab(activeTabId)
                } else {
                    viewModel.navigateTo("velocity://newtab")
                }
            }
            tabs.size > 1 -> {
                viewModel.closeTab(activeTabId)
            }
        }
    }

    Scaffold(
        snackbarHost = {},
        contentWindowInsets = WindowInsets.navigationBars,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (activeTab?.isIncognito == true) Color(0xFF0D0A1A) else MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Omnibox / Address Bar (Only shown on real web pages matching Screen 7; Home Page has center search matching Screen 2)
                if (!isInPipMode && activeTab != null && !activeTab.isNewTab) {
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
                        onOpenPerformanceHud = { viewModel.openSheet(SheetType.PERFORMANCE_HUD) },
                        onOpenMediaSniffer = { viewModel.openSheet(SheetType.MEDIA_SNIFFER) },
                        onGoHome = { viewModel.navigateToHome() },
                        onSpeculativeWarm = { viewModel.speculativelyWarmUrl(it) },
                        onMenuAction = { action ->
                            when (action) {
                                "OPEN_MENU" -> viewModel.openSheet(SheetType.MENU)
                                "FORWARD" -> currentWebView?.goForward()
                                "PERFORMANCE_HUD" -> viewModel.openSheet(SheetType.PERFORMANCE_HUD)
                                "MEDIA_SNIFFER" -> viewModel.openSheet(SheetType.MEDIA_SNIFFER)
                                "NEW_TAB" -> viewModel.createNewTab(isIncognito = false)
                                "NEW_INCOGNITO_TAB" -> viewModel.createNewTab(isIncognito = true)
                                "BOOKMARKS" -> viewModel.openSheet(SheetType.BOOKMARKS)
                                "HISTORY" -> viewModel.openSheet(SheetType.HISTORY)
                                "DOWNLOADS" -> viewModel.openSheet(SheetType.DOWNLOADS)
                                "EXTENSIONS" -> viewModel.openSheet(SheetType.EXTENSIONS)
                                "TOGGLE_DESKTOP" -> activeTab.id.let { viewModel.toggleDesktopMode(it) }
                                "FIND_IN_PAGE" -> viewModel.setFindInPageVisible(true)
                                "PIP_MODE" -> (context as? MainActivity)?.enterPipMode()
                                "SHIELD" -> viewModel.openSheet(SheetType.SHIELD_INFO)
                                "SETTINGS" -> viewModel.openSheet(SheetType.SETTINGS)
                                "AI_ASSISTANT" -> viewModel.openSheet(SheetType.AI_ASSISTANT)
                                "PASSWORDS" -> viewModel.openSheet(SheetType.PASSWORDS)
                                "APPEARANCE" -> viewModel.openSheet(SheetType.APPEARANCE)
                                "TAB_SWITCHER" -> viewModel.openSheet(SheetType.TAB_SWITCHER)
                                "TRANSLATE" -> viewModel.translateCurrentPage()
                                "RATE_APP" -> viewModel.showToast("Thanks for rating Veda Browser! ★★★★★")
                                "ADD_HOME_SCREEN" -> {
                                    viewModel.addShortcutToHomeScreen(activeTab.title, activeTab.url)
                                }
                                "SHARE" -> {
                                    val sendIntent = Intent().apply {
                                        this.action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, activeTab.url)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Web Page"))
                                }
                            }
                        },
                        modifier = Modifier.statusBarsPadding()
                    )
                }

                // Optional Find In Page floating bar
                AnimatedVisibility(
                    visible = !isInPipMode && isFindInPageVisible,
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
                                settings = settings,
                                quickDials = quickDials,
                                tabCount = tabs.size,
                                onAddQuickDial = { viewModel.addQuickDial(it) },
                                onDeleteQuickDial = { viewModel.removeQuickDial(it) },
                                onNavigate = { viewModel.navigateTo(it) },
                                onOpenTabs = { viewModel.openSheet(SheetType.TAB_SWITCHER) },
                                onOpenMenu = { viewModel.openSheet(SheetType.MENU) },
                                onOpenBookmarks = { viewModel.openSheet(SheetType.BOOKMARKS) },
                                onOpenHistory = { viewModel.openSheet(SheetType.HISTORY) },
                                onOpenDownloads = { viewModel.openSheet(SheetType.DOWNLOADS) },
                                onOpenSettings = { viewModel.openSheet(SheetType.SETTINGS) },
                                onOpenShield = { viewModel.openSheet(SheetType.SHIELD_INFO) },
                                onSpeculativeWarm = { viewModel.speculativelyWarmUrl(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            key(activeTab.id) {
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
                                    onMetricsReceived = { metrics ->
                                        viewModel.onMetricsReceived(activeTab.id, metrics)
                                    },
                                    onMediaDetected = { url, pageUrl, pageTitle, mime, type ->
                                        viewModel.onMediaDetected(activeTab.id, url, pageUrl, pageTitle, mime, type)
                                    },
                                    onClearMedia = {
                                        viewModel.clearSniffedMedia(activeTab.id)
                                    },
                                    onExtensionsCountChanged = { count ->
                                        viewModel.onExtensionsCountUpdated(activeTab.id, count)
                                    },
                                    onInterceptUserscript = { url ->
                                        viewModel.fetchAndPromptUserscript(url)
                                    },
                                    onDownloadStart = { url, disposition, mime, ua ->
                                        if (StoragePermissionHelper.hasStoragePermission(context)) {
                                            viewModel.startDownload(url, disposition, mime, ua)
                                        } else {
                                            viewModel.preparePendingDownload(url, disposition, mime, ua)
                                            requestStorageAccess()
                                        }
                                    },
                                    onOpenNewTabRequested = { targetUrl ->
                                        viewModel.createNewTab(
                                            url = if (targetUrl.isNotEmpty()) targetUrl else "velocity://newtab",
                                            isIncognito = activeTab.isIncognito
                                        )
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
                }
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

                    if (!isInPipMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.hideCustomView() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Fullscreen Video",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { (context as? MainActivity)?.enterPipMode() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Enter Picture-in-Picture",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Secondary Sheet Overlays & Dialogs (Hidden in Picture-in-Picture mode)
            if (!isInPipMode) {
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
                        onPauseDownload = { viewModel.pauseDownload(it) },
                        onResumeDownload = { item, forceRestart -> viewModel.resumeDownload(item, forceRestart) },
                        onShareDownload = { viewModel.shareDownloadedFile(it) },
                        onRemoveDownload = { viewModel.removeDownload(it) },
                        onOpenFolder = { viewModel.openDownloadsFolder() },
                        onClearAll = { viewModel.clearDownloads() },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.SETTINGS -> {
                    val isUpdatingFilterLists by viewModel.isUpdatingFilterLists.collectAsState()
                    SettingsScreen(
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onOpenClearData = { viewModel.openSheet(SheetType.CLEAR_DATA) },
                        onOpenAppearance = { viewModel.openSheet(SheetType.APPEARANCE) },
                        onOpenDownloads = { viewModel.openSheet(SheetType.DOWNLOADS) },
                        onRemoveAllowlistedDomain = { viewModel.removeAllowlistedDomain(it) },
                        onUpdateFilterListsNow = { viewModel.updateFilterListsNow() },
                        isUpdatingFilterLists = isUpdatingFilterLists,
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.MENU -> {
                    VelocityMenuSheet(
                        tab = activeTab,
                        isDesktopMode = activeTab?.isDesktopMode == true,
                        tabCount = tabs.size,
                        isBookmarked = isCurrentBookmarked,
                        mediaDetectedCount = currentTabMedia.size,
                        onToggleDesktop = { activeTab?.id?.let { viewModel.toggleDesktopMode(it) } },
                        onMenuAction = { action ->
                            when (action) {
                                "FORWARD" -> currentWebView?.goForward()
                                "RELOAD" -> currentWebView?.reload()
                                "PERFORMANCE_HUD" -> viewModel.openSheet(SheetType.PERFORMANCE_HUD)
                                "MEDIA_SNIFFER" -> viewModel.openSheet(SheetType.MEDIA_SNIFFER)
                                "TOGGLE_BOOKMARK" -> viewModel.toggleBookmarkCurrentTab()
                                "NEW_TAB" -> viewModel.createNewTab(isIncognito = false)
                                "NEW_INCOGNITO_TAB" -> viewModel.createNewTab(isIncognito = true)
                                "BOOKMARKS" -> viewModel.openSheet(SheetType.BOOKMARKS)
                                "HISTORY" -> viewModel.openSheet(SheetType.HISTORY)
                                "DOWNLOADS" -> viewModel.openSheet(SheetType.DOWNLOADS)
                                "EXTENSIONS" -> viewModel.openSheet(SheetType.EXTENSIONS)
                                "PASSWORDS" -> viewModel.openSheet(SheetType.PASSWORDS)
                                "APPEARANCE" -> viewModel.openSheet(SheetType.APPEARANCE)
                                "SETTINGS" -> viewModel.openSheet(SheetType.SETTINGS)
                                "TAB_SWITCHER" -> viewModel.openSheet(SheetType.TAB_SWITCHER)
                                "FIND_IN_PAGE" -> viewModel.setFindInPageVisible(true)
                                "PIP_MODE" -> (context as? MainActivity)?.enterPipMode()
                                "TRANSLATE" -> viewModel.translateCurrentPage()
                                "RATE_APP" -> viewModel.showToast("Thanks for rating Veda Browser! ★★★★★")
                                "SHIELD" -> viewModel.openSheet(SheetType.SHIELD_INFO)
                                "AI_ASSISTANT" -> viewModel.openSheet(SheetType.AI_ASSISTANT)
                                "SHARE" -> {
                                    val shareUrl = activeTab?.url ?: "https://github.com/aaryo/velocity"
                                    val sendIntent = Intent().apply {
                                        this.action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Web Page"))
                                }
                                "ADD_HOME_SCREEN" -> {
                                    activeTab?.let { tab ->
                                        viewModel.addShortcutToHomeScreen(tab.title, tab.url)
                                    }
                                }
                            }
                        },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.AI_ASSISTANT -> {
                    AiAssistantDialog(
                        tab = activeTab,
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.APPEARANCE -> {
                    AppearanceScreen(
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.PASSWORDS -> {
                    PasswordsScreen(
                        credentials = credentials,
                        onAddCredential = { viewModel.addCredential(it) },
                        onDeleteCredential = { viewModel.removeCredential(it) },
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
                SheetType.PERFORMANCE_HUD -> {
                    ChromiumPerformanceHudDialog(
                        tab = activeTab,
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                SheetType.MEDIA_SNIFFER -> {
                    MediaSnifferBottomSheet(
                        mediaItems = currentTabMedia,
                        onDownload = { item ->
                            if (StoragePermissionHelper.hasStoragePermission(context)) {
                                viewModel.downloadSniffedMedia(item)
                            } else {
                                viewModel.preparePendingSniffedMedia(item)
                                requestStorageAccess()
                            }
                        },
                        onCopyUrl = { url -> viewModel.copyMediaUrl(url) },
                        onRescan = { viewModel.rescanMediaForActiveTab() },
                        onDismiss = { viewModel.closeSheet() }
                    )
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

            // Non-resumable download alert dialog
            val currentPromptItem: DownloadItem? = nonResumablePrompt
            if (currentPromptItem != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissNonResumablePrompt() },
                    title = { Text("Resume Not Supported", color = Color.White) },
                    text = {
                        Text(
                            "The server hosting \"${currentPromptItem.fileName}\" does not support resuming interrupted downloads.\n\nWould you like to restart downloading this file from the beginning?",
                            color = Color(0xFF94A3B8)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.resumeDownload(currentPromptItem, forceRestart = true)
                            }
                        ) {
                            Text("Restart from Beginning", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissNonResumablePrompt() }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    },
                    containerColor = Color(0xFF13192B)
                )
            }

            // Storage permission explanation dialog
            if (showStoragePermissionDialog) {
                StoragePermissionDialog(
                    onConfirm = {
                        viewModel.setShowStoragePermissionDialog(false)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            StoragePermissionHelper.requestAllFilesAccess(context)
                            viewModel.onStoragePermissionResult(true)
                        } else {
                            storagePermissionLauncher.launch(
                                StoragePermissionHelper.getRequiredStoragePermissions()
                            )
                        }
                    },
                    onDismiss = {
                        viewModel.setShowStoragePermissionDialog(false)
                    }
                )
            }

            // Top notification overlays (In-app animated download banner card ONLY)
            if (!isInPipMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    DownloadBanner(
                        item = downloadBanner,
                        onOpen = { item -> viewModel.openDownloadedFile(item) },
                        onViewAll = { viewModel.openSheet(SheetType.DOWNLOADS) },
                        onDismiss = { viewModel.dismissDownloadBanner() },
                        onPause = { item -> viewModel.pauseDownload(item) },
                        onResume = { item -> viewModel.resumeDownload(item) }
                    )
                }

                // Non-intrusive bottom snackbar for non-download system notifications (e.g., Bookmarks, URL copied)
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("app_snackbar_host"),
                    snackbar = { snackbarData ->
                        Snackbar(
                            snackbarData = snackbarData,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White,
                            actionColor = Color(0xFF38BDF8),
                            dismissActionContentColor = Color(0xFF94A3B8)
                        )
                    }
                )
            }
        }
    }
}
}
