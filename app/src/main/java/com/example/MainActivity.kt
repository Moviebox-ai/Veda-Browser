package com.example

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
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
        } else {
            null
        }
        fileChooserCallback?.onReceiveValue(uris)
        fileChooserCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Wire up high-speed file upload handler for WebView
        viewModel.onShowFileChooserHandler = { callback, params ->
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

        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            VelocityTheme(themeMode = settings.darkThemeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VelocityNavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.dataString?.let { url ->
                    if (url.isNotBlank()) {
                        viewModel.openNewTab(url = url, isPrivate = false)
                    }
                }
            }
            Intent.ACTION_WEB_SEARCH -> {
                intent.getStringExtra(SearchManager.QUERY)?.let { query ->
                    if (query.isNotBlank()) {
                        viewModel.openNewTab(url = query, isPrivate = false)
                    }
                }
            }
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        if (text.isNotBlank()) {
                            viewModel.openNewTab(url = text, isPrivate = false)
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
