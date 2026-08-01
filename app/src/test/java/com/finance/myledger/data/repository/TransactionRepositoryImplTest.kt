package com.finance.myledger.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finance.myledger.data.local.AppDatabase
import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.DateRangeBounds
import com.finance.myledger.domain.model.Direction
import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit test (Robolectric) for [TransactionRepositoryImpl] with an in-memory Room database.
 * Verifies dedupe-on-conflict, Flow emission, and TRANSFER exclusion from expense totals.
 */
@RunWith(RobolectricTestRunner::class)
class TransactionRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransactionRepositoryImpl(db.transactionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_emitsViaFlow() = runTest {
        val tx = sampleTx(id = "CODE111111", amount = 100.0)

        repository.insert(tx)

        val all = repository.observeAll().first()
        assertThat(all).hasSize(1)
        assertThat(all.first().id).isEqualTo("CODE111111")
        assertThat(all.first().amount).isEqualTo(100.0)
        assertThat(all.first().type).isEqualTo(TransactionType.EXPENSE)
    }

    @Test
    fun insert_duplicateId_isIgnored() = runTest {
        val first = sampleTx(id = "DEDUP12345", amount = 100.0, counterparty = "Alice")
        val duplicate = sampleTx(id = "DEDUP12345", amount = 999.0, counterparty = "Bob")

        repository.insert(first)
        repository.insert(duplicate)

        val all = repository.observeAll().first()
        assertThat(all).hasSize(1)
        assertThat(all.first().amount).isEqualTo(100.0)
        assertThat(all.first().counterparty).isEqualTo("Alice")
    }

    @Test
    fun observeLatestBalance_returnsMostRecent() = runTest {
        repository.insert(sampleTx(id = "OLD0000001", amount = 50.0, balance = 500.0, timestamp = 1_000L))
        repository.insert(sampleTx(id = "NEW0000002", amount = 20.0, balance = 480.0, timestamp = 2_000L))

        val balance = repository.observeLatestBalance().first()
        assertThat(balance).isEqualTo(480.0)
    }

    @Test
    fun observeExpenseTotal_excludesTransfers() = runTest {
        val bounds = DateRangeBounds(0L, Long.MAX_VALUE)
        repository.insert(sampleTx(id = "EXP1111111", amount = 100.0, type = TransactionType.EXPENSE))
        repository.insert(
            sampleTx(
                id = "TRF2222222",
                amount = 500.0,
                type = TransactionType.TRANSFER,
                counterparty = "MALI"
            )
        )

        val expense = repository.observeExpenseTotalInRange(bounds).first()
        assertThat(expense).isEqualTo(100.0)
    }

    @Test
    fun observeTotalFeesInRange_sumsFees() = runTest {
        val bounds = DateRangeBounds(0L, Long.MAX_VALUE)
        repository.insert(sampleTx(id = "FEE1111111", amount = 100.0, fee = 15.0))
        repository.insert(sampleTx(id = "FEE2222222", amount = 50.0, fee = 5.0))

        val fees = repository.observeTotalFeesInRange(bounds).first()
        assertThat(fees).isEqualTo(20.0)
    }

    private fun sampleTx(
        id: String,
        amount: Double,
        counterparty: String = "Counterparty",
        balance: Double? = 1_000.0,
        timestamp: Long = 1_720_000_000_000L,
        type: TransactionType = TransactionType.EXPENSE,
        fee: Double? = null
    ) = Transaction(
        id = id,
        source = "MPESA",
        amount = amount,
        direction = if (type == TransactionType.INCOME) Direction.IN else Direction.OUT,
        type = type,
        counterparty = counterparty,
        category = Category.OTHER,
        balanceAfter = balance,
        feeAmount = fee,
        timestamp = timestamp,
        rawMessage = "raw"
    )
}
