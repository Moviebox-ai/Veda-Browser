package com.aaryo.velocity.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aaryo.velocity.model.Bookmark
import com.aaryo.velocity.model.BrowserSettings
import com.aaryo.velocity.model.DownloadItem
import com.aaryo.velocity.model.HistoryItem
import com.aaryo.velocity.model.ShieldStats
import com.aaryo.velocity.model.Tab
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "velocity_storage")

data class PersistedTab(
    val id: String,
    val title: String,
    val url: String
)

class BrowserRepository(private val context: Context) {

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val KEY_OPEN_TABS = stringPreferencesKey("key_open_tabs")
        private val KEY_BOOKMARKS = stringPreferencesKey("key_bookmarks")
        private val KEY_HISTORY = stringPreferencesKey("key_history")
        private val KEY_DOWNLOADS = stringPreferencesKey("key_downloads")
        private val KEY_SETTINGS = stringPreferencesKey("key_settings")
        private val KEY_SHIELD_STATS = stringPreferencesKey("key_shield_stats")

        const val MAX_HISTORY_ITEMS = 5000
    }

    // Debounce flow for open tabs saving
    private val tabsSaveFlow = MutableSharedFlow<List<Tab>>(extraBufferCapacity = 1)

    @OptIn(FlowPreview::class)
    val debouncedTabsObserver = scope.launch {
        tabsSaveFlow
            .debounce(300)
            .collect { tabs ->
                writeTabsToDisk(tabs)
            }
    }

    val openTabsFlow: Flow<List<PersistedTab>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val json = preferences[KEY_OPEN_TABS] ?: return@map emptyList()
            try {
                val type = object : TypeToken<List<PersistedTab>>() {}.type
                gson.fromJson<List<PersistedTab>>(json, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

    fun requestSaveTabs(tabs: List<Tab>) {
        tabsSaveFlow.tryEmit(tabs)
    }

    private suspend fun writeTabsToDisk(tabs: List<Tab>) = withContext(Dispatchers.IO) {
        // Persist non-incognito tabs only
        val regularTabs = tabs.filterNot { it.isIncognito }.map {
            PersistedTab(id = it.id, title = it.title, url = it.url)
        }
        context.dataStore.edit { preferences ->
            preferences[KEY_OPEN_TABS] = gson.toJson(regularTabs)
        }
    }

    suspend fun getInitialTabs(): List<PersistedTab> = withContext(Dispatchers.IO) {
        openTabsFlow.first()
    }

    val bookmarksFlow: Flow<List<Bookmark>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val json = preferences[KEY_BOOKMARKS] ?: return@map getDefaultBookmarks()
            try {
                val type = object : TypeToken<List<Bookmark>>() {}.type
                gson.fromJson<List<Bookmark>>(json, type) ?: getDefaultBookmarks()
            } catch (_: Exception) {
                getDefaultBookmarks()
            }
        }

    private fun getDefaultBookmarks(): List<Bookmark> {
        return listOf(
            Bookmark(title = "DuckDuckGo", url = "https://duckduckgo.com", isSpeedDial = true),
            Bookmark(title = "GitHub", url = "https://github.com", isSpeedDial = true),
            Bookmark(title = "Wikipedia", url = "https://en.wikipedia.org", isSpeedDial = true),
            Bookmark(title = "Hacker News", url = "https://news.ycombinator.com", isSpeedDial = true),
            Bookmark(title = "Reddit", url = "https://reddit.com", isSpeedDial = true),
            Bookmark(title = "YouTube", url = "https://youtube.com", isSpeedDial = true)
        )
    }

    suspend fun addBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        val current = bookmarksFlow.first().toMutableList()
        current.removeAll { it.url == bookmark.url }
        current.add(0, bookmark)
        context.dataStore.edit { preferences ->
            preferences[KEY_BOOKMARKS] = gson.toJson(current)
        }
    }

    suspend fun removeBookmark(id: String) = withContext(Dispatchers.IO) {
        val updated = bookmarksFlow.first().filterNot { it.id == id }
        context.dataStore.edit { preferences ->
            preferences[KEY_BOOKMARKS] = gson.toJson(updated)
        }
    }

    val historyFlow: Flow<List<HistoryItem>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val json = preferences[KEY_HISTORY] ?: return@map emptyList()
            try {
                val type = object : TypeToken<List<HistoryItem>>() {}.type
                gson.fromJson<List<HistoryItem>>(json, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

    suspend fun addHistoryItem(item: HistoryItem) = withContext(Dispatchers.IO) {
        val current = historyFlow.first().toMutableList()
        current.removeAll { it.url == item.url }
        current.add(0, item)

        // Enforce 5000 item cap on browsing history
        val cappedList = if (current.size > MAX_HISTORY_ITEMS) {
            current.subList(0, MAX_HISTORY_ITEMS)
        } else {
            current
        }

        context.dataStore.edit { preferences ->
            preferences[KEY_HISTORY] = gson.toJson(cappedList)
        }
    }

    suspend fun removeHistoryItem(id: String) = withContext(Dispatchers.IO) {
        val updated = historyFlow.first().filterNot { it.id == id }
        context.dataStore.edit { preferences ->
            preferences[KEY_HISTORY] = gson.toJson(updated)
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_HISTORY)
        }
    }

    val downloadsFlow: Flow<List<DownloadItem>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val json = preferences[KEY_DOWNLOADS] ?: return@map emptyList()
            try {
                val type = object : TypeToken<List<DownloadItem>>() {}.type
                gson.fromJson<List<DownloadItem>>(json, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

    suspend fun saveDownloads(downloads: List<DownloadItem>) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DOWNLOADS] = gson.toJson(downloads)
        }
    }

    suspend fun addOrUpdateDownload(download: DownloadItem) = withContext(Dispatchers.IO) {
        val current = downloadsFlow.first().toMutableList()
        val index = current.indexOfFirst { it.id == download.id }
        if (index >= 0) {
            current[index] = download
        } else {
            current.add(0, download)
        }
        context.dataStore.edit { preferences ->
            preferences[KEY_DOWNLOADS] = gson.toJson(current)
        }
    }

    suspend fun clearDownloads() = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_DOWNLOADS)
        }
    }

    val settingsFlow: Flow<BrowserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val json = preferences[KEY_SETTINGS] ?: return@map BrowserSettings()
            try {
                gson.fromJson(json, BrowserSettings::class.java) ?: BrowserSettings()
            } catch (_: Exception) {
                BrowserSettings()
            }
        }

    suspend fun saveSettings(settings: BrowserSettings) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SETTINGS] = gson.toJson(settings)
        }
    }

    val shieldStatsFlow: Flow<ShieldStats> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val json = preferences[KEY_SHIELD_STATS] ?: return@map ShieldStats()
            try {
                gson.fromJson(json, ShieldStats::class.java) ?: ShieldStats()
            } catch (_: Exception) {
                ShieldStats()
            }
        }

    suspend fun incrementShieldStats(count: Int) = withContext(Dispatchers.IO) {
        val current = shieldStatsFlow.first()
        val updated = current.copy(
            totalAdsBlocked = current.totalAdsBlocked + count,
            totalTrackersBlocked = current.totalTrackersBlocked + count,
            totalBandwidthSavedMb = current.totalBandwidthSavedMb + (count * 0.15f),
            totalTimeSavedSeconds = current.totalTimeSavedSeconds + (count * 0.25f)
        )
        context.dataStore.edit { preferences ->
            preferences[KEY_SHIELD_STATS] = gson.toJson(updated)
        }
    }
}
