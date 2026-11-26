package com.finance.myledger.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.finance.myledger.domain.settings.PrivacySettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_prefs")

/** DataStore-backed [PrivacySettings]. Separate file from the sync cursor — unrelated concerns. */
@Singleton
class PrivacyPreferences @Inject constructor(
    @ApplicationContext context: Context
) : PrivacySettings {

    private val dataStore = context.privacyDataStore

    override val hideBalance: Flow<Boolean> = dataStore.data.map { it[KEY_HIDE_BALANCE] ?: false }

    override suspend fun setHideBalance(hidden: Boolean) {
        dataStore.edit { it[KEY_HIDE_BALANCE] = hidden }
    }

    private companion object {
        val KEY_HIDE_BALANCE = booleanPreferencesKey("hide_balance")
    }
}
