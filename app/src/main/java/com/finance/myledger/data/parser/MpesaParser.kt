package com.finance.myledger.data.parser

import com.finance.myledger.domain.classifier.CategoryClassifier
import com.finance.myledger.domain.model.Direction
import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.model.TransactionKind
import com.finance.myledger.domain.model.TransactionType
import com.finance.myledger.domain.sms.SmsParser
import com.finance.myledger.domain.sms.normalizeSender
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Safaricom M-PESA SMS parser. One regex per message shape; samples documented above each pattern.
 * Extensible: additional shapes can be appended to [patterns] without touching the registry.
 *
 * Fee capture: sends, withdrawals, and paybill/till messages often include
 * "Transaction cost, Ksh X.XX" — extracted separately when present. Receive messages typically
 * do not report a cost, so [Transaction.feeAmount] stays null for those.
 *
 * Self-transfer fix: after a normal parse, [SelfTransferDetector] reclassifies known wallet
 * counterparties as [TransactionType.TRANSFER] so they are excluded from income/expense totals.
 */
class MpesaParser @Inject constructor() : SmsParser {

    override val sourceName: String = "MPESA"

    override val senderIds: Set<String> = setOf("MPESA", "M-PESA")

    private val normalizedSenders = senderIds.map(::normalizeSender).toSet()

    /**
     * Sender ID only. Safaricom sends from the alphanumeric ID "MPESA", which ordinary phones
     * can't use — so a look-alike "Confirmed. Ksh…" text from a phone number is rejected even
     * if its body mentions M-PESA.
     */
    override fun matches(sender: String, body: String): Boolean =
        normalizeSender(sender) in normalizedSenders

    override fun parse(sender: String, body: String, timestamp: Long): Transaction? {
        for ((pattern, extractor) in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                return extractor(matcher, body, timestamp)
            }
        }
        return null
    }

    private data class Extracted(
        val code: String,
        val amount: Double,
        val direction: Direction,
        val kind: TransactionKind,
        val counterparty: String,
        val balanceAfter: Double?,
        val bodyTimestamp: Long?
    )

    private val patterns: List<Pair<Pattern, (java.util.regex.Matcher, String, Long) -> Transaction?>> =
        listOf(
            // Sent to person (fee usually present):
            // "TDJ45H7X3R Confirmed. Ksh500.00 sent to JOHN DOE 0712345678 on 1/7/26 at 3:15 PM. New M-PESA balance is Ksh1,200.00. Transaction cost, Ksh0.00."
            Pattern.compile(
                """(?i)([A-Z0-9]{10})\s+Confirmed\.\s*Ksh([\d,]+\.?\d*)\s+sent to\s+(.+?)\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4}).*?New M-PESA balance is Ksh([\d,]+\.?\d*)""",
                Pattern.DOTALL
            ) to { m, body, smsTs ->
                toTransaction(
                    Extracted(
                        code = m.group(1),
                        amount = parseAmount(m.group(2)),
                        direction = Direction.OUT,
                        kind = if (FOR_ACCOUNT.matcher(body).find()) TransactionKind.PAYBILL else TransactionKind.SEND_MONEY,
                        counterparty = m.group(3).trim(),
                        balanceAfter = parseAmount(m.group(5)),
                        bodyTimestamp = parseMpesaDate(m.group(4), body, smsTs)
                    ),
                    body,
                    smsTs
                )
            },

            // Received from person (fee typically absent):
            // "ABC12DE34F Confirmed. You have received Ksh1,000.00 from JANE DOE 0722334455 on 1/7/26 at 2:00 PM. New M-PESA balance is Ksh2,200.00."
            Pattern.compile(
                """(?i)([A-Z0-9]{10})\s+Confirmed\.\s*You have received Ksh([\d,]+\.?\d*)\s+from\s+(.+?)\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4}).*?New M-PESA balance is Ksh([\d,]+\.?\d*)""",
                Pattern.DOTALL
            ) to { m, body, smsTs ->
                toTransaction(
                    Extracted(
                        code = m.group(1),
                        amount = parseAmount(m.group(2)),
                        direction = Direction.IN,
                        kind = TransactionKind.RECEIVED,
                        counterparty = m.group(3).trim(),
                        balanceAfter = parseAmount(m.group(5)),
                        bodyTimestamp = parseMpesaDate(m.group(4), body, smsTs)
                    ),
                    body,
                    smsTs
                )
            },

            // Paybill / till payment (fee often present):
            // "XYZ98AB76C Confirmed. Ksh450.00 paid to KPLC PREPAID. on 1/7/26 at 10:00 AM. New M-PESA balance is Ksh750.00. Transaction cost, Ksh15.00."
            // The merchant name is usually followed by "." before " on <date>" — the optional `\.?`
            // keeps the body date in that case. Without it the date was dropped and the SMS delivery
            // time (with seconds) was used, making this row sort after same-minute siblings.
            Pattern.compile(
                """(?i)([A-Z0-9]{10})\s+Confirmed\.\s*Ksh([\d,]+\.?\d*)\s+paid to\s+(.+?)(?:\.?\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})|\.).*?New M-PESA balance is Ksh([\d,]+\.?\d*)""",
                Pattern.DOTALL
            ) to { m, body, smsTs ->
                toTransaction(
                    Extracted(
                        code = m.group(1),
                        amount = parseAmount(m.group(2)),
                        direction = Direction.OUT,
                        kind = TransactionKind.BUY_GOODS,
                        counterparty = m.group(3).trim().trimEnd('.'),
                        balanceAfter = parseAmount(m.group(5)),
                        bodyTimestamp = m.group(4)?.let { parseMpesaDate(it, body, smsTs) }
                    ),
                    body,
                    smsTs
                )
            },

            // Withdrawal (fee usually present):
            // "QWE11RTY22 Confirmed. Ksh2,000.00 withdrawn from Agent AMOS SHOP on 1/7/26 at 4:30 PM. New M-PESA balance is Ksh500.00. Transaction cost, Ksh28.00."
            Pattern.compile(
                """(?i)([A-Z0-9]{10})\s+Confirmed\.\s*Ksh([\d,]+\.?\d*)\s+withdrawn from\s+(.+?)\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4}).*?New M-PESA balance is Ksh([\d,]+\.?\d*)""",
                Pattern.DOTALL
            ) to { m, body, smsTs ->
                toTransaction(
                    Extracted(
                        code = m.group(1),
                        amount = parseAmount(m.group(2)),
                        direction = Direction.OUT,
                        kind = TransactionKind.WITHDRAWAL,
                        counterparty = m.group(3).trim(),
                        balanceAfter = parseAmount(m.group(5)),
                        bodyTimestamp = parseMpesaDate(m.group(4), body, smsTs)
                    ),
                    body,
                    smsTs
                )
            },

            // Airtime purchase:
            // "AIR55TIME66 Confirmed. You bought Ksh100.00 of airtime on 1/7/26 at 9:00 AM. New M-PESA balance is Ksh400.00."
            Pattern.compile(
                """(?i)([A-Z0-9]{10})\s+Confirmed\.\s*You bought Ksh([\d,]+\.?\d*)\s+of airtime.*?on\s+(\d{1,2}/\d{1,2}/\d{2,4}).*?New M-PESA balance is Ksh([\d,]+\.?\d*)""",
                Pattern.DOTALL
            ) to { m, body, smsTs ->
                toTransaction(
                    Extracted(
                        code = m.group(1),
                        amount = parseAmount(m.group(2)),
                        direction = Direction.OUT,
                        kind = TransactionKind.AIRTIME,
                        counterparty = "Airtime",
                        balanceAfter = parseAmount(m.group(4)),
                        bodyTimestamp = parseMpesaDate(m.group(3), body, smsTs)
                    ),
                    body,
                    smsTs
                )
            }
        )

    private fun toTransaction(extracted: Extracted, body: String, smsTimestamp: Long): Transaction {
        val counterparty = extracted.counterparty
        // Default type from message direction; overridden below for self-wallet transfers.
        var type = when (extracted.direction) {
            Direction.IN -> TransactionType.INCOME
            Direction.OUT -> TransactionType.EXPENSE
        }
        // Fix double-counting: MALI / ZIIDI / M-SHWARI / etc. must never sum into income/expense.
        if (SelfTransferDetector.isSelfTransfer(counterparty)) {
            type = TransactionType.TRANSFER
        }
        return Transaction(
            id = extracted.code.uppercase(Locale.US),
            source = sourceName,
            amount = extracted.amount,
            direction = extracted.direction,
            type = type,
            kind = extracted.kind,
            counterparty = counterparty,
            category = CategoryClassifier.classify(counterparty, body),
            balanceAfter = extracted.balanceAfter,
            feeAmount = parseFee(body),
            timestamp = extracted.bodyTimestamp ?: smsTimestamp,
            rawMessage = body
        )
    }

    /**
     * Extracts "Transaction cost, Ksh X.XX" when present (sends, withdrawals, paybill/till).
     * Returns null when the fragment is missing (typical for receive messages).
     */
    private fun parseFee(body: String): Double? {
        val matcher = FEE_PATTERN.matcher(body)
        if (!matcher.find()) return null
        return parseAmount(matcher.group(1))
    }

    private fun parseAmount(raw: String): Double =
        raw.replace(",", "").toDoubleOrNull() ?: 0.0

    private fun parseMpesaDate(datePart: String, body: String, smsTimestamp: Long): Long {
        val timeMatch = TIME_PATTERN.matcher(body)
        val timePart = if (timeMatch.find()) timeMatch.group(1) else "12:00 AM"
        val combined = "$datePart $timePart"
        for (format in DATE_FORMATS) {
            try {
                val parsed = format.parse(combined)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // try next format
            }
        }
        return smsTimestamp
    }

    companion object {
        private val FEE_PATTERN: Pattern =
            Pattern.compile("""(?i)Transaction cost,?\s*Ksh\s*([\d,]+\.?\d*)""")

        // Paybill confirmations read "sent to KPLC PREPAID for account 123…"; plain sends don't.
        private val FOR_ACCOUNT: Pattern = Pattern.compile("""(?i)sent to\s+.+?\s+for account""")

        private val TIME_PATTERN: Pattern =
            Pattern.compile("""(?i)at\s+(\d{1,2}:\d{2}\s*[AP]M)""")

        private val DATE_FORMATS = listOf(
            SimpleDateFormat("d/M/yy h:mm a", Locale.US),
            SimpleDateFormat("d/M/yyyy h:mm a", Locale.US),
            SimpleDateFormat("d/M/yy hh:mm a", Locale.US),
            SimpleDateFormat("d/M/yyyy hh:mm a", Locale.US)
        )
    }
}
