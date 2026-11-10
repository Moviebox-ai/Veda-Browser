package com.aaryo.vedabrowser.ui.screens

import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.viewModelScope
import com.aaryo.vedabrowser.R
import com.aaryo.vedabrowser.model.Extension
import com.aaryo.vedabrowser.model.QuickDialShortcut
import com.aaryo.vedabrowser.model.SavedCredential
import com.aaryo.vedabrowser.extension.UserscriptParser
import com.aaryo.vedabrowser.viewmodel.BrowserViewModel
import com.aaryo.vedabrowser.viewmodel.CustomViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

private fun <T> BrowserViewModel.setPrivateState(fieldName: String, value: T) {
    try {
        val field = BrowserViewModel::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        val flow = field.get(this) as? kotlinx.coroutines.flow.MutableStateFlow<T>
        flow?.value = value
    } catch (_: Exception) {
        // A missing private state must not crash UI event handling.
    }
}

fun BrowserViewModel.dismissToast() = clearToast()

fun BrowserViewModel.hideCustomView() {
    customViewState.value?.callback?.onCustomViewHidden()
    setPrivateState("_customViewState", null as CustomViewState?)
}

fun BrowserViewModel.setCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
    setPrivateState("_customViewState", CustomViewState(view, callback))
}

fun BrowserViewModel.setFindInPageVisible(visible: Boolean) {
    setPrivateState("_isFindInPageVisible", visible)
    if (!visible) setFindInPageQuery("")
}

fun BrowserViewModel.setFindInPageQuery(query: String) {
    setPrivateState("_findInPageQuery", query)
}

fun BrowserViewModel.navigateToHome() {
    navigateTo(settings.value.homepageUrl)
}

fun BrowserViewModel.translateCurrentPage() {
    val tab = getActiveTab() ?: return
    if (tab.isNewTab || tab.url.isBlank()) return
    val encoded = URLEncoder.encode(tab.url, Charsets.UTF_8.name())
    navigateTo("https://translate.google.com/translate?sl=auto&tl=hi&u=$encoded")
}

fun BrowserViewModel.addShortcutToHomeScreen(title: String, url: String) {
    val app = getApplication<android.app.Application>()
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(app)) {
        showToast("Home screen shortcuts are not supported on this launcher")
        return
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setClass(app, com.aaryo.vedabrowser.MainActivity::class.java)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    val shortcut = ShortcutInfoCompat.Builder(app, "veda_${System.currentTimeMillis()}")
        .setShortLabel(title.take(15).ifBlank { "Veda" })
        .setLongLabel(title.ifBlank { url })
        .setIcon(IconCompat.createWithResource(app, R.mipmap.ic_launcher))
        .setIntent(intent)
        .build()
    if (ShortcutManagerCompat.requestPinShortcut(app, shortcut, null)) {
        showToast("Shortcut added to Home Screen")
    } else {
        showToast("Could not add shortcut")
    }
}

fun BrowserViewModel.addQuickDial(shortcut: QuickDialShortcut) {
    viewModelScope.launch { repository.addQuickDial(shortcut) }
}

fun BrowserViewModel.removeQuickDial(id: String) {
    viewModelScope.launch { repository.removeQuickDial(id) }
}

fun BrowserViewModel.onExtensionsCountUpdated(tabId: String, count: Int) {
    updateTab(tabId) { it.copy(activeExtensionsCount = count) }
}

fun BrowserViewModel.fetchAndPromptUserscript(url: String) {
    viewModelScope.launch {
        try {
            val extension = withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
                OkHttpClient().newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful || body.isNullOrBlank()) null
                    else UserscriptParser.parse(body).copy(updateUrl = url)
                }
            }
            if (extension != null) {
                setPrivateState("_pendingInstallScript", extension)
            } else {
                showToast("Could not fetch userscript")
            }
        } catch (e: Exception) {
            showToast("Could not fetch userscript: ${e.localizedMessage}")
        }
    }
}

fun BrowserViewModel.addCredential(credential: SavedCredential) {
    viewModelScope.launch { repository.addCredential(credential) }
}

fun BrowserViewModel.removeCredential(id: String) {
    viewModelScope.launch { repository.removeCredential(id) }
}

fun BrowserViewModel.toggleExtensionsGlobal(enabled: Boolean) {
    viewModelScope.launch { extensionRepository.setGlobalEnabled(enabled) }
}

fun BrowserViewModel.toggleExtension(id: String, enabled: Boolean) {
    viewModelScope.launch { extensionRepository.toggleExtension(id, enabled) }
}

fun BrowserViewModel.deleteExtension(id: String) {
    viewModelScope.launch { extensionRepository.deleteExtension(id) }
}

fun BrowserViewModel.saveExtension(extension: Extension) {
    viewModelScope.launch { extensionRepository.saveExtension(extension) }
}

fun BrowserViewModel.installFromUrl(url: String) {
    fetchAndPromptUserscript(url)
}

fun BrowserViewModel.selectExtensionForDetail(extension: Extension?) {
    setPrivateState("_selectedExtensionForDetail", extension)
}

fun BrowserViewModel.dismissInstallDialog() {
    setPrivateState("_pendingInstallScript", null as Extension?)
}
