package com.example

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.util.AppLogger
import com.example.util.DownloadNotificationHelper
import com.example.util.UrlResolver
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeedTestScreen
import com.example.ui.screens.TabSwitcherScreen
import com.example.ui.theme.VelocityTheme
import com.example.ui.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels {
        BrowserViewModel.Factory(application)
    }

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        AppLogger.d("MainActivity", "POST_NOTIFICATIONS granted: $isGranted")
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = uri.lastPathSegment ?: "file"
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val displayName = cursor.getString(nameIndex)
                        if (!displayName.isNullOrBlank()) {
                            name = displayName
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return name
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val selectedUris = if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                val uriList = mutableListOf<Uri>()
                for (i in 0 until count) {
                    data.clipData!!.getItemAt(i).uri?.let { uriList.add(it) }
                }
                uriList.toTypedArray()
            } else if (data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }

            // Removed misleading upload notification on mere file selection
            selectedUris
        } else {
            null
        }
        fileChooserCallback?.onReceiveValue(uris)
        fileChooserCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels and request notification permissions on Android 13+
        DownloadNotificationHelper.createChannels(this)
        requestNotificationPermissionIfNeeded()

        // Allow viewModel to request notification permission when downloads start
        viewModel.onRequestNotificationPermission = {
            requestNotificationPermissionIfNeeded()
        }

        // Wire up file chooser handler for WebView
        viewModel.onShowFileChooserHandler = { callback, params ->
            requestNotificationPermissionIfNeeded()
            fileChooserCallback?.onReceiveValue(null)
            fileChooserCallback = callback
            val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            try {
                fileChooserLauncher.launch(intent)
                true
            } catch (_: Exception) {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = null
                false
            }
        }

        // Guard against duplicate tab creation during configuration changes
        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            VelocityTheme(themeMode = settings.darkThemeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VelocityNavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        viewModel.onShowFileChooserHandler = null
        viewModel.onRequestNotificationPermission = null
        viewModel.webViewManager.detachAllContexts(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val navTarget = intent.getStringExtra("navigate_to")
        if (!navTarget.isNullOrBlank()) {
            viewModel.navigationEvent.value = navTarget
        }

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.dataString?.let { rawUrl ->
                    val uri = intent.data
                    val scheme = uri?.scheme?.lowercase()
                    // Strictly allow only http and https from external apps
                    if (scheme == "http" || scheme == "https") {
                        val resolved = UrlResolver.resolve(rawUrl, viewModel.settings.value.searchEngine)
                        if (resolved.type != UrlResolver.ResolvedType.BLOCKED_SCHEME) {
                            viewModel.openNewTab(url = resolved.url, isPrivate = false)
                        }
                    } else {
                        AppLogger.w("MainActivity", "Blocked external intent with unsafe scheme: $scheme")
                    }
                }
            }
            Intent.ACTION_WEB_SEARCH -> {
                intent.getStringExtra(SearchManager.QUERY)?.let { query ->
                    if (query.isNotBlank()) {
                        val searchUrl = viewModel.settings.value.searchEngine.buildSearchUrl(query)
                        viewModel.openNewTab(url = searchUrl, isPrivate = false)
                    }
                }
            }
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        if (text.isNotBlank()) {
                            val resolved = UrlResolver.resolve(text, viewModel.settings.value.searchEngine)
                            if (resolved.type != UrlResolver.ResolvedType.BLOCKED_SCHEME) {
                                viewModel.openNewTab(url = resolved.url, isPrivate = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VelocityNavHost(viewModel: BrowserViewModel) {
    val navController = rememberNavController()

    val navEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
    LaunchedEffect(navEvent) {
        if (navEvent == "downloads") {
            navController.navigate(Screen.Downloads.route)
            viewModel.navigationEvent.value = null
        }
    }

    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val downloads by viewModel.downloadRepo.getAllDownloads().collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Browser.route
    ) {
        composable(Screen.Browser.route) {
            BrowserScreen(
                viewModel = viewModel,
                onOpenTabs = { navController.navigate(Screen.Tabs.route) },
                onOpenBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                onOpenHistory = { navController.navigate(Screen.History.route) },
                onOpenDownloads = { navController.navigate(Screen.Downloads.route) },
                onOpenSpeedTest = { navController.navigate(Screen.SpeedTest.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Tabs.route) {
            TabSwitcherScreen(
                tabs = tabs,
                activeTabId = activeTabId,
                onSelectTab = { tabId ->
                    viewModel.selectTab(tabId)
                },
                onCloseTab = { tabId ->
                    viewModel.closeTab(tabId)
                },
                onCloseAll = { privateOnly ->
                    viewModel.closeAllTabs(privateOnly)
                },
                onNewTab = { isPrivate ->
                    viewModel.openNewTab(isPrivate = isPrivate)
                    navController.popBackStack()
                },
                onReopenClosed = {
                    viewModel.reopenClosedTab()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                bookmarks = bookmarks,
                onOpenBookmark = { url ->
                    viewModel.navigateTo(url)
                    navController.popBackStack()
                },
                onDeleteBookmark = { bookmark -> viewModel.deleteBookmark(bookmark) },
                onAddBookmark = { title, url -> viewModel.addBookmark(title, url) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                history = history,
                onOpenUrl = { url ->
                    viewModel.navigateTo(url)
                    navController.popBackStack()
                },
                onDeleteItem = { id -> viewModel.deleteHistoryItem(id) },
                onClearAll = { viewModel.clearHistory() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Downloads.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            DownloadsScreen(
                downloads = downloads,
                onOpenFile = { item -> viewModel.downloadRepo.openFile(context, item) },
                onPauseDownload = { id -> viewModel.pauseDownload(id) },
                onResumeDownload = { id -> viewModel.resumeDownload(context, id) },
                onCancelDownload = { id -> viewModel.cancelDownload(id) },
                onDeleteDownload = { item -> viewModel.deleteDownload(context, item) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SpeedTest.route) {
            val speedState by viewModel.speedTestState.collectAsStateWithLifecycle()
            SpeedTestScreen(
                state = speedState,
                onStartTest = { viewModel.runSpeedTest() },
                onReset = { viewModel.resetSpeedTest() },
                onOpenUrl = { url ->
                    viewModel.navigateTo(url)
                    navController.popBackStack(Screen.Browser.route, false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                settings = settings,
                onSetSearchEngine = { engineId -> viewModel.setSearchEngine(engineId) },
                onSetContentBlocker = { enabled -> viewModel.setContentBlocker(enabled) },
                onSetHttpsFirst = { enabled -> viewModel.setHttpsFirst(enabled) },
                onSetBottomToolbar = { enabled -> viewModel.setBottomToolbar(enabled) },
                onSetDarkThemeMode = { mode -> viewModel.setDarkThemeMode(mode) },
                onSetSaveHistory = { enabled -> viewModel.setSaveHistory(enabled) },
                onSetTextZoom = { zoom -> viewModel.setTextZoom(zoom) },
                onClearBrowsingData = { cookies, cache, storage, historyData ->
                    viewModel.clearBrowsingData(cookies, cache, storage, historyData)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
