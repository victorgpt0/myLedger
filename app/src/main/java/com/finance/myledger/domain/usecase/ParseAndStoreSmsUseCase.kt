package com.finance.myledger.domain.usecase

import com.finance.myledger.data.parser.ParserRegistry
import com.finance.myledger.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Parses a single SMS via [ParserRegistry] (first matching parser) and stores it.
 * Unmatched messages are silently ignored — no "unknown transaction" rows.
 *
 * Used by both the live [com.finance.myledger.data.sms.SmsReceiver] and backfill path.
 *
 * Note: depends on [ParserRegistry] from the data package (single-module app). The registry
 * itself only holds pure-Kotlin [com.finance.myledger.domain.sms.SmsParser] implementations.
 */
class ParseAndStoreSmsUseCase @Inject constructor(
    private val parserRegistry: ParserRegistry,
    private val repository: TransactionRepository
) {
    /**
     * @return true if a parser matched and the transaction was handed to the repository
     *         (insert may still no-op on dedupe conflict).
     */
    suspend operator fun invoke(sender: String, body: String, timestamp: Long): Boolean {
        val transaction = parserRegistry.parse(sender, body, timestamp) ?: return false
        repository.insert(transaction)
        return true
    }
}
