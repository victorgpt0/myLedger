package com.finance.myledger.domain.model

import java.util.Calendar

/**
 * Represents a period to analyze. Presets cover the common cases; [Custom] covers the rest.
 */
sealed class DateRange {
    data object ThisMonth : DateRange()
    data object LastMonth : DateRange()
    data object Last3Months : DateRange()
    data object Last6Months : DateRange()
    data object ThisYear : DateRange()
    data object AllTime : DateRange()
    data class Custom(val startMillis: Long, val endMillis: Long) : DateRange()
}

/**
 * Concrete half-open bounds `[startMillis, endMillis)` for repository/DAO queries.
 * [DateRange.AllTime] maps to `0L` .. [Long.MAX_VALUE]; callers that want actual table
 * min/max can refine later without changing this API.
 */
data class DateRangeBounds(
    val startMillis: Long,
    val endMillis: Long
)

fun DateRange.toBounds(nowMillis: Long = System.currentTimeMillis()): DateRangeBounds {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return when (this) {
        DateRange.ThisMonth -> {
            val start = cal.startOfMonth()
            DateRangeBounds(start, cal.apply { timeInMillis = start; add(Calendar.MONTH, 1) }.timeInMillis)
        }
        DateRange.LastMonth -> {
            cal.add(Calendar.MONTH, -1)
            val start = cal.startOfMonth()
            DateRangeBounds(start, cal.apply { timeInMillis = start; add(Calendar.MONTH, 1) }.timeInMillis)
        }
        DateRange.Last3Months -> {
            val end = nowMillis + 1
            cal.add(Calendar.MONTH, -3)
            DateRangeBounds(cal.startOfMonth(), end)
        }
        DateRange.Last6Months -> {
            val end = nowMillis + 1
            cal.add(Calendar.MONTH, -6)
            DateRangeBounds(cal.startOfMonth(), end)
        }
        DateRange.ThisYear -> {
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.clearTime()
            val start = cal.timeInMillis
            cal.add(Calendar.YEAR, 1)
            DateRangeBounds(start, cal.timeInMillis)
        }
        DateRange.AllTime -> DateRangeBounds(0L, Long.MAX_VALUE)
        is DateRange.Custom -> DateRangeBounds(startMillis, endMillis)
    }
}

private fun Calendar.startOfMonth(): Long {
    set(Calendar.DAY_OF_MONTH, 1)
    clearTime()
    return timeInMillis
}

private fun Calendar.clearTime() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
