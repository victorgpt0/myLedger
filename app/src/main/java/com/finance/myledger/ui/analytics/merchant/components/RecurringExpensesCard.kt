package com.finance.myledger.ui.analytics.merchant.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finance.myledger.domain.model.RecurringExpense
import com.finance.myledger.ui.dashboard.components.formatKes

@Composable
fun RecurringExpensesCard(
    recurring: List<RecurringExpense>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Likely recurring", style = MaterialTheme.typography.titleMedium)
            Text(
                "Heuristic — same counterparty in 3+ of last 4 months, ±10% amount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (recurring.isEmpty()) {
                Text("No recurring pattern detected", style = MaterialTheme.typography.bodyMedium)
            } else {
                recurring.forEach { item ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = item.counterparty,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "~${formatKes(item.averageAmount)} · seen ${item.monthsSeen} months",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
