package com.finance.myledger.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finance.myledger.domain.sync.SyncCursorStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

/**
 * DataStore-backed sync cursor. One scalar (`lastSyncedTimestamp`) — not relational data,
 * so DataStore is preferred over a Room single-row table.
 */
@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext context: Context
) : SyncCursorStore {

    private val dataStore = context.syncDataStore

    override suspend fun getLastSyncedTimestamp(): Long =
        dataStore.data.map { prefs -> prefs[KEY_LAST_SYNCED] ?: 0L }.first()

    override suspend fun setLastSyncedTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNCED] = timestamp
        }
    }

    override suspend fun getImportedParserVersion(): Int =
        dataStore.data.map { prefs -> prefs[KEY_PARSER_VERSION] ?: 0 }.first()

    override suspend fun setImportedParserVersion(version: Int) {
        dataStore.edit { prefs -> prefs[KEY_PARSER_VERSION] = version }
    }

    companion object {
        private val KEY_LAST_SYNCED = longPreferencesKey("last_synced_timestamp")
        private val KEY_PARSER_VERSION = intPreferencesKey("imported_parser_version")
    }
}
