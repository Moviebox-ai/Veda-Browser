package com.example.velocity.data

import android.content.Context
import android.content.SharedPreferences
import com.example.velocity.model.Bookmark
import com.example.velocity.model.BrowserSettings
import com.example.velocity.model.DownloadItem
import com.example.velocity.model.HistoryItem
import com.example.velocity.model.SearchEngine
import com.example.velocity.model.ShieldStats
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BrowserStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("velocity_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_BOOKMARKS = "key_bookmarks"
        private const val KEY_HISTORY = "key_history"
        private const val KEY_DOWNLOADS = "key_downloads"
        private const val KEY_SETTINGS = "key_settings"
        private const val KEY_SHIELD_STATS = "key_shield_stats"
    }

    init {
        // Initialize default bookmarks if first launch
        if (getBookmarks().isEmpty()) {
            saveBookmarks(getDefaultBookmarks())
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

    fun getBookmarks(): List<Bookmark> {
        val json = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Bookmark>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveBookmarks(bookmarks: List<Bookmark>) {
        prefs.edit().putString(KEY_BOOKMARKS, gson.toJson(bookmarks)).apply()
    }

    fun addBookmark(bookmark: Bookmark) {
        val current = getBookmarks().toMutableList()
        // Replace if URL already exists or add new
        val existingIndex = current.indexOfFirst { it.url == bookmark.url }
        if (existingIndex >= 0) {
            current[existingIndex] = bookmark
        } else {
            current.add(0, bookmark)
        }
        saveBookmarks(current)
    }

    fun removeBookmark(id: String) {
        val updated = getBookmarks().filterNot { it.id == id }
        saveBookmarks(updated)
    }

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addHistoryItem(item: HistoryItem) {
        val current = getHistory().toMutableList()
        // Deduplicate recent visits to same URL
        current.removeAll { it.url == item.url }
        current.add(0, item)
        // Keep max 200 history items
        if (current.size > 200) {
            val trimmed = current.subList(0, 200)
            prefs.edit().putString(KEY_HISTORY, gson.toJson(trimmed)).apply()
        } else {
            prefs.edit().putString(KEY_HISTORY, gson.toJson(current)).apply()
        }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun removeHistoryItem(id: String) {
        val updated = getHistory().filterNot { it.id == id }
        prefs.edit().putString(KEY_HISTORY, gson.toJson(updated)).apply()
    }

    fun getDownloads(): List<DownloadItem> {
        val json = prefs.getString(KEY_DOWNLOADS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DownloadItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDownloads(downloads: List<DownloadItem>) {
        prefs.edit().putString(KEY_DOWNLOADS, gson.toJson(downloads)).apply()
    }

    fun addDownload(download: DownloadItem) {
        val current = getDownloads().toMutableList()
        current.removeAll { it.id == download.id }
        current.add(0, download)
        saveDownloads(current)
    }

    fun updateDownload(download: DownloadItem) {
        val current = getDownloads().toMutableList()
        val index = current.indexOfFirst { it.id == download.id }
        if (index >= 0) {
            current[index] = download
            saveDownloads(current)
        }
    }

    fun clearDownloads() {
        prefs.edit().remove(KEY_DOWNLOADS).apply()
    }

    fun getSettings(): BrowserSettings {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return BrowserSettings()
        return try {
            gson.fromJson(json, BrowserSettings::class.java) ?: BrowserSettings()
        } catch (_: Exception) {
            BrowserSettings()
        }
    }

    fun saveSettings(settings: BrowserSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }

    fun getShieldStats(): ShieldStats {
        val json = prefs.getString(KEY_SHIELD_STATS, null) ?: return ShieldStats()
        return try {
            gson.fromJson(json, ShieldStats::class.java) ?: ShieldStats()
        } catch (_: Exception) {
            ShieldStats()
        }
    }

    fun incrementShieldStats(adsCount: Int) {
        val current = getShieldStats()
        val updated = current.copy(
            totalAdsBlocked = current.totalAdsBlocked + adsCount,
            totalTrackersBlocked = current.totalTrackersBlocked + adsCount,
            totalBandwidthSavedMb = current.totalBandwidthSavedMb + (adsCount * 0.15f),
            totalTimeSavedSeconds = current.totalTimeSavedSeconds + (adsCount * 0.25f)
        )
        prefs.edit().putString(KEY_SHIELD_STATS, gson.toJson(updated)).apply()
    }
}
