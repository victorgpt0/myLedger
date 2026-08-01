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
 */
@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        ORDER BY timestamp DESC
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
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeCategoryTotalsInRange(startMillis: Long, endMillis: Long): Flow<List<CategoryTotalRow>>

    @Query(
        """
        SELECT balanceAfter FROM transactions
        WHERE balanceAfter IS NOT NULL
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    fun observeLatestBalance(): Flow<Double?>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
        """
    )
    fun observeExpenseTotalInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'INCOME'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
        """
    )
    fun observeIncomeTotalInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'TRANSFER'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
        """
    )
    fun observeTransferTotalInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(feeAmount), 0) FROM transactions
        WHERE feeAmount IS NOT NULL
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
        """
    )
    fun observeTotalFeesInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type IN ('INCOME', 'EXPENSE')
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
        """
    )
    fun observeMoneyMovedInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT
          strftime('%Y-%m', timestamp / 1000, 'unixepoch') AS yearMonth,
          SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) AS income,
          SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS expense,
          SUM(COALESCE(feeAmount, 0)) AS fees
        FROM transactions
        WHERE type != 'TRANSFER'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """
    )
    fun observeMonthlyTrend(startMillis: Long, endMillis: Long): Flow<List<MonthlyTotalRow>>

    @Query(
        """
        SELECT
          CAST(strftime('%w', timestamp / 1000, 'unixepoch') AS INTEGER) AS dayOfWeek,
          SUM(amount) AS total
        FROM transactions
        WHERE type = 'EXPENSE'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek ASC
        """
    )
    fun observeDayOfWeekSpend(startMillis: Long, endMillis: Long): Flow<List<DayOfWeekSpendRow>>

    @Query(
        """
        SELECT counterparty,
               SUM(amount) AS total,
               COUNT(*) AS transactionCount
        FROM transactions
        WHERE type = 'EXPENSE'
          AND timestamp >= :startMillis
          AND timestamp < :endMillis
        GROUP BY counterparty
        ORDER BY total DESC
        LIMIT :limit
        """
    )
    fun observeTopCounterparties(
        startMillis: Long,
        endMillis: Long,
        limit: Int
    ): Flow<List<CounterpartySpendRow>>
}
