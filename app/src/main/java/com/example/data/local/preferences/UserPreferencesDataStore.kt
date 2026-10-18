package com.example.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.SearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "velocity_settings")

data class BrowserSettings(
    val searchEngineId: String = "duckduckgo",
    val customSearchEngineTemplate: String = "https://duckduckgo.com/?q=%s",
    val contentBlockerEnabled: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val desktopModeDefault: Boolean = false,
    val httpsFirst: Boolean = true,
    val bottomToolbar: Boolean = false,
    val darkThemeMode: String = "system", // system, light, dark
    val saveHistory: Boolean = true,
    val textZoom: Int = 100,
    val homepageUrl: String = "velocity:home"
) {
    val searchEngine: SearchEngine
        get() = SearchEngine.fromId(searchEngineId)
}

class UserPreferencesDataStore(private val context: Context) {

    private object PreferencesKeys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val CUSTOM_SEARCH_TEMPLATE = stringPreferencesKey("custom_search_template")
        val CONTENT_BLOCKER = booleanPreferencesKey("content_blocker")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val DESKTOP_MODE_DEFAULT = booleanPreferencesKey("desktop_mode_default")
        val HTTPS_FIRST = booleanPreferencesKey("https_first")
        val BOTTOM_TOOLBAR = booleanPreferencesKey("bottom_toolbar")
        val DARK_THEME_MODE = stringPreferencesKey("dark_theme_mode")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val TEXT_ZOOM = intPreferencesKey("text_zoom")
        val HOMEPAGE_URL = stringPreferencesKey("homepage_url")
    }

    val settingsFlow: Flow<BrowserSettings> = context.dataStore.data.map { preferences ->
        BrowserSettings(
            searchEngineId = preferences[PreferencesKeys.SEARCH_ENGINE] ?: "duckduckgo",
            customSearchEngineTemplate = preferences[PreferencesKeys.CUSTOM_SEARCH_TEMPLATE] ?: "https://duckduckgo.com/?q=%s",
            contentBlockerEnabled = preferences[PreferencesKeys.CONTENT_BLOCKER] ?: true,
            javascriptEnabled = preferences[PreferencesKeys.JAVASCRIPT_ENABLED] ?: true,
            desktopModeDefault = preferences[PreferencesKeys.DESKTOP_MODE_DEFAULT] ?: false,
            httpsFirst = preferences[PreferencesKeys.HTTPS_FIRST] ?: true,
            bottomToolbar = preferences[PreferencesKeys.BOTTOM_TOOLBAR] ?: false,
            darkThemeMode = preferences[PreferencesKeys.DARK_THEME_MODE] ?: "system",
            saveHistory = preferences[PreferencesKeys.SAVE_HISTORY] ?: true,
            textZoom = preferences[PreferencesKeys.TEXT_ZOOM] ?: 100,
            homepageUrl = preferences[PreferencesKeys.HOMEPAGE_URL] ?: "velocity:home"
        )
    }

    suspend fun setSearchEngine(engineId: String) {
        context.dataStore.edit { it[PreferencesKeys.SEARCH_ENGINE] = engineId }
    }

    suspend fun setContentBlocker(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CONTENT_BLOCKER] = enabled }
    }

    suspend fun setJavascriptEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun setDesktopModeDefault(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DESKTOP_MODE_DEFAULT] = enabled }
    }

    suspend fun setHttpsFirst(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HTTPS_FIRST] = enabled }
    }

    suspend fun setBottomToolbar(bottom: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BOTTOM_TOOLBAR] = bottom }
    }

    suspend fun setDarkThemeMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.DARK_THEME_MODE] = mode }
    }

    suspend fun setSaveHistory(save: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SAVE_HISTORY] = save }
    }

    suspend fun setTextZoom(zoom: Int) {
        context.dataStore.edit { it[PreferencesKeys.TEXT_ZOOM] = zoom }
    }

    suspend fun setHomepageUrl(url: String) {
        context.dataStore.edit { it[PreferencesKeys.HOMEPAGE_URL] = url }
    }
}
