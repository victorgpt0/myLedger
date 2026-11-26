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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finance.myledger.domain.model.FeeByKind
import com.finance.myledger.domain.model.TransactionKind
import com.finance.myledger.ui.dashboard.components.formatKes
import java.util.Locale

/**
 * Where transaction fees come from, per M-PESA product, with each kind's share of the total
 * and the average fee per transaction. Calls out the biggest fee source in plain words.
 */
@Composable
fun FeesBreakdownCard(
    fees: List<FeeByKind>,
    expenseTotal: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Fees", style = MaterialTheme.typography.titleMedium)
            val total = fees.sumOf { it.totalFees }
            if (total <= 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("No transaction fees in this range", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            Text(
                text = formatKes(total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (expenseTotal > 0) {
                Text(
                    text = String.format(Locale.US, "Equal to %.1f%% of what you spent", total / expenseTotal * 100),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val top = fees.maxBy { it.totalFees }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${top.transactionCount} ${kindLabel(top.kind).lowercase()} cost you ${formatKes(top.totalFees)}" +
                    " — ${formatKes(top.totalFees / top.transactionCount.coerceAtLeast(1))} each on average",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            fees.forEach { fee ->
                val share = (fee.totalFees / total).toFloat()
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(kindLabel(fee.kind), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatKes(fee.totalFees),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { share },
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {}
                    )
                    Text(
                        text = String.format(Locale.US, "%.0f%% of fees · %d transactions", share * 100, fee.transactionCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun kindLabel(kind: TransactionKind): String = when (kind) {
    TransactionKind.SEND_MONEY -> "Sends"
    TransactionKind.PAYBILL -> "Paybill payments"
    TransactionKind.BUY_GOODS -> "Till payments"
    TransactionKind.WITHDRAWAL -> "Agent withdrawals"
    TransactionKind.AIRTIME -> "Airtime purchases"
    TransactionKind.RECEIVED -> "Receipts"
    TransactionKind.DEPOSIT -> "Cash deposits"
    TransactionKind.DIVIDEND -> "Dividends"
    TransactionKind.REVERSAL -> "Reversals"
    TransactionKind.OTHER -> "Other transactions"
}
