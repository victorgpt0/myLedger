package com.finance.myledger.data.parser

import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.sms.SmsParser
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
    fun parse(sender: String, body: String, timestamp: Long): Transaction? {
        val parser = parsers.firstOrNull { it.matches(sender, body) } ?: return null
        return parser.parse(sender, body, timestamp)
    }
}
