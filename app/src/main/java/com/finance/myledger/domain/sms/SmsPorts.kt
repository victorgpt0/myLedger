package com.finance.myledger.domain.sms

import com.finance.myledger.domain.model.RawSms
import com.finance.myledger.domain.model.Transaction

/**
 * Strategy for turning an SMS into a domain [Transaction].
 * Interface lives in domain (pure Kotlin) so use cases stay Android-free;
 * concrete parsers (M-PESA, banks, …) live in data/parser/.
 */
interface SmsParser {
    val sourceName: String
    fun matches(sender: String, body: String): Boolean
    fun parse(sender: String, body: String, timestamp: Long): Transaction?
}

/**
 * Reads existing inbox messages. Implemented in data via ContentResolver.
 */
interface SmsInboxReader {
    /** Full inbox read — used by the one-time [com.finance.myledger.domain.usecase.BackfillSmsUseCase]. */
    suspend fun readInbox(): List<RawSms>

    /**
     * Incremental read: only messages with `date > afterTimestamp`.
     * Used by [com.finance.myledger.domain.usecase.SyncTransactionsUseCase].
     */
    suspend fun readInboxSince(afterTimestamp: Long): List<RawSms>
}

/**
 * Runtime SMS permission check abstracted so ViewModels don't touch Android Context APIs.
 */
interface SmsPermissionChecker {
    fun hasSmsPermissions(): Boolean
}
