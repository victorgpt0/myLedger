package com.finance.myledger.domain.model

/**
 * Headline totals for one period. Transfers are split by direction so money moved into a
 * savings wallet and later withdrawn isn't counted twice.
 */
data class PeriodTotals(
    val income: Double,
    val expense: Double,
    val fees: Double,
    val transferOut: Double,
    val transferIn: Double,
    val expenseCount: Int
) {
    val net: Double get() = income - expense
    val netToWallets: Double get() = transferOut - transferIn

    companion object {
        val EMPTY = PeriodTotals(0.0, 0.0, 0.0, 0.0, 0.0, 0)
    }
}

/** Fees paid on one [TransactionKind], with how many transactions of that kind occurred. */
data class FeeByKind(
    val kind: TransactionKind,
    val totalFees: Double,
    val transactionCount: Int
)

/**
 * Spend for one weekday (0 = Sunday … 6 = Saturday, matching SQLite `%w`), averaged over how
 * many times that weekday occurs in the period, so a month with five Saturdays isn't skewed.
 */
data class WeekdayAverage(
    val dayOfWeek: Int,
    val total: Double,
    val occurrences: Int
) {
    val average: Double get() = if (occurrences == 0) 0.0 else total / occurrences
}

/** What the previous-period figures are compared against; the UI turns this into a label. */
enum class ComparisonBasis {
    SAME_POINT_LAST_MONTH,
    MONTH_BEFORE,
    SAME_POINT_LAST_YEAR,
    PREVIOUS_EQUAL_PERIOD
}
