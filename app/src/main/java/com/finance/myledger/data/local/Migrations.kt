package com.finance.myledger.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    /**
     * Adds `kind` and backfills it from the stored SMS text, so existing users keep their data.
     * The LIKE checks mirror which [com.finance.myledger.data.parser.MpesaParser] pattern would
     * have matched; SQLite LIKE is case-insensitive for ASCII.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'OTHER'")
            db.execSQL(
                """
                UPDATE `transactions` SET `kind` = CASE
                    WHEN rawMessage LIKE '%withdrawn from%' THEN 'WITHDRAWAL'
                    WHEN rawMessage LIKE '%of airtime%' THEN 'AIRTIME'
                    WHEN rawMessage LIKE '%you have received%' THEN 'RECEIVED'
                    WHEN rawMessage LIKE '%paid to%' THEN 'BUY_GOODS'
                    WHEN rawMessage LIKE '%sent to%for account%' THEN 'PAYBILL'
                    WHEN rawMessage LIKE '%sent to%' THEN 'SEND_MONEY'
                    ELSE 'OTHER'
                END
                """.trimIndent()
            )
        }
    }
}
