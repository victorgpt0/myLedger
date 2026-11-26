package com.finance.myledger.ui.dashboard.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finance.myledger.ui.common.PixelatedText
import java.text.NumberFormat
import java.util.Locale

/**
 * Fixed stand-in shown (pixelated) while the balance is hidden. Never derived from the real
 * balance, so the mosaic can't leak its size or digit count.
 */
private const val HIDDEN_PLACEHOLDER = "Ksh 88,888.88"

/**
 * Balance card — latest M-PESA balance from the most recent parsed SMS. The eye button hides it
 * behind a pixelated placeholder (persisted, so it stays hidden across launches).
 */
@Composable
fun BalanceCard(
    balance: Double?,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Total Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleHidden) {
                    Icon(
                        imageVector = if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (hidden) "Show balance" else "Hide balance",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val style = MaterialTheme.typography.displayLarge
            val color = MaterialTheme.colorScheme.onPrimaryContainer
            Crossfade(targetState = hidden, label = "balanceVisibility") { isHidden ->
                if (isHidden) {
                    PixelatedText(
                        text = HIDDEN_PLACEHOLDER,
                        style = style,
                        color = color,
                        modifier = Modifier.semantics { contentDescription = "Balance hidden" }
                    )
                } else {
                    Text(text = formatKes(balance), style = style, color = color)
                }
            }
        }
    }
}

internal fun formatKes(amount: Double?): String {
    if (amount == null) return "—"
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "Ksh ${nf.format(amount)}"
}
