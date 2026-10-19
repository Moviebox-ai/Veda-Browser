package com.aaryo.velocity.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aaryo.velocity.extension.BuiltInExtensions
import com.aaryo.velocity.model.Extension
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.extensionDataStore: DataStore<Preferences> by preferencesDataStore(name = "velocity_extensions")

class ExtensionRepository(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val KEY_EXTENSIONS = stringPreferencesKey("installed_extensions")
        private val KEY_GLOBAL_ENABLED = booleanPreferencesKey("extensions_global_enabled")
    }

    val globalEnabledFlow: Flow<Boolean> = context.extensionDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_GLOBAL_ENABLED] ?: true }

    suspend fun setGlobalEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        context.extensionDataStore.edit { it[KEY_GLOBAL_ENABLED] = enabled }
    }

    val extensionsFlow: Flow<List<Extension>> = context.extensionDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            val json = preferences[KEY_EXTENSIONS]
            if (json.isNullOrBlank()) {
                BuiltInExtensions.getAll()
            } else {
                try {
                    val type = object : TypeToken<List<Extension>>() {}.type
                    val list = gson.fromJson<List<Extension>>(json, type) ?: emptyList()
                    mergeWithBuiltIns(list)
                } catch (_: Exception) {
                    BuiltInExtensions.getAll()
                }
            }
        }

    private fun mergeWithBuiltIns(saved: List<Extension>): List<Extension> {
        val builtIns = BuiltInExtensions.getAll()
        val merged = saved.toMutableList()
        for (bi in builtIns) {
            if (merged.none { it.id == bi.id }) {
                merged.add(bi)
            }
        }
        return merged
    }

    suspend fun saveExtension(extension: Extension) = withContext(Dispatchers.IO) {
        val current = extensionsFlow.first().toMutableList()
        val idx = current.indexOfFirst { it.id == extension.id }
        if (idx >= 0) {
            current[idx] = extension
        } else {
            current.add(0, extension)
        }
        writeExtensions(current)
    }

    suspend fun deleteExtension(id: String) = withContext(Dispatchers.IO) {
        val current = extensionsFlow.first().filterNot { it.id == id }
        writeExtensions(current)
    }

    suspend fun toggleExtension(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val current = extensionsFlow.first().map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        writeExtensions(current)
    }

    suspend fun toggleIncognito(id: String, allow: Boolean) = withContext(Dispatchers.IO) {
        val current = extensionsFlow.first().map {
            if (it.id == id) it.copy(allowInIncognito = allow) else it
        }
        writeExtensions(current)
    }

    private suspend fun writeExtensions(list: List<Extension>) {
        context.extensionDataStore.edit { preferences ->
            preferences[KEY_EXTENSIONS] = gson.toJson(list)
        }
    }

    suspend fun getGmValue(extId: String, key: String): String? = withContext(Dispatchers.IO) {
        val prefKey = stringPreferencesKey("gm_${extId}_$key")
        context.extensionDataStore.data.first()[prefKey]
    }

    suspend fun setGmValue(extId: String, key: String, value: String) = withContext(Dispatchers.IO) {
        val prefKey = stringPreferencesKey("gm_${extId}_$key")
        context.extensionDataStore.edit { it[prefKey] = value }
    }

    suspend fun deleteGmValue(extId: String, key: String) = withContext(Dispatchers.IO) {
        val prefKey = stringPreferencesKey("gm_${extId}_$key")
        context.extensionDataStore.edit { it.remove(prefKey) }
    }
}
