package com.finance.myledger.domain.model

/**
 * Aggregated dashboard data assembled by [com.finance.myledger.domain.usecase.ObserveDashboardSummaryUseCase].
 * Lives in domain so ViewModels never assemble Room query results themselves.
 */
data class DashboardSummary(
    val latestBalance: Double?,
    val recentTransactions: List<Transaction>,
    val monthlySpendTotal: Double,
    val categorySpend: List<CategorySpend>,
    val feesPaidThisMonth: Double
)

data class CategorySpend(
    val category: Category,
    val total: Double
)

/**
 * One calendar month of income / expense / fee totals for trend charts.
 * [yearMonth] is `yyyy-MM`.
 */
data class MonthlyTotal(
    val yearMonth: String,
    val income: Double,
    val expense: Double,
    val fees: Double
)

/**
 * Spend aggregated for a single weekday (0 = Sunday … 6 = Saturday, matching SQLite `%w`).
 */
data class DayOfWeekSpend(
    val dayOfWeek: Int,
    val total: Double
)

/**
 * Counterparty ranked by expense total in a range.
 */
data class CounterpartySpend(
    val counterparty: String,
    val total: Double,
    val transactionCount: Int
)

/**
 * Heuristic recurring expense candidate from [com.finance.myledger.domain.usecase.DetectRecurringExpensesUseCase].
 */
data class RecurringExpense(
    val counterparty: String,
    val averageAmount: Double,
    val monthsSeen: Int,
    val lastSeenMillis: Long
)

/**
 * Raw SMS payload read from the device inbox or BroadcastReceiver.
 * Domain-friendly so use cases never touch Android Cursor/ContentResolver types.
 */
data class RawSms(
    val sender: String,
    val body: String,
    val timestamp: Long
)
