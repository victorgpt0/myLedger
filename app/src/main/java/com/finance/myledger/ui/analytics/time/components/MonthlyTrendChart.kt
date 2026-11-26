package com.finance.myledger.ui.analytics.time.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finance.myledger.domain.model.MonthlyTotal
import com.finance.myledger.ui.common.formatCompact
import com.finance.myledger.ui.dashboard.components.formatKes
import com.finance.myledger.ui.theme.ExpenseRed
import com.finance.myledger.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Locale

private const val MAX_MONTHS = 12

/**
 * Received vs spent per month. Tap a month to see its exact figures and net; the latest month
 * is selected by default. Long ranges show the most recent [MAX_MONTHS] months.
 */
@Composable
fun MonthlyTrendChart(
    months: List<MonthlyTotal>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Month by month", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (months.size > MAX_MONTHS) "Latest $MAX_MONTHS months · tap a month for details"
                else "Tap a month for details · wallet transfers excluded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (months.isEmpty()) {
                Text("No data in this range", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            val shown = months.takeLast(MAX_MONTHS)
            var selected by remember(shown) { mutableIntStateOf(shown.lastIndex) }
            val month = shown[selected.coerceIn(0, shown.lastIndex)]

            MonthDetail(month)
            Spacer(modifier = Modifier.height(12.dp))
            Legend()
            Spacer(modifier = Modifier.height(8.dp))

            val maxVal = shown.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0)
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            val highlight = MaterialTheme.colorScheme.surfaceVariant
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(shown) {
                        detectTapGestures { offset ->
                            val groupWidth = size.width.toFloat() / shown.size
                            selected = (offset.x / groupWidth).toInt().coerceIn(0, shown.lastIndex)
                        }
                    }
            ) {
                val labelH = 20.dp.toPx()
                val topPad = 14.dp.toPx()
                val chartH = size.height - labelH - topPad
                val baseline = topPad + chartH
                val groupWidth = size.width / shown.size
                val barWidth = (groupWidth * 0.32f).coerceAtMost(18.dp.toPx())
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 10.dp.toPx()
                    isAntiAlias = true
                }

                // Scale: top gridline labelled with the max value so bar heights mean something.
                drawLine(gridColor, Offset(0f, topPad), Offset(size.width, topPad), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(formatCompact(maxVal), 0f, topPad - 3.dp.toPx(), paint)

                paint.textAlign = android.graphics.Paint.Align.CENTER
                shown.forEachIndexed { index, m ->
                    val groupLeft = index * groupWidth
                    val center = groupLeft + groupWidth / 2f
                    if (index == selected) {
                        drawRoundRect(
                            color = highlight,
                            topLeft = Offset(groupLeft + 2.dp.toPx(), topPad),
                            size = Size(groupWidth - 4.dp.toPx(), chartH),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                    val incomeH = (m.income / maxVal * chartH).toFloat()
                    val expenseH = (m.expense / maxVal * chartH).toFloat()
                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(center - barWidth - 1.dp.toPx(), baseline - incomeH),
                        size = Size(barWidth, incomeH),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                    drawRoundRect(
                        color = ExpenseRed,
                        topLeft = Offset(center + 1.dp.toPx(), baseline - expenseH),
                        size = Size(barWidth, expenseH),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                    paint.isFakeBoldText = index == selected
                    drawContext.canvas.nativeCanvas.drawText(
                        shortMonth(m.yearMonth),
                        center,
                        size.height - 4.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDetail(month: MonthlyTotal) {
    val net = month.income - month.expense
    Column {
        Text(longMonth(month.yearMonth), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text("Received", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatKes(month.income), style = MaterialTheme.typography.bodyMedium, color = IncomeGreen)
            }
            Column {
                Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatKes(month.expense), style = MaterialTheme.typography.bodyMedium, color = ExpenseRed)
            }
            Column {
                Text("Net", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = (if (net >= 0) "+" else "−") + formatCompact(kotlin.math.abs(net)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (net >= 0) IncomeGreen else ExpenseRed
                )
            }
        }
        if (month.fees > 0) {
            Text(
                text = "incl. ${formatKes(month.fees)} in fees",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendDot(IncomeGreen)
        Text(" Received", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.width(12.dp))
        LegendDot(ExpenseRed)
        Text(" Spent", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
}

private fun parseYearMonth(yearMonth: String) =
    runCatching { SimpleDateFormat("yyyy-MM", Locale.US).parse(yearMonth) }.getOrNull()

private fun shortMonth(yearMonth: String): String =
    parseYearMonth(yearMonth)?.let { SimpleDateFormat("MMM", Locale.getDefault()).format(it) } ?: yearMonth

private fun longMonth(yearMonth: String): String =
    parseYearMonth(yearMonth)?.let { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(it) } ?: yearMonth
