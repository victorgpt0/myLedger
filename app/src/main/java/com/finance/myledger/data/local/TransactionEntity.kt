package com.finance.myledger.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.Direction
import com.finance.myledger.domain.model.TransactionKind
import com.finance.myledger.domain.model.TransactionType

/**
 * Room persistence model for a financial transaction.
 *
 * Intentionally separate from the domain [com.finance.myledger.domain.model.Transaction]:
 * Room annotations and SQL column types must not leak into UI/domain. Mappers in
 * [com.finance.myledger.data.mapper] convert between the two.
 *
 * [id] is the parser-provided reference code (e.g. M-PESA receipt) and the dedupe key.
 * Enums are stored as strings via [Converters].
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["type"]),
        Index(value = ["counterparty"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val source: String,
    val amount: Double,
    val direction: Direction,
    val type: TransactionType,
    // Default must match MIGRATION_2_3 so Room's schema validation passes for migrated DBs.
    @ColumnInfo(defaultValue = "OTHER") val kind: TransactionKind,
    val counterparty: String,
    val category: Category,
    val balanceAfter: Double?,
    val feeAmount: Double?,
    val timestamp: Long,
    val rawMessage: String
)

data class CategoryTotalRow(
    val category: String,
    val total: Double
)

data class MonthlyTotalRow(
    val yearMonth: String,
    val income: Double,
    val expense: Double,
    val fees: Double
)

data class PeriodTotalsRow(
    val income: Double,
    val expense: Double,
    val fees: Double,
    val transferOut: Double,
    val transferIn: Double,
    val expenseCount: Int
)

data class FeeByKindRow(
    val kind: String,
    val fees: Double,
    val transactionCount: Int
)

data class DayOfWeekSpendRow(
    val dayOfWeek: Int,
    val total: Double
)

data class CounterpartySpendRow(
    val counterparty: String,
    val total: Double,
    val transactionCount: Int
)
