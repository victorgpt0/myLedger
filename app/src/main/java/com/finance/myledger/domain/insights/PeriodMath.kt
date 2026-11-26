package com.finance.myledger.domain.insights

import com.finance.myledger.domain.model.ComparisonBasis
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.model.DateRangeBounds
import com.finance.myledger.domain.model.toBounds
import java.util.Calendar

/**
 * Pure calendar math behind the analytics pages. All calendar work uses the device's default
 * time zone, which matches the `'localtime'` modifier used in the DAO's date grouping.
 */
object PeriodMath {

    /** Previous-period bounds plus what they represent. */
    data class Comparison(val bounds: DateRangeBounds, val basis: ComparisonBasis)

    /**
     * The part of [bounds] that actually has data: starts no earlier than the first stored
     * transaction and ends no later than now. Null when there's nothing stored yet or the
     * range is entirely in the future / before the first transaction.
     */
    fun effectiveSpan(bounds: DateRangeBounds, firstTimestamp: Long?, nowMillis: Long): DateRangeBounds? {
        if (firstTimestamp == null) return null
        val start = maxOf(bounds.startMillis, firstTimestamp)
        val end = minOf(bounds.endMillis, nowMillis + 1)
        return if (end > start) DateRangeBounds(start, end) else null
    }

    /**
     * The period to compare [range] against. Partial periods (this month / this year) are
     * compared with the same elapsed stretch of the previous month / year, not a full one.
     * Returns null for [DateRange.AllTime] or when the previous period starts before the first
     * stored transaction, since comparing against missing history would be misleading.
     */
    fun previousPeriod(range: DateRange, firstTimestamp: Long?, nowMillis: Long): Comparison? {
        if (firstTimestamp == null) return null
        val bounds = range.toBounds(nowMillis)
        val comparison = when (range) {
            DateRange.AllTime -> return null
            DateRange.ThisMonth -> samePointLast(Calendar.MONTH, bounds.startMillis, nowMillis)
                ?.let { Comparison(it, ComparisonBasis.SAME_POINT_LAST_MONTH) }
            DateRange.ThisYear -> samePointLast(Calendar.YEAR, bounds.startMillis, nowMillis)
                ?.let { Comparison(it, ComparisonBasis.SAME_POINT_LAST_YEAR) }
            DateRange.LastMonth -> {
                val start = calendarAt(bounds.startMillis).apply { add(Calendar.MONTH, -1) }.timeInMillis
                Comparison(DateRangeBounds(start, bounds.startMillis), ComparisonBasis.MONTH_BEFORE)
            }
            DateRange.Last3Months, DateRange.Last6Months, is DateRange.Custom -> {
                val span = effectiveSpan(bounds, firstTimestamp, nowMillis) ?: return null
                val length = span.endMillis - span.startMillis
                Comparison(
                    DateRangeBounds(span.startMillis - length, span.startMillis),
                    ComparisonBasis.PREVIOUS_EQUAL_PERIOD
                )
            }
        } ?: return null
        return comparison.takeIf { it.bounds.startMillis >= startOfDay(firstTimestamp) }
    }

    /** Number of calendar days the span touches (a partial day counts as one). */
    fun dayCount(span: DateRangeBounds): Int = weekdayOccurrences(span).sum()

    /**
     * How many times each weekday occurs in [span]; index 0 = Sunday … 6 = Saturday,
     * matching SQLite's `strftime('%w')`.
     */
    fun weekdayOccurrences(span: DateRangeBounds): IntArray {
        val counts = IntArray(7)
        val cal = calendarAt(startOfDay(span.startMillis))
        while (cal.timeInMillis < span.endMillis) {
            counts[cal.get(Calendar.DAY_OF_WEEK) - 1]++
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return counts
    }

    /** Percent change from [previous] to [current]; null when there's no baseline. */
    fun percentChange(current: Double, previous: Double): Double? =
        if (previous <= 0.0) null else (current - previous) / previous * 100.0

    /**
     * Straight-line projection of this month's spend to month end. Null for other ranges and
     * during the first few days, when a projection is mostly noise.
     */
    fun projectedMonthEnd(range: DateRange, expenseSoFar: Double, nowMillis: Long, minDays: Int = 3): Double? {
        if (range != DateRange.ThisMonth) return null
        val bounds = range.toBounds(nowMillis)
        val elapsedDays = dayCount(DateRangeBounds(bounds.startMillis, nowMillis + 1))
        if (elapsedDays < minDays) return null
        val daysInMonth = calendarAt(nowMillis).getActualMaximum(Calendar.DAY_OF_MONTH)
        return expenseSoFar / elapsedDays * daysInMonth
    }

    private fun samePointLast(field: Int, periodStart: Long, nowMillis: Long): DateRangeBounds? {
        val prevStart = calendarAt(periodStart).apply { add(field, -1) }.timeInMillis
        val elapsed = nowMillis + 1 - periodStart
        // Clamp so e.g. 31 days into March never spills past the end of February.
        val prevEnd = minOf(prevStart + elapsed, periodStart)
        return if (prevEnd > prevStart) DateRangeBounds(prevStart, prevEnd) else null
    }

    private fun startOfDay(millis: Long): Long = calendarAt(millis).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun calendarAt(millis: Long): Calendar = Calendar.getInstance().apply { timeInMillis = millis }
}
