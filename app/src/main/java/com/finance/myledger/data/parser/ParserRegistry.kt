package com.finance.myledger.data.parser

import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.sms.SmsParser
import com.finance.myledger.domain.sms.normalizeSender
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ordered registry of [SmsParser] implementations.
 * Hilt injects the multibound [Set]; first match wins. Adding a bank parser later is just
 * another `@IntoSet` binding — no changes to this class or call sites.
 */
@Singleton
class ParserRegistry @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards SmsParser>
) {
    /** Every sender ID any parser accepts, as written in the inbox — drives the inbox query filter. */
    val senderIds: Set<String> = parsers.flatMap { it.senderIds }.toSet()

    private val normalizedSenders = senderIds.map(::normalizeSender).toSet()

    /** Changes whenever any parser's [SmsParser.version] does. */
    val version: Int = parsers.sumOf { it.version }

    fun isKnownSender(sender: String): Boolean = normalizeSender(sender) in normalizedSenders

    fun parse(sender: String, body: String, timestamp: Long): Transaction? {
        val parser = parsers.firstOrNull { it.matches(sender, body) } ?: return null
        return parser.parse(sender, body, timestamp)
    }
}
