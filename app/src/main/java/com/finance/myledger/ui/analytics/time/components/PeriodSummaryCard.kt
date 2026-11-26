package com.finance.myledger.ui.analytics.time.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finance.myledger.domain.model.ComparisonBasis
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.usecase.ObserveTimeAnalyticsUseCase.TimeAnalytics
import com.finance.myledger.ui.common.formatChange
import com.finance.myledger.ui.dashboard.components.formatKes
import com.finance.myledger.ui.theme.ExpenseRed
import com.finance.myledger.ui.theme.IncomeGreen
import kotlin.math.abs

/**
 * Headline numbers for the selected range: spent / received / net, each compared with the
 * previous period, plus daily average and (this month only) a month-end projection.
 */
@Composable
fun PeriodSummaryCard(
    analytics: TimeAnalytics,
    range: DateRange,
    modifier: Modifier = Modifier
) {
    val current = analytics.current
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Summary", style = MaterialTheme.typography.titleMedium)
            Text(
                text = comparisonLabel(analytics.comparisonBasis, range),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Metric(
                    label = "Spent",
                    value = formatKes(current.expense),
                    change = analytics.expenseChangePercent,
                    previous = analytics.previous?.expense,
                    higherIsGood = false,
                    modifier = Modifier.weight(1f)
                )
                Metric(
                    label = "Received",
                    value = formatKes(current.income),
                    change = analytics.incomeChangePercent,
                    previous = analytics.previous?.income,
                    higherIsGood = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            val net = current.net
            Text(
                text = "Net ${if (net >= 0) "+" else "−"}${formatKes(abs(net))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (net >= 0) IncomeGreen else ExpenseRed
            )
            Text(
                text = if (net >= 0) "You received more than you spent" else "You spent more than you received",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "${formatKes(analytics.averageDailySpend)} a day on average",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${current.expenseCount} payments over ${analytics.daysInPeriod} days" +
                    if (current.expenseCount > 0) " · avg ${formatKes(current.expense / current.expenseCount)} each" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            analytics.projectedMonthEnd?.let { projected ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "On pace to spend ${formatKes(projected)} this month",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Assumes the same daily rate for the rest of the month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    change: Double?,
    previous: Double?,
    higherIsGood: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        val changeText = formatChange(change)
        if (changeText != null) {
            val good = (change!! >= 0) == higherIsGood
            Text(
                text = changeText,
                style = MaterialTheme.typography.labelLarge,
                color = if (good) IncomeGreen else ExpenseRed
            )
        } else if (previous != null) {
            // Previous period exists but was zero — a percentage would be meaningless.
            Text(
                "Nothing in prior period",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (previous != null) {
            Text(
                text = "was ${formatKes(previous)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun comparisonLabel(basis: ComparisonBasis?, range: DateRange): String = when (basis) {
    ComparisonBasis.SAME_POINT_LAST_MONTH -> "Compared with the same days last month"
    ComparisonBasis.MONTH_BEFORE -> "Compared with the month before"
    ComparisonBasis.SAME_POINT_LAST_YEAR -> "Compared with the same point last year"
    ComparisonBasis.PREVIOUS_EQUAL_PERIOD -> "Compared with the previous period of equal length"
    null -> if (range == DateRange.AllTime) "Everything on record" else "Not enough history yet to compare"
}
