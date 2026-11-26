package com.finance.myledger.data.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SelfTransferDetectorTest {

    @Test
    fun matchesWalletNamesAsWholeWords() {
        assertThat(SelfTransferDetector.isSelfTransfer("MALI")).isTrue()
        assertThat(SelfTransferDetector.isSelfTransfer("ZIIDI")).isTrue()
        assertThat(SelfTransferDetector.isSelfTransfer("M-Shwari Savings")).isTrue()
        assertThat(SelfTransferDetector.isSelfTransfer("KCB M-PESA")).isTrue()
    }

    @Test
    fun ignoresWalletNamesInsideOtherWords() {
        // Previously substring matches: these people/shops vanished from income and spending.
        assertThat(SelfTransferDetector.isSelfTransfer("JOHN KAMALI 0712345678")).isFalse()
        assertThat(SelfTransferDetector.isSelfTransfer("MALINDI SHOP")).isFalse()
        assertThat(SelfTransferDetector.isSelfTransfer("SOMALI RESTAURANT")).isFalse()
    }
}
