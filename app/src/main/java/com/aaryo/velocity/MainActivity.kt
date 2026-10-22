package com.aaryo.velocity

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aaryo.velocity.ui.screens.BrowserScreen
import com.aaryo.velocity.ui.theme.VelocityTheme
import com.aaryo.velocity.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        VelocityApplication.cleanCorruptedVariationsSeeds(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            VelocityTheme(
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

    override fun onStop() {
        super.onStop()
        viewModel.onActivityStop()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
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
