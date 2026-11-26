package com.finance.myledger.ui.analytics.time.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.finance.myledger.domain.model.WeekdayAverage
import com.finance.myledger.ui.common.formatCompact
import com.finance.myledger.ui.dashboard.components.formatKes

// Monday-first, matching how a Kenyan week is usually read. Values are SQLite %w (0 = Sunday).
private val WEEK_ORDER = listOf(1, 2, 3, 4, 5, 6, 0)
private val SHORT = mapOf(0 to "Sun", 1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat")
private val LONG = mapOf(
    0 to "Sundays", 1 to "Mondays", 2 to "Tuesdays", 3 to "Wednesdays",
    4 to "Thursdays", 5 to "Fridays", 6 to "Saturdays"
)

/**
 * Average spend per weekday — total for that weekday divided by how many times it occurred in
 * the range, so a month with five Saturdays doesn't make Saturday look worse than it is.
 */
@Composable
fun WeekdayAverageChart(
    days: List<WeekdayAverage>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Spending by weekday", style = MaterialTheme.typography.titleMedium)
            Text(
                "Average spent on each day of the week",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val byDay = days.associateBy { it.dayOfWeek }
            val ordered = WEEK_ORDER.map { byDay[it] ?: WeekdayAverage(it, 0.0, 0) }
            val totalSpend = ordered.sumOf { it.total }
            if (totalSpend <= 0.0) {
                Text("No spending in this range", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            val peak = ordered.maxBy { it.average }
            val typicalDay = totalSpend / ordered.sumOf { it.occurrences }.coerceAtLeast(1)
            Text(
                text = "Most on ${LONG[peak.dayOfWeek]}: ${formatKes(peak.average)} on average",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "A typical day is ${formatKes(typicalDay)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val maxVal = peak.average.coerceAtLeast(1.0)
            val strong = MaterialTheme.colorScheme.primary
            val muted = strong.copy(alpha = 0.4f)
            val labelColor = MaterialTheme.colorScheme.onSurface

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                val gap = 8.dp.toPx()
                val labelH = 20.dp.toPx()
                val valueH = 16.dp.toPx()
                val chartH = size.height - labelH - valueH
                val barW = (size.width - gap * 8) / 7
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 10.dp.toPx()
                    isAntiAlias = true
                }
                ordered.forEachIndexed { index, day ->
                    val h = (day.average / maxVal * chartH).toFloat()
                    val left = gap + index * (barW + gap)
                    val top = valueH + chartH - h
                    drawRoundRect(
                        color = if (day.dayOfWeek == peak.dayOfWeek) strong else muted,
                        topLeft = Offset(left, top),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    if (day.average > 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            formatCompact(day.average), left + barW / 2f, top - 4.dp.toPx(), paint
                        )
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        SHORT.getValue(day.dayOfWeek), left + barW / 2f, size.height - 4.dp.toPx(), paint
                    )
                }
            }
        }
    }
}
