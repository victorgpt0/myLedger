package com.finance.myledger.data.parser

import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.Direction
import com.finance.myledger.domain.model.TransactionKind
import com.finance.myledger.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for each M-PESA message shape using sample SMS fixtures.
 */
class MpesaParserTest {

    private lateinit var parser: MpesaParser
    private val smsTimestamp = 1_720_000_000_000L

    @Before
    fun setUp() {
        parser = MpesaParser()
    }

    @Test
    fun matches_mpesaSenderIdVariants() {
        assertThat(parser.matches("MPESA", "anything")).isTrue()
        assertThat(parser.matches("M-PESA", "anything")).isTrue()
        assertThat(parser.matches(" mpesa ", "anything")).isTrue()
        assertThat(parser.matches("BANK", "Hello")).isFalse()
    }

    @Test
    fun matches_rejectsLookAlikeFromOtherSenders() {
        // A forged confirmation texted from an ordinary number must not become a transaction.
        val forged = "ZZZ1234567 Confirmed. You have received Ksh50,000.00 from BOSS on 1/7/26 at 2:00 PM. " +
            "New M-PESA balance is Ksh60,000.00."
        assertThat(parser.matches("+254712345678", forged)).isFalse()
        assertThat(parser.matches("Safaricom", "M-PESA balance")).isFalse()
    }

    @Test
    fun parse_sentToPerson_capturesFee_andIsExpense() {
        val body =
            "TDJ45H7X3R Confirmed. Ksh500.00 sent to JOHN DOE 0712345678 on 1/7/26 at 3:15 PM. " +
                "New M-PESA balance is Ksh1,200.00. Transaction cost, Ksh15.00."

        val tx = parser.parse("MPESA", body, smsTimestamp)!!

        assertThat(tx.id).isEqualTo("TDJ45H7X3R")
        assertThat(tx.amount).isEqualTo(500.0)
        assertThat(tx.direction).isEqualTo(Direction.OUT)
        assertThat(tx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(tx.feeAmount).isEqualTo(15.0)
        assertThat(tx.category).isEqualTo(Category.PERSON_TO_PERSON)
    }

    @Test
    fun parse_receivedFromPerson_noFee_isIncome() {
        val body =
            "ABC12DE34F Confirmed. You have received Ksh1,000.00 from JANE DOE 0722334455 on 1/7/26 at 2:00 PM. " +
                "New M-PESA balance is Ksh2,200.00."

        val tx = parser.parse("MPESA", body, smsTimestamp)!!

        assertThat(tx.type).isEqualTo(TransactionType.INCOME)
        assertThat(tx.feeAmount).isNull()
        assertThat(tx.direction).isEqualTo(Direction.IN)
    }

    @Test
    fun parse_paybillTill_isExpense() {
        val body =
            "XYZ98AB76C Confirmed. Ksh450.00 paid to KPLC PREPAID. on 1/7/26 at 10:00 AM. " +
                "New M-PESA balance is Ksh750.00. Transaction cost, Ksh7.00."

        val tx = parser.parse("MPESA", body, smsTimestamp)!!

        assertThat(tx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(tx.feeAmount).isEqualTo(7.0)
        assertThat(tx.category).isEqualTo(Category.UTILITIES)
    }

    @Test
    fun parse_withdrawal_isExpense() {
        val body =
            "QWE11RTY22 Confirmed. Ksh2,000.00 withdrawn from Agent AMOS SHOP on 1/7/26 at 4:30 PM. " +
                "New M-PESA balance is Ksh500.00. Transaction cost, Ksh28.00."

        val tx = parser.parse("MPESA", body, smsTimestamp)!!

        assertThat(tx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(tx.feeAmount).isEqualTo(28.0)
    }

    @Test
    fun parse_airtime_isExpense() {
        val body =
            "AIR55TIME66 Confirmed. You bought Ksh100.00 of airtime on 1/7/26 at 9:00 AM. " +
                "New M-PESA balance is Ksh400.00."

        val tx = parser.parse("MPESA", body, smsTimestamp)!!

        assertThat(tx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(tx.category).isEqualTo(Category.AIRTIME)
    }

    @Test
    fun parse_sentToMali_isTransfer_notExpense() {
        // Self-transfer fix: wallet top-ups must not inflate expense totals.
        val body =
            "MALI123456 Confirmed. Ksh1,000.00 sent to MALI on 1/7/26 at 3:15 PM. " +
                "New M-PESA balance is Ksh500.00. Transaction cost, Ksh0.00."

        val tx = parser.parse("MPESA", body, smsTimestamp)!!

        assertThat(tx.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(tx.direction).isEqualTo(Direction.OUT)
        assertThat(SelfTransferDetector.isSelfTransfer(tx.counterparty)).isTrue()
    }

    @Test
    fun parse_receivedFromZiidi_isTransfer_notIncome() {
        val body =
            "ZIIDI98765 Confirmed. You have received Ksh500.00 from ZIIDI on 1/7/26 at 2:00 PM. " +
                "New M-PESA balance is Ksh1,000.00."

        val tx = parser.parse("MPESA", body, smsTimestamp)!!

        assertThat(tx.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(tx.direction).isEqualTo(Direction.IN)
    }

    @Test
    fun parse_sentToMshwari_isTransfer() {
        val body =
            "MSHWAR1234 Confirmed. Ksh2,000.00 sent to M-SHWARI on 1/7/26 at 1:00 PM. " +
                "New M-PESA balance is Ksh100.00. Transaction cost, Ksh0.00."

        val tx = parser.parse("MPESA", body, smsTimestamp)!!

        assertThat(tx.type).isEqualTo(TransactionType.TRANSFER)
    }

    @Test
    fun parse_ordinaryPaybill_isNotTransfer() {
        val body =
            "XYZ98AB76C Confirmed. Ksh450.00 paid to KPLC PREPAID. on 1/7/26 at 10:00 AM. " +
                "New M-PESA balance is Ksh750.00."

        val tx = parser.parse("MPESA", body, smsTimestamp)!!

        assertThat(tx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(SelfTransferDetector.isSelfTransfer(tx.counterparty)).isFalse()
    }

    @Test
    fun parse_unmatched_returnsNull() {
        val body = "Your OTP is 123456. Do not share."
        assertThat(parser.parse("MPESA", body, smsTimestamp)).isNull()
    }

    @Test
    fun parse_sameMinuteMessages_allUseBodyTimestamp() {
        // Regression: "paid to MERCHANT. on <date>" used to drop the body date and fall back to
        // the SMS delivery time, making it sort after same-minute siblings (wrong latest balance).
        val paid = "UKQA13VVJN Confirmed. Ksh535.00 paid to YAJJA DELIVERY ENTERPRISE. on 26/11/26 at 12:27 PM." +
            "New M-PESA balance is Ksh2,487.20. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,395.00."
        val received = "UKQFE3YGCJ Confirmed.You have received Ksh360.00 from Evans  Maingi 0759***207 on 26/11/26 at 12:27 PM  " +
            "New M-PESA balance is Ksh2,847.20. Invest & earn daily interest with ZIIDI on https://saf.cx/cF6ir"
        val sent = "UKQA13VPWQ Confirmed. Ksh10.00 sent to ZIIDI on 26/11/26 at 12:27 PM New M-PESA balance is Ksh2,837.20. " +
            "Transaction cost, Ksh0.00.Amount you can transact within the day is 499,385.00."

        val txs = listOf(paid, received, sent).map { parser.parse("MPESA", it, smsTimestamp)!! }

        assertThat(txs.map { it.timestamp }.toSet()).hasSize(1)
        assertThat(txs[0].timestamp).isNotEqualTo(smsTimestamp)
        assertThat(txs[0].counterparty).isEqualTo("YAJJA DELIVERY ENTERPRISE")
        assertThat(txs.map { it.balanceAfter }).containsExactly(2487.2, 2847.2, 2837.2).inOrder()
    }

    @Test
    fun parse_setsKindPerMessageShape() {
        fun kindOf(body: String) = parser.parse("MPESA", body, smsTimestamp)!!.kind
        assertThat(kindOf("TDJ45H7X3R Confirmed. Ksh500.00 sent to JOHN DOE 0712345678 on 1/7/26 at 3:15 PM. New M-PESA balance is Ksh1,200.00."))
            .isEqualTo(TransactionKind.SEND_MONEY)
        assertThat(kindOf("TDJ45H7X3S Confirmed. Ksh450.00 sent to KPLC PREPAID for account 1234 on 1/7/26 at 10:00 AM. New M-PESA balance is Ksh750.00. Transaction cost, Ksh5.00."))
            .isEqualTo(TransactionKind.PAYBILL)
        assertThat(kindOf("XYZ98AB76C Confirmed. Ksh450.00 paid to KPLC PREPAID. on 1/7/26 at 10:00 AM. New M-PESA balance is Ksh750.00."))
            .isEqualTo(TransactionKind.BUY_GOODS)
        assertThat(kindOf("ABC12DE34F Confirmed. You have received Ksh1,000.00 from JANE DOE 0722334455 on 1/7/26 at 2:00 PM. New M-PESA balance is Ksh2,200.00."))
            .isEqualTo(TransactionKind.RECEIVED)
        assertThat(kindOf("QWE11RTY22 Confirmed. Ksh2,000.00 withdrawn from Agent AMOS SHOP on 1/7/26 at 4:30 PM. New M-PESA balance is Ksh500.00. Transaction cost, Ksh28.00."))
            .isEqualTo(TransactionKind.WITHDRAWAL)
        assertThat(kindOf("AIR55TIME6 Confirmed. You bought Ksh100.00 of airtime on 1/7/26 at 9:00 AM. New M-PESA balance is Ksh400.00."))
            .isEqualTo(TransactionKind.AIRTIME)
    }

    // --- Real samples (inflows, bank transfers, reversals) ---------------------------------------

    @Test
    fun parse_cashDeposit_isIncomeWithAgentAndBodyTime() {
        val body = "UJPA1BPMQV Confirmed. On 25/10/26 at 6:27 PM Give Ksh1,500.00 cash to Minutes Comm Destiny shop " +
            "along chwele kimi New M-PESA balance is Ksh1,616.20. You can now access M-PESA via *334#"
        val tx = parser.parse("MPESA", body, smsTimestamp)!!
        assertThat(tx.id).isEqualTo("UJPA1BPMQV")
        assertThat(tx.amount).isEqualTo(1_500.0)
        assertThat(tx.direction).isEqualTo(Direction.IN)
        assertThat(tx.type).isEqualTo(TransactionType.INCOME)
        assertThat(tx.kind).isEqualTo(TransactionKind.DEPOSIT)
        assertThat(tx.counterparty).isEqualTo("Minutes Comm Destiny shop along chwele kimi")
        assertThat(tx.balanceAfter).isEqualTo(1_616.2)
        assertThat(tx.timestamp).isNotEqualTo(smsTimestamp)
    }

    @Test
    fun parse_cashDeposit_companyAgent() {
        val body = "UHPA1497J8 Confirmed. On 25/8/26 at 3:59 PM Give Ksh2,000.00 cash to MOSRACH LTD EBENEZER SHOP " +
            "SANKALE RD MADARAKA NRB New M-PESA balance is Ksh2,139.00. You can now access M-PESA via *334#"
        val tx = parser.parse("MPESA", body, smsTimestamp)!!
        assertThat(tx.amount).isEqualTo(2_000.0)
        assertThat(tx.kind).isEqualTo(TransactionKind.DEPOSIT)
        assertThat(tx.balanceAfter).isEqualTo(2_139.0)
    }

    @Test
    fun parse_dividend_usesAccountBalanceLineAndDividendName() {
        val body = "UI2A155WAF Confirmed.  You have received a dividend payment from 2026 Safaricom Final Dividend " +
            "from Stanbic Bank of Ksh38.20 on 2/9/26  at 12:14 PM. Your new Account balance is Ksh1,334.20."
        val tx = parser.parse("MPESA", body, smsTimestamp)!!
        assertThat(tx.id).isEqualTo("UI2A155WAF")
        assertThat(tx.amount).isEqualTo(38.2)
        assertThat(tx.type).isEqualTo(TransactionType.INCOME)
        assertThat(tx.kind).isEqualTo(TransactionKind.DIVIDEND)
        assertThat(tx.counterparty).isEqualTo("2026 Safaricom Final Dividend")
        assertThat(tx.balanceAfter).isEqualTo(1_334.2)
        assertThat(tx.timestamp).isNotEqualTo(smsTimestamp)
    }

    @Test
    fun parse_bankToMpesa_isReceived() {
        val body = "UGSA110IFW Confirmed. You have received Ksh142.00 from STANBIC BANK KENYA LTD on 28/7/26 at 4:20 PM. " +
            "New M-PESA balance is Ksh468.00. Separate personal and business funds through Pochi la Biashara on *334#."
        val tx = parser.parse("MPESA", body, smsTimestamp)!!
        assertThat(tx.amount).isEqualTo(142.0)
        assertThat(tx.kind).isEqualTo(TransactionKind.RECEIVED)
        assertThat(tx.counterparty).isEqualTo("STANBIC BANK KENYA LTD")
        assertThat(tx.balanceAfter).isEqualTo(468.0)
    }

    @Test
    fun parse_mpesaToBank_isPaybill() {
        val body = "UEFA14H4EY Confirmed. Ksh40.00 sent to Stanbic Bank for account 106168 on 15/5/26 at 11:52 AM " +
            "New M-PESA balance is Ksh2,102.00. Transaction cost, Ksh0.00.Amount you can transact within the day is " +
            "499,860.00. Save frequent paybills for quick payment on M-PESA app https://bit.ly/mpesalnk"
        val tx = parser.parse("MPESA", body, smsTimestamp)!!
        assertThat(tx.amount).isEqualTo(40.0)
        assertThat(tx.kind).isEqualTo(TransactionKind.PAYBILL)
        assertThat(tx.balanceAfter).isEqualTo(2_102.0)
        assertThat(tx.feeAmount).isEqualTo(0.0)
    }

    @Test
    fun parse_reversalDebited_linksOriginalAndIsOutbound() {
        val body = "UAL2IH85NG  confirmed. Reversal of transaction UAL2I4I7W9 has been successfully reversed  on 21/1/26  " +
            "at 5:02 PM and Ksh50.00 is debited from your M-PESA account. New M-PESA account balance is Ksh1,052.16."
        val tx = parser.parse("MPESA", body, smsTimestamp)!!
        assertThat(tx.id).isEqualTo("UAL2IH85NG")
        assertThat(tx.kind).isEqualTo(TransactionKind.REVERSAL)
        assertThat(tx.reversesId).isEqualTo("UAL2I4I7W9")
        assertThat(tx.amount).isEqualTo(50.0)
        assertThat(tx.direction).isEqualTo(Direction.OUT)
        assertThat(tx.balanceAfter).isEqualTo(1_052.16)
    }

    @Test
    fun parse_reversalCredited_isInbound() {
        val body = "UCJA1JM9X5 confirmed. Reversal of transaction UCIA19UNYW has been successfully reversed  on 19/3/26  " +
            "at 8:05 AM and Ksh544.00 is credited to your M-PESA account. New M-PESA account balance is Ksh3,057.75."
        val tx = parser.parse("MPESA", body, smsTimestamp)!!
        assertThat(tx.reversesId).isEqualTo("UCIA19UNYW")
        assertThat(tx.amount).isEqualTo(544.0)
        assertThat(tx.direction).isEqualTo(Direction.IN)
        assertThat(tx.balanceAfter).isEqualTo(3_057.75)
    }
}
