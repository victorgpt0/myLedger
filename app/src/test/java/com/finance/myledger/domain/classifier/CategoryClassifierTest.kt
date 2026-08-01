package com.finance.myledger.domain.classifier

import com.finance.myledger.domain.model.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Table-driven unit tests for [CategoryClassifier] keyword heuristics.
 */
@RunWith(Parameterized::class)
class CategoryClassifierTest(
    private val counterparty: String,
    private val body: String,
    private val expected: Category
) {

    @Test
    fun classify_mapsKeywords() {
        assertThat(CategoryClassifier.classify(counterparty, body)).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} / {2}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf("Airtime", "You bought Ksh100 of airtime", Category.AIRTIME),
            arrayOf("Safaricom", "data bundle purchase", Category.AIRTIME),
            arrayOf("KPLC PREPAID", "Ksh450 paid to KPLC PREPAID", Category.UTILITIES),
            arrayOf("DSTV", "paybill payment", Category.UTILITIES),
            arrayOf("UBER", "trip payment", Category.TRANSPORT),
            arrayOf("NAIVAS WESTLANDS", "shopping", Category.SHOPPING),
            arrayOf("JOHN DOE 0712345678", "Ksh500 sent to JOHN DOE", Category.PERSON_TO_PERSON),
            arrayOf("Agent AMOS", "Ksh2000 withdrawn from Agent", Category.PERSON_TO_PERSON),
            arrayOf("UNKNOWN MERCHANT", "Confirmed. Ksh10 paid.", Category.OTHER)
        )
    }
}
