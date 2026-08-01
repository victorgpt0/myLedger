package com.finance.myledger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * App Room database — the single source of truth for persisted transactions.
 * Version bumped when feeAmount / TransactionType columns were added; destructive
 * fallback is fine for v1 (re-scan SMS after upgrade).
 */
@Database(
    entities = [TransactionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
