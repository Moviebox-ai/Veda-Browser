package com.aaryo.vedabrowser

import androidx.lifecycle.viewModelScope
import com.aaryo.vedabrowser.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

/** Activity lifecycle adapters kept outside the view model's core UI logic. */
fun BrowserViewModel.onActivityResume() {
    tabWebViewManager.warmupSpareWebView(getApplication(), settings.value)
    onActivityResumeDownloads()
}

fun BrowserViewModel.onActivityStop() {
    tabWebViewManager.saveAllStates()
}

private fun BrowserViewModel.onActivityResumeDownloads() {
    viewModelScope.launch {
        realDownloadManager.triggerStorageMigration()
    }
}
