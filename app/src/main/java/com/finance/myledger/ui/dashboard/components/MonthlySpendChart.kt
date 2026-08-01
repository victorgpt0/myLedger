package com.finance.myledger.ui.dashboard.components

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
import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.CategorySpend

/**
 * Monthly spend card with a Canvas-drawn category bar chart (no third-party charting lib).
 */
@Composable
fun MonthlySpendChart(
    monthlyTotal: Double,
    categorySpend: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Spend",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatKes(monthlyTotal),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (categorySpend.isEmpty()) {
                Text(
                    text = "No outbound spend this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val barColor = MaterialTheme.colorScheme.primary
                val labelColor = MaterialTheme.colorScheme.onSurface
                val maxTotal = categorySpend.maxOf { it.total }.coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val barCount = categorySpend.size
                    val gap = 12.dp.toPx()
                    val labelHeight = 28.dp.toPx()
                    val chartHeight = size.height - labelHeight
                    val barWidth = (size.width - gap * (barCount + 1)) / barCount

                    categorySpend.forEachIndexed { index, spend ->
                        val barHeight = (spend.total / maxTotal * chartHeight).toFloat()
                        val left = gap + index * (barWidth + gap)
                        val top = chartHeight - barHeight

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )

                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = labelColor.toArgb()
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 10.dp.toPx()
                                isAntiAlias = true
                            }
                            drawText(
                                shortLabel(spend.category),
                                left + barWidth / 2f,
                                size.height - 4.dp.toPx(),
                                paint
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shortLabel(category: Category): String = when (category) {
    Category.AIRTIME -> "Airtime"
    Category.UTILITIES -> "Utils"
    Category.SHOPPING -> "Shop"
    Category.TRANSPORT -> "Transit"
    Category.PERSON_TO_PERSON -> "P2P"
    Category.OTHER -> "Other"
}
