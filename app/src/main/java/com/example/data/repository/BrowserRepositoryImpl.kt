package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.ShortcutEntity
import com.example.data.local.preferences.BrowserSettings
import com.example.data.local.preferences.UserPreferencesDataStore
import com.example.domain.model.Bookmark
import com.example.domain.model.HistoryItem
import com.example.domain.model.Shortcut
import com.example.domain.repository.BrowserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BrowserRepositoryImpl(
    private val database: AppDatabase,
    private val preferences: UserPreferencesDataStore
) : BrowserRepository {

    override val settingsFlow: Flow<BrowserSettings> = preferences.settingsFlow

    override suspend fun updateSettings(update: suspend (UserPreferencesDataStore) -> Unit) {
        update(preferences)
    }

    override suspend fun setSearchEngine(engineId: String) = preferences.setSearchEngine(engineId)
    override suspend fun setContentBlocker(enabled: Boolean) = preferences.setContentBlocker(enabled)
    override suspend fun setJavascriptEnabled(enabled: Boolean) = preferences.setJavascriptEnabled(enabled)
    override suspend fun setDesktopModeDefault(enabled: Boolean) = preferences.setDesktopModeDefault(enabled)
    override suspend fun setHttpsFirst(enabled: Boolean) = preferences.setHttpsFirst(enabled)
    override suspend fun setBottomToolbar(bottom: Boolean) = preferences.setBottomToolbar(bottom)
    override suspend fun setDarkThemeMode(mode: String) = preferences.setDarkThemeMode(mode)
    override suspend fun setSaveHistory(save: Boolean) = preferences.setSaveHistory(save)
    override suspend fun setTextZoom(zoom: Int) = preferences.setTextZoom(zoom)

    // Bookmarks
    override fun getAllBookmarks(): Flow<List<Bookmark>> =
        database.bookmarkDao().getAllBookmarks().map { list ->
            list.map { Bookmark(it.id, it.title, it.url, it.folder, it.createdAt) }
        }

    override fun getBookmarksByFolder(folder: String): Flow<List<Bookmark>> =
        database.bookmarkDao().getBookmarksByFolder(folder).map { list ->
            list.map { Bookmark(it.id, it.title, it.url, it.folder, it.createdAt) }
        }

    override fun getAllFolders(): Flow<List<String>> =
        database.bookmarkDao().getAllFolders()

    override fun searchBookmarks(query: String): Flow<List<Bookmark>> =
        database.bookmarkDao().searchBookmarks(query).map { list ->
            list.map { Bookmark(it.id, it.title, it.url, it.folder, it.createdAt) }
        }

    override fun isBookmarked(url: String): Flow<Boolean> =
        database.bookmarkDao().isBookmarked(url)

    override suspend fun addBookmark(title: String, url: String, folder: String): Long =
        database.bookmarkDao().insert(BookmarkEntity(title = title, url = url, folder = folder))

    override suspend fun deleteBookmark(bookmark: Bookmark) =
        database.bookmarkDao().delete(
            BookmarkEntity(
                id = bookmark.id,
                title = bookmark.title,
                url = bookmark.url,
                folder = bookmark.folder,
                createdAt = bookmark.createdAt
            )
        )

    override suspend fun deleteBookmarkByUrl(url: String) =
        database.bookmarkDao().deleteByUrl(url)

    // History
    override fun getAllHistory(): Flow<List<HistoryItem>> =
        database.historyDao().getAllHistory().map { list ->
            list.map { HistoryItem(it.id, it.title, it.url, it.visitedAt, it.visitCount) }
        }

    override fun getRecentHistory(limit: Int): Flow<List<HistoryItem>> =
        database.historyDao().getRecentHistory(limit).map { list ->
            list.map { HistoryItem(it.id, it.title, it.url, it.visitedAt, it.visitCount) }
        }

    override fun getFrequentlyVisited(limit: Int): Flow<List<HistoryItem>> =
        database.historyDao().getFrequentlyVisited(limit).map { list ->
            list.map { HistoryItem(it.id, it.title, it.url, it.visitedAt, it.visitCount) }
        }

    override fun searchHistory(query: String): Flow<List<HistoryItem>> =
        database.historyDao().searchHistory(query).map { list ->
            list.map { HistoryItem(it.id, it.title, it.url, it.visitedAt, it.visitCount) }
        }

    override suspend fun recordVisit(title: String, url: String) {
        val existing = database.historyDao().getEntryByUrl(url)
        if (existing != null) {
            database.historyDao().updateVisit(url, title.ifEmpty { existing.title })
        } else {
            database.historyDao().insert(HistoryEntity(title = title.ifEmpty { url }, url = url))
        }
    }

    override suspend fun deleteHistoryItem(id: Long) =
        database.historyDao().deleteById(id)

    override suspend fun clearHistory() =
        database.historyDao().clearAll()

    override suspend fun pruneOldHistory(maxAgeDays: Int) {
        val cutoff = System.currentTimeMillis() - (maxAgeDays.toLong() * 24 * 60 * 60 * 1000)
        database.historyDao().pruneOldHistory(cutoff)
    }

    // Shortcuts
    override fun getAllShortcuts(): Flow<List<Shortcut>> =
        database.shortcutDao().getAllShortcuts().map { list ->
            list.map { Shortcut(it.id, it.title, it.url, it.position) }
        }

    override suspend fun addShortcut(title: String, url: String): Long {
        val count = database.shortcutDao().count()
        return database.shortcutDao().insert(ShortcutEntity(title = title, url = url, position = count))
    }

    override suspend fun deleteShortcut(shortcut: Shortcut) =
        database.shortcutDao().delete(
            ShortcutEntity(id = shortcut.id, title = shortcut.title, url = shortcut.url, position = shortcut.position)
        )
}
