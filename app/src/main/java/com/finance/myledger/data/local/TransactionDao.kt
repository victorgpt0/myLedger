package com.finance.myledger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for transactions. Only the data layer talks to Room;
 * repository maps these entities to domain models before they leave this layer.
 *
 * Income/expense aggregations exclude TRANSFER so self-wallet movements are not double-counted.
 * Date grouping uses `'localtime'` so buckets follow the device's day/month, not UTC
 * (otherwise 00:00–02:59 EAT lands on the previous day).
 */
/**
 * SQL condition for rows that count towards income/expense/fee totals: excludes reversal rows and
 * the transactions they reversed, which together net to nothing. Every aggregate query must
 * include it; transaction lists and the balance deliberately don't (they show what happened).
 */
const val COUNTED =
    "kind != 'REVERSAL' AND id NOT IN (SELECT reversesId FROM transactions WHERE reversesId IS NOT NULL)"

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, rowid DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, rowid DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        ORDER BY timestamp DESC, rowid DESC
        """
    )
    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT category, SUM(amount) AS total
        FROM transactions
        WHERE type = 'EXPENSE'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
          AND $COUNTED
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeCategoryTotalsInRange(startMillis: Long, endMillis: Long): Flow<List<CategoryTotalRow>>

    /**
     * All balance-bearing rows at the newest timestamp. Several can tie (minute-precision SMS
     * times); the repository resolves the true latest via the balance chain. Ordered by rowid
     * DESC so the fallback is the most recently inserted row.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE balanceAfter IS NOT NULL
          AND timestamp = (SELECT MAX(timestamp) FROM transactions WHERE balanceAfter IS NOT NULL)
        ORDER BY rowid DESC
        """
    )
    fun observeLatestBalanceCandidates(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
          AND $COUNTED
        """
    )
    fun observeExpenseTotalInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'INCOME'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
          AND $COUNTED
        """
    )
    fun observeIncomeTotalInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(feeAmount), 0) FROM transactions
        WHERE feeAmount IS NOT NULL
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
          AND $COUNTED
        """
    )
    fun observeTotalFeesInRange(startMillis: Long, endMillis: Long): Flow<Double>

    /**
     * Headline totals for one period in a single pass. Transfers are split by direction so
     * wallet deposits and withdrawals don't add up to a misleading combined figure.
     */
    @Query(
        """
        SELECT
          COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount END), 0) AS income,
          COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount END), 0) AS expense,
          COALESCE(SUM(feeAmount), 0) AS fees,
          COALESCE(SUM(CASE WHEN type = 'TRANSFER' AND direction = 'OUT' THEN amount END), 0) AS transferOut,
          COALESCE(SUM(CASE WHEN type = 'TRANSFER' AND direction = 'IN' THEN amount END), 0) AS transferIn,
          COUNT(CASE WHEN type = 'EXPENSE' THEN 1 END) AS expenseCount
        FROM transactions
        WHERE timestamp >= :startMillis
          AND timestamp < :endMillis
          AND $COUNTED
        """
    )
    fun observePeriodTotals(startMillis: Long, endMillis: Long): Flow<PeriodTotalsRow>

    /** Fees grouped by transaction kind; kinds that cost nothing are omitted. */
    @Query(
        """
        SELECT kind,
               COALESCE(SUM(feeAmount), 0) AS fees,
               COUNT(*) AS transactionCount
        FROM transactions
        WHERE timestamp >= :startMillis
          AND timestamp < :endMillis
          AND $COUNTED
        GROUP BY kind
        HAVING fees > 0
        ORDER BY fees DESC
        """
    )
    fun observeFeesByKind(startMillis: Long, endMillis: Long): Flow<List<FeeByKindRow>>

    /** Earliest stored transaction — bounds "All time" and per-day averages to real data. */
    @Query("SELECT MIN(timestamp) FROM transactions")
    fun observeFirstTimestamp(): Flow<Long?>

    @Query(
        """
        SELECT
          strftime('%Y-%m', timestamp / 1000, 'unixepoch', 'localtime') AS yearMonth,
          SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) AS income,
          SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS expense,
          SUM(COALESCE(feeAmount, 0)) AS fees
        FROM transactions
        WHERE type != 'TRANSFER'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
          AND $COUNTED
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """
    )
    fun observeMonthlyTrend(startMillis: Long, endMillis: Long): Flow<List<MonthlyTotalRow>>

    @Query(
        """
        SELECT
          CAST(strftime('%w', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) AS dayOfWeek,
          SUM(amount) AS total
        FROM transactions
        WHERE type = 'EXPENSE'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
          AND $COUNTED
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek ASC
        """
    )
    fun observeDayOfWeekSpend(startMillis: Long, endMillis: Long): Flow<List<DayOfWeekSpendRow>>

    /**
     * Rows that count towards totals ([COUNTED]) in a range — for aggregations done in Kotlin
     * (counterparty grouping by normalised name, recurring detection).
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE timestamp >= :startMillis
          AND timestamp < :endMillis
          AND $COUNTED
        ORDER BY timestamp DESC, rowid DESC
        """
    )
    fun observeCountedInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>
}
