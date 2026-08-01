package com.finance.myledger.data.parser

import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.Direction
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
    fun matches_mpesaSender() {
        assertThat(parser.matches("MPESA", "anything")).isTrue()
        assertThat(parser.matches("Safaricom", "M-PESA balance")).isTrue()
        assertThat(parser.matches("BANK", "Hello")).isFalse()
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
}
