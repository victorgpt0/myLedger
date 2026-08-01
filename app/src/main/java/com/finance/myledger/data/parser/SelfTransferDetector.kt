package com.finance.myledger.data.parser

/**
 * Detects self-transfers into linked Safaricom / bank wallet products.
 *
 * Matched case-insensitively against the parsed counterparty. Keep the keyword list in
 * this one place so extending later (new wallet brands) is a one-line change.
 *
 * FULIZA is included because Fuliza repayments / advances are credit-line movements that
 * inflate "spend" if treated as EXPENSE — exclude from expense totals the same way as
 * wallet top-ups. Revisit if product semantics change.
 */
object SelfTransferDetector {

    private val KNOWN_WALLETS = listOf(
        "MALI",
        "ZIIDI",
        "MSHWARI",
        "M-SHWARI",
        "M SHWARI",
        "KCB M-PESA",
        "KCB MPESA",
        "HUSTLER FUND",
        "FULIZA"
    )

    fun isSelfTransfer(counterparty: String): Boolean {
        val haystack = counterparty.uppercase()
        return KNOWN_WALLETS.any { keyword -> haystack.contains(keyword) }
    }
}
