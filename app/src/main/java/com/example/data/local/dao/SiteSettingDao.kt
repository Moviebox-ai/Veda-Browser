package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SiteSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteSettingDao {

    @Query("SELECT * FROM site_settings WHERE origin = :origin LIMIT 1")
    suspend fun getSettingForOrigin(origin: String): SiteSettingEntity?

    @Query("SELECT * FROM site_settings WHERE origin = :origin LIMIT 1")
    fun observeSettingForOrigin(origin: String): Flow<SiteSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SiteSettingEntity)

    @Update
    suspend fun update(setting: SiteSettingEntity)

    @Delete
    suspend fun delete(setting: SiteSettingEntity)

    @Query("DELETE FROM site_settings WHERE origin = :origin")
    suspend fun deleteByOrigin(origin: String)
}
