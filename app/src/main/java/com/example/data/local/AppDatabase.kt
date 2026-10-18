package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.dao.ShortcutDao
import com.example.data.local.dao.SiteSettingDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.ShortcutEntity
import com.example.data.local.entity.SiteSettingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        ShortcutEntity::class,
        DownloadEntity::class,
        SiteSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun downloadDao(): DownloadDao
    abstract fun siteSettingDao(): SiteSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "velocity_browser.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate popular shortcuts
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).shortcutDao()
                            val defaults = listOf(
                                ShortcutEntity(title = "DuckDuckGo", url = "https://duckduckgo.com", position = 0),
                                ShortcutEntity(title = "Wikipedia", url = "https://en.wikipedia.org", position = 1),
                                ShortcutEntity(title = "GitHub", url = "https://github.com", position = 2),
                                ShortcutEntity(title = "Hacker News", url = "https://news.ycombinator.com", position = 3),
                                ShortcutEntity(title = "OpenStreetMap", url = "https://www.openstreetmap.org", position = 4),
                                ShortcutEntity(title = "Ars Technica", url = "https://arstechnica.com", position = 5)
                            )
                            dao.insertAll(defaults)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
