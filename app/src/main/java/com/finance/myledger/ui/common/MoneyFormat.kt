package com.finance.myledger.ui.common

import java.util.Locale
import kotlin.math.abs

/** Short amounts for chart labels: 950 → "950", 12_500 → "12.5K", 1_200_000 → "1.2M". */
fun formatCompact(amount: Double): String {
    val a = abs(amount)
    val text = when {
        a >= 1_000_000 -> String.format(Locale.US, "%.1fM", a / 1_000_000)
        a >= 10_000 -> String.format(Locale.US, "%.0fK", a / 1_000)
        a >= 1_000 -> String.format(Locale.US, "%.1fK", a / 1_000)
        else -> String.format(Locale.US, "%.0f", a)
    }.replace(".0K", "K").replace(".0M", "M")
    return if (amount < 0) "-$text" else text
}

/** "▲ 12%" / "▼ 8%" — null-safe so callers can pass a missing comparison straight through. */
fun formatChange(percent: Double?): String? {
    if (percent == null) return null
    val arrow = if (percent >= 0) "▲" else "▼"
    return String.format(Locale.US, "%s %.0f%%", arrow, abs(percent))
}
