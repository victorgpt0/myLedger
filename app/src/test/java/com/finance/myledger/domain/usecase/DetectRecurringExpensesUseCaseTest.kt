package com.finance.myledger.domain.usecase

import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.CategorySpend
import com.finance.myledger.domain.model.CounterpartySpend
import com.finance.myledger.domain.model.DateRangeBounds
import com.finance.myledger.domain.model.DayOfWeekSpend
import com.finance.myledger.domain.model.Direction
import com.finance.myledger.domain.model.FeeByKind
import com.finance.myledger.domain.model.MonthlyTotal
import com.finance.myledger.domain.model.PeriodTotals
import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.model.TransactionType
import com.finance.myledger.domain.repository.TransactionRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test
import java.util.Calendar

class DetectRecurringExpensesUseCaseTest {

    private val useCase = DetectRecurringExpensesUseCase(EmptyRepo)

    @Test
    fun detect_flagsMonthlyCadence() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            expense("KPLC", 450.0, monthsAgo(now, 0)),
            expense("KPLC", 460.0, monthsAgo(now, 1)),
            expense("KPLC", 440.0, monthsAgo(now, 2)),
            expense("ONEOFF SHOP", 900.0, monthsAgo(now, 0))
        )

        val recurring = useCase.detect(txs, lookbackMonths = 4, minMonthsSeen = 3)

        assertThat(recurring).hasSize(1)
        assertThat(recurring.first().counterparty).isEqualTo("KPLC")
    }

    @Test
    fun detect_ignoresTransfers() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            transfer("MALI", 1000.0, monthsAgo(now, 0)),
            transfer("MALI", 1000.0, monthsAgo(now, 1)),
            transfer("MALI", 1000.0, monthsAgo(now, 2))
        )

        assertThat(useCase.detect(txs)).isEmpty()
    }

    private fun expense(counterparty: String, amount: Double, timestamp: Long) = Transaction(
        id = "$counterparty-$timestamp",
        source = "MPESA",
        amount = amount,
        direction = Direction.OUT,
        type = TransactionType.EXPENSE,
        counterparty = counterparty,
        category = Category.OTHER,
        balanceAfter = null,
        feeAmount = null,
        timestamp = timestamp,
        rawMessage = "raw"
    )

    private fun transfer(counterparty: String, amount: Double, timestamp: Long) =
        expense(counterparty, amount, timestamp).copy(type = TransactionType.TRANSFER)

    private fun monthsAgo(now: Long, months: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -months)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        return cal.timeInMillis
    }

    /** Stub — only [DetectRecurringExpensesUseCase.detect] is exercised here. */
    private object EmptyRepo : TransactionRepository {
        override suspend fun insert(transaction: Transaction) = Unit
        override fun observeAll() = emptyFlow<List<Transaction>>()
        override fun observeRecent(limit: Int) = emptyFlow<List<Transaction>>()
        override fun observeInRange(bounds: DateRangeBounds) = emptyFlow<List<Transaction>>()
        override fun observeCountedInRange(bounds: DateRangeBounds) = emptyFlow<List<Transaction>>()
        override fun observeLatestBalance() = emptyFlow<Double?>()
        override fun observeExpenseTotalInRange(bounds: DateRangeBounds) = emptyFlow<Double>()
        override fun observeIncomeTotalInRange(bounds: DateRangeBounds) = emptyFlow<Double>()
        override fun observeCategoryTotalsInRange(bounds: DateRangeBounds) =
            emptyFlow<List<CategorySpend>>()
        override fun observeTotalFeesInRange(bounds: DateRangeBounds) = emptyFlow<Double>()
        override fun observePeriodTotals(bounds: DateRangeBounds) = emptyFlow<PeriodTotals>()
        override fun observeFeesByKind(bounds: DateRangeBounds) = emptyFlow<List<FeeByKind>>()
        override fun observeFirstTimestamp() = emptyFlow<Long?>()
        override fun observeMonthlyTrend(bounds: DateRangeBounds) = emptyFlow<List<MonthlyTotal>>()
        override fun observeDayOfWeekSpend(bounds: DateRangeBounds) =
            emptyFlow<List<DayOfWeekSpend>>()
        override fun observeTopCounterparties(bounds: DateRangeBounds, limit: Int) =
            emptyFlow<List<CounterpartySpend>>()
    }
}
