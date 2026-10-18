package com.example.domain.repository

import com.example.data.local.preferences.BrowserSettings
import com.example.domain.model.Bookmark
import com.example.domain.model.HistoryItem
import com.example.domain.model.Shortcut
import kotlinx.coroutines.flow.Flow

interface BrowserRepository {
    // Settings
    val settingsFlow: Flow<BrowserSettings>
    suspend fun updateSettings(update: suspend (com.example.data.local.preferences.UserPreferencesDataStore) -> Unit)
    suspend fun setSearchEngine(engineId: String)
    suspend fun setContentBlocker(enabled: Boolean)
    suspend fun setJavascriptEnabled(enabled: Boolean)
    suspend fun setDesktopModeDefault(enabled: Boolean)
    suspend fun setHttpsFirst(enabled: Boolean)
    suspend fun setBottomToolbar(bottom: Boolean)
    suspend fun setDarkThemeMode(mode: String)
    suspend fun setSaveHistory(save: Boolean)
    suspend fun setTextZoom(zoom: Int)

    // Bookmarks
    fun getAllBookmarks(): Flow<List<Bookmark>>
    fun getBookmarksByFolder(folder: String): Flow<List<Bookmark>>
    fun getAllFolders(): Flow<List<String>>
    fun searchBookmarks(query: String): Flow<List<Bookmark>>
    fun isBookmarked(url: String): Flow<Boolean>
    suspend fun addBookmark(title: String, url: String, folder: String = "Default"): Long
    suspend fun deleteBookmark(bookmark: Bookmark)
    suspend fun deleteBookmarkByUrl(url: String)

    // History
    fun getAllHistory(): Flow<List<HistoryItem>>
    fun getRecentHistory(limit: Int = 20): Flow<List<HistoryItem>>
    fun getFrequentlyVisited(limit: Int = 8): Flow<List<HistoryItem>>
    fun searchHistory(query: String): Flow<List<HistoryItem>>
    suspend fun recordVisit(title: String, url: String)
    suspend fun deleteHistoryItem(id: Long)
    suspend fun clearHistory()
    suspend fun pruneOldHistory(maxAgeDays: Int = 90)

    // Shortcuts
    fun getAllShortcuts(): Flow<List<Shortcut>>
    suspend fun addShortcut(title: String, url: String): Long
    suspend fun deleteShortcut(shortcut: Shortcut)
}
