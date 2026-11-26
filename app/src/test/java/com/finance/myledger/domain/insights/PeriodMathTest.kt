package com.finance.myledger.domain.insights

import com.finance.myledger.domain.model.ComparisonBasis
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.model.DateRangeBounds
import com.finance.myledger.domain.model.toBounds
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PeriodMathTest {

    private lateinit var originalTz: TimeZone

    @Before
    fun setUp() {
        originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Nairobi"))
    }

    @After
    fun tearDown() = TimeZone.setDefault(originalTz)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, 0, 0)
        }.timeInMillis

    @Test
    fun effectiveSpan_clampsToFirstTransactionAndNow() {
        val now = at(2026, 11, 26)
        val first = at(2026, 11, 10)
        val span = PeriodMath.effectiveSpan(DateRangeBounds(0L, Long.MAX_VALUE), first, now)!!
        assertThat(span.startMillis).isEqualTo(first)
        assertThat(span.endMillis).isEqualTo(now + 1)
        assertThat(PeriodMath.effectiveSpan(DateRangeBounds(0L, 1L), null, now)).isNull()
    }

    @Test
    fun weekdayOccurrences_countsEachDayOnce() {
        // Sun 1 Nov – Mon 30 Nov 2026: five Sundays and five Mondays, four of everything else.
        val span = DateRangeBounds(at(2026, 11, 1, 0), at(2026, 12, 1, 0))
        val counts = PeriodMath.weekdayOccurrences(span)
        assertThat(counts.toList()).containsExactly(5, 5, 4, 4, 4, 4, 4).inOrder()
        assertThat(PeriodMath.dayCount(span)).isEqualTo(30)
    }

    @Test
    fun weekdayOccurrences_partialDayCounts() {
        val span = DateRangeBounds(at(2026, 11, 26, 9), at(2026, 11, 26, 10))
        assertThat(PeriodMath.dayCount(span)).isEqualTo(1)
    }

    @Test
    fun previousPeriod_thisMonth_isSameElapsedDaysLastMonth() {
        val now = at(2026, 11, 26)
        val cmp = PeriodMath.previousPeriod(DateRange.ThisMonth, at(2025, 1, 1), now)!!
        assertThat(cmp.basis).isEqualTo(ComparisonBasis.SAME_POINT_LAST_MONTH)
        assertThat(cmp.bounds.startMillis).isEqualTo(at(2026, 10, 1, 0))
        assertThat(cmp.bounds.endMillis).isEqualTo(at(2026, 10, 26) + 1)
    }

    @Test
    fun previousPeriod_thisMonth_neverSpillsIntoCurrentMonth() {
        // 31 March vs a 28-day February: the comparison must stop at 1 March.
        val now = at(2027, 3, 31)
        val cmp = PeriodMath.previousPeriod(DateRange.ThisMonth, at(2025, 1, 1), now)!!
        assertThat(cmp.bounds.startMillis).isEqualTo(at(2027, 2, 1, 0))
        assertThat(cmp.bounds.endMillis).isEqualTo(at(2027, 3, 1, 0))
    }

    @Test
    fun previousPeriod_lastMonth_isTheMonthBefore() {
        val cmp = PeriodMath.previousPeriod(DateRange.LastMonth, at(2025, 1, 1), at(2026, 11, 26))!!
        assertThat(cmp.basis).isEqualTo(ComparisonBasis.MONTH_BEFORE)
        assertThat(cmp.bounds).isEqualTo(DateRangeBounds(at(2026, 9, 1, 0), at(2026, 10, 1, 0)))
    }

    @Test
    fun previousPeriod_rollingRange_shiftsByItsLength() {
        val now = at(2026, 11, 26)
        val bounds = DateRange.Last3Months.toBoundsFor(now)
        val cmp = PeriodMath.previousPeriod(DateRange.Last3Months, at(2025, 1, 1), now)!!
        val length = bounds.endMillis - bounds.startMillis
        assertThat(cmp.basis).isEqualTo(ComparisonBasis.PREVIOUS_EQUAL_PERIOD)
        assertThat(cmp.bounds).isEqualTo(DateRangeBounds(bounds.startMillis - length, bounds.startMillis))
    }

    @Test
    fun previousPeriod_nullWithoutEnoughHistoryOrForAllTime() {
        val now = at(2026, 11, 26)
        assertThat(PeriodMath.previousPeriod(DateRange.ThisMonth, at(2026, 10, 15), now)).isNull()
        assertThat(PeriodMath.previousPeriod(DateRange.AllTime, at(2020, 1, 1), now)).isNull()
        assertThat(PeriodMath.previousPeriod(DateRange.ThisMonth, null, now)).isNull()
    }

    @Test
    fun previousPeriod_historyStartingMidDayStillCounts() {
        // First transaction at 15:00 on 1 Oct still covers a comparison that starts at 00:00.
        val cmp = PeriodMath.previousPeriod(DateRange.ThisMonth, at(2026, 10, 1, 15), at(2026, 11, 26))
        assertThat(cmp).isNotNull()
    }

    @Test
    fun percentChange_nullWithoutBaseline() {
        assertThat(PeriodMath.percentChange(150.0, 100.0)).isWithin(1e-9).of(50.0)
        assertThat(PeriodMath.percentChange(50.0, 100.0)).isWithin(1e-9).of(-50.0)
        assertThat(PeriodMath.percentChange(50.0, 0.0)).isNull()
    }

    @Test
    fun projectedMonthEnd_scalesDailyRateToMonthLength() {
        // 10 days into a 30-day month, 1,000 spent → 3,000 projected.
        val projected = PeriodMath.projectedMonthEnd(DateRange.ThisMonth, 1_000.0, at(2026, 11, 10))
        assertThat(projected).isWithin(1e-6).of(3_000.0)
        assertThat(PeriodMath.projectedMonthEnd(DateRange.ThisMonth, 1_000.0, at(2026, 11, 2))).isNull()
        assertThat(PeriodMath.projectedMonthEnd(DateRange.LastMonth, 1_000.0, at(2026, 11, 10))).isNull()
    }

    private fun DateRange.toBoundsFor(now: Long) = toBounds(now)
}
