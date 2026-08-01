package com.finance.myledger.domain.sync

/**
 * Persists the SMS sync cursor (`lastSyncedTimestamp`).
 * Implemented with DataStore in the data layer — not relational, so not Room.
 */
interface SyncCursorStore {
    suspend fun getLastSyncedTimestamp(): Long
    suspend fun setLastSyncedTimestamp(timestamp: Long)
}
