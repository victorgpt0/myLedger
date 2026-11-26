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
import com.finance.myledger.domain.model.PeriodTotals
import com.finance.myledger.ui.dashboard.components.formatKes
import com.finance.myledger.ui.theme.ExpenseRed
import com.finance.myledger.ui.theme.IncomeGreen
import kotlin.math.abs

/**
 * Money moved between M-PESA and the user's own wallets (ZIIDI, M-Shwari, …), split by
 * direction so a deposit and a later withdrawal aren't summed into one misleading figure.
 */
@Composable
fun WalletTransfersCard(
    totals: PeriodTotals,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Savings & wallets", style = MaterialTheme.typography.titleMedium)
            Text(
                "ZIIDI, M-Shwari and similar — not counted as spending or income",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (totals.transferOut <= 0.0 && totals.transferIn <= 0.0) {
                Text("No wallet transfers in this range", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            Line("Moved into wallets", formatKes(totals.transferOut))
            Line("Taken back out", formatKes(totals.transferIn))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val net = totals.netToWallets
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (net >= 0) "Net put away" else "Net drawn down",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatKes(abs(net)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (net >= 0) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
