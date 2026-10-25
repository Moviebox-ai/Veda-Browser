package com.aaryo.vedabrowser

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aaryo.vedabrowser.ui.screens.BrowserScreen
import com.aaryo.vedabrowser.ui.theme.VedaTheme
import com.aaryo.vedabrowser.viewmodel.BrowserViewModel
import com.aaryo.vedabrowser.viewmodel.SheetType

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Notifications enabled or denied by user
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        VedaApplication.cleanCorruptedVariationsSeeds(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            VedaTheme(
                themeMode = settings.themeMode,
                accentColorHex = settings.accentColorHex
            ) {
                BrowserScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun enterPipMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Throwable) {
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setInPipMode(isInPictureInPictureMode)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.customViewState.value != null) {
            enterPipMode()
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onActivityStop()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.getStringExtra("OPEN_SCREEN") == "DOWNLOADS") {
            viewModel.openSheet(SheetType.DOWNLOADS)
            return
        }

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val url = intent.dataString
                if (!url.isNullOrEmpty()) {
                    viewModel.createNewTab(url = url)
                }
            }
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.createNewTab(url = sharedText.trim())
                    }
                }
            }
            Intent.ACTION_WEB_SEARCH -> {
                val query = intent.getStringExtra(SearchManager.QUERY)
                    ?: intent.getStringExtra("query")
                    ?: intent.dataString
                if (!query.isNullOrBlank()) {
                    viewModel.createNewTab(url = query.trim())
                }
            }
        }
    }
}
