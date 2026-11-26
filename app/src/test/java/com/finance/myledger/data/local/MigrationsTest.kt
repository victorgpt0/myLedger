package com.finance.myledger.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finance.myledger.data.repository.TransactionRepositoryImpl
import com.finance.myledger.domain.model.TransactionKind
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Builds a real v2 database (schema copied from Room's generated v2 code), then opens it with
 * the current Room setup. Room validates the migrated schema on open, so a mismatch fails here.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate2ToLatest_keepsRowsAndBackfillsKind() = runTest {
        val path = context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { v2 ->
            v2.execSQL(V2_SCHEMA)
            V2_INDICES.forEach(v2::execSQL)
            mapOf(
                "PAID000001" to "PAID000001 Confirmed. Ksh535.00 paid to YAJJA DELIVERY ENTERPRISE. on 26/11/26 at 12:27 PM.",
                "BILL000001" to "BILL000001 Confirmed. Ksh450.00 sent to KPLC PREPAID for account 1234 on 1/7/26 at 10:00 AM.",
                "SEND000001" to "SEND000001 Confirmed. Ksh500.00 sent to JOHN DOE 0712345678 on 1/7/26 at 3:15 PM.",
                "RECV000001" to "RECV000001 Confirmed.You have received Ksh360.00 from Evans Maingi on 26/11/26 at 12:27 PM",
                "WDRW000001" to "WDRW000001 Confirmed. Ksh2,000.00 withdrawn from Agent AMOS SHOP on 1/7/26 at 4:30 PM.",
                "AIRT000001" to "AIRT000001 Confirmed. You bought Ksh100.00 of airtime on 1/7/26 at 9:00 AM.",
                "ODD0000001" to "Something unrecognised"
            ).forEach { (id, raw) ->
                v2.execSQL(
                    "INSERT INTO transactions VALUES (?, 'MPESA', 10.0, 'OUT', 'EXPENSE', 'X', 'OTHER', 100.0, NULL, 1000, ?)",
                    arrayOf(id, raw)
                )
            }
            v2.version = 2
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val rows = TransactionRepositoryImpl(db.transactionDao()).observeAll().first()
            assertThat(rows.map { it.reversesId }.toSet()).containsExactly(null) // 3 → 4 column added, empty
            val kinds = rows.associate { it.id to it.kind }
            assertThat(kinds).containsExactly(
                "PAID000001", TransactionKind.BUY_GOODS,
                "BILL000001", TransactionKind.PAYBILL,
                "SEND000001", TransactionKind.SEND_MONEY,
                "RECV000001", TransactionKind.RECEIVED,
                "WDRW000001", TransactionKind.WITHDRAWAL,
                "AIRT000001", TransactionKind.AIRTIME,
                "ODD0000001", TransactionKind.OTHER
            )
        } finally {
            db.close()
        }
    }

    private companion object {
        const val V2_SCHEMA = "CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `source` TEXT NOT NULL, " +
            "`amount` REAL NOT NULL, `direction` TEXT NOT NULL, `type` TEXT NOT NULL, `counterparty` TEXT NOT NULL, " +
            "`category` TEXT NOT NULL, `balanceAfter` REAL, `feeAmount` REAL, `timestamp` INTEGER NOT NULL, " +
            "`rawMessage` TEXT NOT NULL, PRIMARY KEY(`id`))"
        val V2_INDICES = listOf(
            "CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_counterparty` ON `transactions` (`counterparty`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)"
        )
    }
}
