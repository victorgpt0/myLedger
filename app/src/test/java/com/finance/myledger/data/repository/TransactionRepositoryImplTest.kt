package com.finance.myledger.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finance.myledger.data.local.AppDatabase
import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.DateRangeBounds
import com.finance.myledger.domain.model.Direction
import com.finance.myledger.domain.model.FeeByKind
import com.finance.myledger.domain.model.PeriodTotals
import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.model.TransactionKind
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

    @Test
    fun observeLatestBalance_sameTimestamp_followsBalanceChain() = runTest {
        // 2487.20 -> +360 -> 2847.20 -> -10 -> 2837.20, all in the same minute, inserted out of order.
        repository.insert(sampleTx(id = "UKQA13VPWQ", amount = 10.0, balance = 2837.2, fee = 0.0))
        repository.insert(sampleTx(id = "UKQFE3YGCJ", amount = 360.0, balance = 2847.2, type = TransactionType.INCOME))
        repository.insert(sampleTx(id = "UKQA13VVJN", amount = 535.0, balance = 2487.2, fee = 0.0))

        assertThat(repository.observeLatestBalance().first()).isEqualTo(2837.2)
    }

    @Test
    fun observeAll_sameTimestamp_ordersByBalanceChain() = runTest {
        repository.insert(sampleTx(id = "UKQA13VPWQ", amount = 10.0, balance = 2837.2, fee = 0.0))
        repository.insert(sampleTx(id = "UKQFE3YGCJ", amount = 360.0, balance = 2847.2, type = TransactionType.INCOME))
        repository.insert(sampleTx(id = "UKQA13VVJN", amount = 535.0, balance = 2487.2, fee = 0.0))

        val ids = repository.observeAll().first().map { it.id }
        assertThat(ids).containsExactly("UKQA13VPWQ", "UKQFE3YGCJ", "UKQA13VVJN").inOrder()
    }

    @Test
    fun observePeriodTotals_splitsTransfersByDirection() = runTest {
        val bounds = DateRangeBounds(0L, Long.MAX_VALUE)
        repository.insert(sampleTx(id = "INC0000001", amount = 1_000.0, type = TransactionType.INCOME))
        repository.insert(sampleTx(id = "EXP0000001", amount = 300.0, fee = 7.0))
        repository.insert(sampleTx(id = "EXP0000002", amount = 200.0))
        repository.insert(sampleTx(id = "TRO0000001", amount = 500.0, type = TransactionType.TRANSFER, direction = Direction.OUT))
        repository.insert(sampleTx(id = "TRI0000001", amount = 200.0, type = TransactionType.TRANSFER, direction = Direction.IN))

        val totals = repository.observePeriodTotals(bounds).first()

        assertThat(totals.income).isEqualTo(1_000.0)
        assertThat(totals.expense).isEqualTo(500.0)
        assertThat(totals.fees).isEqualTo(7.0)
        assertThat(totals.transferOut).isEqualTo(500.0)
        assertThat(totals.transferIn).isEqualTo(200.0)
        assertThat(totals.netToWallets).isEqualTo(300.0)
        assertThat(totals.expenseCount).isEqualTo(2)
    }

    @Test
    fun observePeriodTotals_emptyRangeIsZero() = runTest {
        val totals = repository.observePeriodTotals(DateRangeBounds(0L, 1L)).first()
        assertThat(totals).isEqualTo(PeriodTotals.EMPTY)
    }

    @Test
    fun observeFeesByKind_groupsAndSkipsFreeKinds() = runTest {
        val bounds = DateRangeBounds(0L, Long.MAX_VALUE)
        repository.insert(sampleTx(id = "WD00000001", amount = 1_000.0, fee = 29.0, kind = TransactionKind.WITHDRAWAL))
        repository.insert(sampleTx(id = "WD00000002", amount = 2_000.0, fee = 29.0, kind = TransactionKind.WITHDRAWAL))
        repository.insert(sampleTx(id = "SM00000001", amount = 500.0, fee = 7.0, kind = TransactionKind.SEND_MONEY))
        repository.insert(sampleTx(id = "BG00000001", amount = 50.0, fee = 0.0, kind = TransactionKind.BUY_GOODS))

        val fees = repository.observeFeesByKind(bounds).first()

        assertThat(fees).containsExactly(
            FeeByKind(TransactionKind.WITHDRAWAL, 58.0, 2),
            FeeByKind(TransactionKind.SEND_MONEY, 7.0, 1)
        ).inOrder()
    }

    @Test
    fun observeFirstTimestamp_returnsEarliestOrNull() = runTest {
        assertThat(repository.observeFirstTimestamp().first()).isNull()
        repository.insert(sampleTx(id = "LATE000001", amount = 1.0, timestamp = 5_000L))
        repository.insert(sampleTx(id = "EARLY00001", amount = 1.0, timestamp = 2_000L))
        assertThat(repository.observeFirstTimestamp().first()).isEqualTo(2_000L)
    }

    @Test
    fun reversals_andTheirOriginals_areLeftOutOfTotals() = runTest {
        val bounds = DateRangeBounds(0L, Long.MAX_VALUE)
        repository.insert(sampleTx(id = "PAY0000001", amount = 544.0, fee = 7.0))                       // refunded later
        repository.insert(sampleTx(id = "PAY0000002", amount = 100.0))                                  // stands
        repository.insert(sampleTx(id = "REV0000001", amount = 544.0, type = TransactionType.INCOME,
            direction = Direction.IN, kind = TransactionKind.REVERSAL, reversesId = "PAY0000001"))
        repository.insert(sampleTx(id = "REV0000002", amount = 50.0, kind = TransactionKind.REVERSAL,
            reversesId = "OLDER00001"))                                                                  // original not stored

        val totals = repository.observePeriodTotals(bounds).first()
        assertThat(totals.expense).isEqualTo(100.0)
        assertThat(totals.income).isEqualTo(0.0)
        assertThat(totals.fees).isEqualTo(0.0)
        assertThat(totals.expenseCount).isEqualTo(1)
        assertThat(repository.observeExpenseTotalInRange(bounds).first()).isEqualTo(100.0)
        assertThat(repository.observeCountedInRange(bounds).first().map { it.id }).containsExactly("PAY0000002")
        // Lists still show everything.
        assertThat(repository.observeAll().first()).hasSize(4)
    }

    @Test
    fun observeTopCounterparties_mergesDifferentlyWrittenNames() = runTest {
        val bounds = DateRangeBounds(0L, Long.MAX_VALUE)
        repository.insert(sampleTx(id = "P000000001", amount = 100.0, counterparty = "Evans  Maingi 0759***207"))
        repository.insert(sampleTx(id = "P000000002", amount = 50.0, counterparty = "EVANS MAINGI 0712345678"))
        repository.insert(sampleTx(id = "P000000003", amount = 40.0, counterparty = "Stanbic Bank for account 106168"))
        repository.insert(sampleTx(id = "P000000004", amount = 60.0, counterparty = "Stanbic Bank for account 2"))

        val top = repository.observeTopCounterparties(bounds, 10).first()

        assertThat(top.map { Triple(it.counterparty, it.total, it.transactionCount) }).containsExactly(
            Triple("Evans Maingi", 150.0, 2),
            Triple("Stanbic Bank", 100.0, 2)
        ).inOrder()
    }

    private fun sampleTx(
        id: String,
        amount: Double,
        counterparty: String = "Counterparty",
        balance: Double? = 1_000.0,
        timestamp: Long = 1_720_000_000_000L,
        type: TransactionType = TransactionType.EXPENSE,
        fee: Double? = null,
        direction: Direction = if (type == TransactionType.INCOME) Direction.IN else Direction.OUT,
        kind: TransactionKind = TransactionKind.OTHER,
        reversesId: String? = null
    ) = Transaction(
        id = id,
        source = "MPESA",
        amount = amount,
        direction = direction,
        type = type,
        kind = kind,
        counterparty = counterparty,
        category = Category.OTHER,
        balanceAfter = balance,
        feeAmount = fee,
        timestamp = timestamp,
        rawMessage = "raw",
        reversesId = reversesId
    )
}
