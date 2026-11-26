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

    /**
     * Sender IDs this parser accepts, as they appear in the inbox (e.g. "MPESA"). Used both to
     * restrict the inbox query (Play's SMS policy: don't read non-financial messages) and to
     * reject look-alike messages from ordinary phone numbers. Compared via [normalizeSender].
     */
    val senderIds: Set<String>

    fun matches(sender: String, body: String): Boolean
    fun parse(sender: String, body: String, timestamp: Long): Transaction?
}

/** "M-PESA", "mpesa", " MPESA " → "MPESA": uppercase, letters and digits only. */
fun normalizeSender(sender: String): String = sender.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

/**
 * Reads existing inbox messages. Implemented in data via ContentResolver.
 */
interface SmsInboxReader {
    // Implementations must only return messages from senders some registered parser accepts.

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
