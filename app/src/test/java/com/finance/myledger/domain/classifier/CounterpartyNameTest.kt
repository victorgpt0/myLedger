package com.finance.myledger.domain.classifier

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CounterpartyNameTest {

    @Test
    fun stripsMaskedAndFullPhoneNumbersAndExtraSpaces() {
        assertThat(CounterpartyName.display("Evans  Maingi 0759***207")).isEqualTo("Evans Maingi")
        assertThat(CounterpartyName.display("JOHN DOE 0712345678")).isEqualTo("JOHN DOE")
        assertThat(CounterpartyName.display("JANE DOE 254722334455")).isEqualTo("JANE DOE")
    }

    @Test
    fun stripsPaybillAccountAndLeadingYear() {
        assertThat(CounterpartyName.display("Stanbic Bank for account 106168")).isEqualTo("Stanbic Bank")
        assertThat(CounterpartyName.display("2026 Safaricom Final Dividend")).isEqualTo("Safaricom Final Dividend")
    }

    @Test
    fun keepsBusinessNamesAndNumbersThatArentPhones() {
        assertThat(CounterpartyName.display("YAJJA DELIVERY ENTERPRISE.")).isEqualTo("YAJJA DELIVERY ENTERPRISE")
        assertThat(CounterpartyName.display("NAIVAS TILL 123456")).isEqualTo("NAIVAS TILL 123456")
    }

    @Test
    fun keyIgnoresCase() {
        assertThat(CounterpartyName.key("Evans  Maingi 0759***207")).isEqualTo(CounterpartyName.key("EVANS MAINGI"))
    }
}
