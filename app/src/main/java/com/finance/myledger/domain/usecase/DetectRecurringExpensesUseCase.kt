package com.finance.myledger.domain.usecase

import com.finance.myledger.domain.classifier.CounterpartyName
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.model.RecurringExpense
import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.model.TransactionType
import com.finance.myledger.domain.model.toBounds
import com.finance.myledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

/**
 * Heuristic recurring-expense detector — NOT a guarantee.
 *
 * Groups EXPENSE transactions by counterparty and flags ones that appear in at least
 * [minMonthsSeen] of the last [lookbackMonths] calendar months with amounts within
 * ±[amountTolerance] of that counterparty's mean. No ML — plain grouping + interval math
 * so this stays unit-testable. Easy to tighten/loosen thresholds later.
 */
class DetectRecurringExpensesUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(
        range: DateRange = DateRange.Last6Months,
        lookbackMonths: Int = 4,
        minMonthsSeen: Int = 3,
        amountTolerance: Double = 0.10
    ): Flow<List<RecurringExpense>> {
        val bounds = range.toBounds()
        return repository.observeCountedInRange(bounds).map { transactions ->
            detect(transactions, lookbackMonths, minMonthsSeen, amountTolerance)
        }
    }

    /** Pure detection entry point for unit tests. */
    fun detect(
        transactions: List<Transaction>,
        lookbackMonths: Int = 4,
        minMonthsSeen: Int = 3,
        amountTolerance: Double = 0.10
    ): List<RecurringExpense> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenses.isEmpty()) return emptyList()

        val monthKeys = lastNMonthKeys(lookbackMonths)
        val byCounterparty = expenses.groupBy { CounterpartyName.key(it.counterparty) }

        return byCounterparty.mapNotNull { (_, txs) ->
            val byMonth = txs.groupBy { yearMonthKey(it.timestamp) }
            val monthsPresent = monthKeys.count { byMonth.containsKey(it) }
            if (monthsPresent < minMonthsSeen) return@mapNotNull null

            val amounts = txs.map { it.amount }
            val avg = amounts.average()
            if (avg <= 0.0) return@mapNotNull null

            val similar = amounts.count { abs(it - avg) / avg <= amountTolerance }
            // Require most amounts to cluster near the mean (±tolerance).
            if (similar < minMonthsSeen) return@mapNotNull null

            RecurringExpense(
                counterparty = CounterpartyName.bestDisplay(txs.map { it.counterparty }),
                averageAmount = avg,
                monthsSeen = monthsPresent,
                lastSeenMillis = txs.maxOf { it.timestamp }
            )
        }.sortedByDescending { it.averageAmount }
    }

    private fun yearMonthKey(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val month = cal.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(cal.get(Calendar.YEAR), month)
    }

    private fun lastNMonthKeys(n: Int): List<String> {
        val cal = Calendar.getInstance()
        return (0 until n).map {
            val key = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            cal.add(Calendar.MONTH, -1)
            key
        }
    }
}
