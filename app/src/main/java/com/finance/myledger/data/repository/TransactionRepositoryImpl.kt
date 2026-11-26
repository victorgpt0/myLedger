package com.finance.myledger.data.repository

import com.finance.myledger.data.local.TransactionDao
import com.finance.myledger.data.local.TransactionEntity
import com.finance.myledger.data.mapper.toDomain
import com.finance.myledger.data.mapper.toEntity
import com.finance.myledger.domain.balance.BalanceChainResolver
import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.CategorySpend
import com.finance.myledger.domain.model.CounterpartySpend
import com.finance.myledger.domain.model.DateRangeBounds
import com.finance.myledger.domain.model.DayOfWeekSpend
import com.finance.myledger.domain.model.FeeByKind
import com.finance.myledger.domain.model.MonthlyTotal
import com.finance.myledger.domain.model.PeriodTotals
import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.model.TransactionKind
import com.finance.myledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [TransactionRepository]. Single source of truth: every use case reads
 * through this impl so swapping to a remote source later only changes this class + DI bind.
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override suspend fun insert(transaction: Transaction) {
        dao.insert(transaction.toEntity())
    }

    override fun observeAll(): Flow<List<Transaction>> =
        dao.observeAll().map { it.toChainOrderedDomain() }

    override fun observeRecent(limit: Int): Flow<List<Transaction>> =
        dao.observeRecent(limit).map { it.toChainOrderedDomain() }

    override fun observeInRange(bounds: DateRangeBounds): Flow<List<Transaction>> =
        dao.observeInRange(bounds.startMillis, bounds.endMillis)
            .map { it.toChainOrderedDomain() }

    override fun observeLatestBalance(): Flow<Double?> =
        dao.observeLatestBalanceCandidates().map { rows ->
            BalanceChainResolver.latest(rows.map { it.toDomain() })?.balanceAfter
        }

    override fun observeExpenseTotalInRange(bounds: DateRangeBounds): Flow<Double> =
        dao.observeExpenseTotalInRange(bounds.startMillis, bounds.endMillis)

    override fun observeIncomeTotalInRange(bounds: DateRangeBounds): Flow<Double> =
        dao.observeIncomeTotalInRange(bounds.startMillis, bounds.endMillis)

    override fun observeCategoryTotalsInRange(bounds: DateRangeBounds): Flow<List<CategorySpend>> =
        dao.observeCategoryTotalsInRange(bounds.startMillis, bounds.endMillis).map { rows ->
            rows.map { row ->
                CategorySpend(
                    category = runCatching { Category.valueOf(row.category) }.getOrDefault(Category.OTHER),
                    total = row.total
                )
            }
        }

    override fun observeTotalFeesInRange(bounds: DateRangeBounds): Flow<Double> =
        dao.observeTotalFeesInRange(bounds.startMillis, bounds.endMillis)

    override fun observePeriodTotals(bounds: DateRangeBounds): Flow<PeriodTotals> =
        dao.observePeriodTotals(bounds.startMillis, bounds.endMillis).map { row ->
            PeriodTotals(
                income = row.income,
                expense = row.expense,
                fees = row.fees,
                transferOut = row.transferOut,
                transferIn = row.transferIn,
                expenseCount = row.expenseCount
            )
        }

    override fun observeFeesByKind(bounds: DateRangeBounds): Flow<List<FeeByKind>> =
        dao.observeFeesByKind(bounds.startMillis, bounds.endMillis).map { rows ->
            rows.map { row ->
                FeeByKind(
                    kind = TransactionKind.entries.firstOrNull { it.name == row.kind } ?: TransactionKind.OTHER,
                    totalFees = row.fees,
                    transactionCount = row.transactionCount
                )
            }
        }

    override fun observeFirstTimestamp(): Flow<Long?> = dao.observeFirstTimestamp()

    override fun observeMonthlyTrend(bounds: DateRangeBounds): Flow<List<MonthlyTotal>> =
        dao.observeMonthlyTrend(bounds.startMillis, bounds.endMillis).map { rows ->
            rows.map { row ->
                MonthlyTotal(
                    yearMonth = row.yearMonth,
                    income = row.income,
                    expense = row.expense,
                    fees = row.fees
                )
            }
        }

    override fun observeDayOfWeekSpend(bounds: DateRangeBounds): Flow<List<DayOfWeekSpend>> =
        dao.observeDayOfWeekSpend(bounds.startMillis, bounds.endMillis).map { rows ->
            rows.map { row -> DayOfWeekSpend(dayOfWeek = row.dayOfWeek, total = row.total) }
        }

    override fun observeTopCounterparties(
        bounds: DateRangeBounds,
        limit: Int
    ): Flow<List<CounterpartySpend>> =
        dao.observeTopCounterparties(bounds.startMillis, bounds.endMillis, limit).map { rows ->
            rows.map { row ->
                CounterpartySpend(
                    counterparty = row.counterparty,
                    total = row.total,
                    transactionCount = row.transactionCount
                )
            }
        }

    /** Same-minute ties come back in arbitrary order from SQL; fix them via the balance chain. */
    private fun List<TransactionEntity>.toChainOrderedDomain(): List<Transaction> =
        BalanceChainResolver.orderNewestFirst(map { it.toDomain() })
}
