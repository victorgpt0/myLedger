package com.finance.myledger.ui.analytics.time.components

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
import androidx.compose.ui.unit.dp
import com.finance.myledger.ui.dashboard.components.formatKes
import java.util.Locale

@Composable
fun FeesOverTimeCard(
    totalFees: Double,
    feesPercentOfMoved: Double,
    transferTotal: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Fees & transfers", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Fees paid: ${formatKes(totalFees)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = String.format(
                    Locale.US,
                    "%.1f%% of money moved (income + expense)",
                    feesPercentOfMoved
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Transfers (wallets/savings): ${formatKes(transferTotal)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Kept separate from spend — not counted as expense.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
