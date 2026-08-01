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
import com.finance.myledger.domain.model.DayOfWeekSpend

@Composable
fun DayOfWeekSpendChart(
    days: List<DayOfWeekSpend>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Spend by day of week", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            if (days.isEmpty()) {
                Text("No expense data in this range", style = MaterialTheme.typography.bodyMedium)
            } else {
                val totals = (0..6).map { dow ->
                    days.firstOrNull { it.dayOfWeek == dow }?.total ?: 0.0
                }
                val maxVal = totals.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val barColor = MaterialTheme.colorScheme.primary
                val labelColor = MaterialTheme.colorScheme.onSurface
                val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val gap = 8.dp.toPx()
                    val labelH = 24.dp.toPx()
                    val chartH = size.height - labelH
                    val barW = (size.width - gap * 8) / 7
                    totals.forEachIndexed { index, total ->
                        val h = (total / maxVal * chartH).toFloat()
                        val left = gap + index * (barW + gap)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, chartH - h),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            labels[index],
                            left + barW / 2f,
                            size.height - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = labelColor.toArgb()
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 10.dp.toPx()
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}
