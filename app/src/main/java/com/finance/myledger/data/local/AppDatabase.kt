package com.finance.myledger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * App Room database — the single source of truth for persisted transactions.
 *
 * v2 → v3 adds `kind` via [Migrations.MIGRATION_2_3]; v3 → v4 adds `reversesId` via
 * [Migrations.MIGRATION_3_4] (data preserved). Older versions still
 * use destructive fallback (re-scan SMS after upgrade).
 */
@Database(
    entities = [TransactionEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
