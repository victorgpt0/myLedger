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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.finance.myledger.domain.model.MonthlyTotal
import com.finance.myledger.ui.theme.ExpenseRed
import com.finance.myledger.ui.theme.IncomeGreen

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
            Text("Monthly trend", style = MaterialTheme.typography.titleMedium)
            Text(
                "Income vs expense (transfers excluded)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (months.isEmpty()) {
                Text("No data in this range", style = MaterialTheme.typography.bodyMedium)
            } else {
                val maxVal = months.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val groupWidth = size.width / months.size
                    val barWidth = groupWidth * 0.3f
                    months.forEachIndexed { index, month ->
                        val x = index * groupWidth + groupWidth * 0.2f
                        val incomeH = (month.income / maxVal * size.height).toFloat()
                        val expenseH = (month.expense / maxVal * size.height).toFloat()
                        drawRect(
                            color = IncomeGreen,
                            topLeft = Offset(x, size.height - incomeH),
                            size = Size(barWidth, incomeH)
                        )
                        drawRect(
                            color = ExpenseRed,
                            topLeft = Offset(x + barWidth + 4.dp.toPx(), size.height - expenseH),
                            size = Size(barWidth, expenseH)
                        )
                    }
                }
            }
        }
    }
}
