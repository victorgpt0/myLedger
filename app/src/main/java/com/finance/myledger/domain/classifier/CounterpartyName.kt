package com.finance.myledger.domain.classifier

/**
 * Normalises counterparty strings from SMS so the same payee/payer groups together.
 *
 * M-PESA writes the same party in different ways: "Evans  Maingi 0759***207" vs
 * "EVANS MAINGI 0712345678", "Stanbic Bank for account 106168" vs "Stanbic Bank for account 2",
 * "2026 Safaricom Final Dividend". Stored values are left untouched (rows are deduped on insert, so
 * re-parsing can't rewrite them); normalisation happens when grouping.
 */
object CounterpartyName {

    // Masked ("0759***207") or full Kenyan numbers ("0712345678", "254712345678", "+254 712 345678").
    private val PHONE = Regex("""(?:\+?254|0)?\s?\d{2,4}[\d*\s]*\*{2,}[\d*]*|(?:\+?254|0)\s?[17]\d{2}\s?\d{3}\s?\d{3}""")
    private val ACCOUNT_SUFFIX = Regex("""\s+for account\b.*$""", RegexOption.IGNORE_CASE)
    private val LEADING_YEAR = Regex("""^(?:19|20)\d{2}\s+""")
    private val SPACES = Regex("""\s+""")

    /** Human-readable name: cleaned, original casing kept. */
    fun display(raw: String): String =
        raw.replace(ACCOUNT_SUFFIX, "")
            .replace(PHONE, " ")
            .replace(LEADING_YEAR, "")
            .replace(SPACES, " ")
            .trim()
            .trimEnd('.', ',', '-')
            .ifEmpty { raw.trim() }

    /** Grouping key: [display] uppercased. */
    fun key(raw: String): String = display(raw).uppercase()

    /**
     * Label for a group of raw names sharing one [key]: the most common written form, preferring
     * mixed case on a tie ("Evans Maingi" reads better than "EVANS MAINGI").
     */
    fun bestDisplay(rawNames: List<String>): String =
        rawNames.map(::display)
            .groupingBy { it }.eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key == it.key.uppercase() })
            .first().key
}
