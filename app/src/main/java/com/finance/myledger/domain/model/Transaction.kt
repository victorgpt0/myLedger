package com.finance.myledger.domain.model

/**
 * Domain transaction model — the single type used by UI, use cases, and tests.
 *
 * Kept separate from [com.finance.myledger.data.local.TransactionEntity] so the UI/domain
 * never depend on Room annotations. When cloud sync or an alternate store is added later,
 * only the data layer and mappers change.
 *
 * [type] drives all income/expense aggregations. [direction] remains for UI sign display
 * (e.g. a TRANSFER sent into MALI still shows as outbound).
 */
data class Transaction(
    val id: String,
    val source: String,
    val amount: Double,
    val direction: Direction,
    val type: TransactionType,
    val counterparty: String,
    val category: Category,
    val balanceAfter: Double?,
    val feeAmount: Double?,
    val timestamp: Long,
    val rawMessage: String
)

enum class Direction { IN, OUT }

/**
 * A TRANSFER is money moving between the user's own accounts/wallets — it must never be
 * summed into income or expense totals, or the same money gets counted twice.
 */
enum class TransactionType { INCOME, EXPENSE, TRANSFER }

enum class Category {
    AIRTIME,
    UTILITIES,
    SHOPPING,
    TRANSPORT,
    PERSON_TO_PERSON,
    OTHER
}
