package com.finance.myledger.domain.classifier

import com.finance.myledger.domain.model.Category

/**
 * Heuristic keyword → [Category] mapper — NOT exhaustive.
 *
 * Easy to extend: add keywords to the lists below. False positives/negatives are expected;
 * a proper ML/user-editable taxonomy can replace this later without changing callers.
 */
object CategoryClassifier {

    fun classify(counterparty: String, body: String): Category {
        val haystack = "$counterparty $body".lowercase()
        return when {
            AIRTIME.any { haystack.contains(it) } -> Category.AIRTIME
            UTILITIES.any { haystack.contains(it) } -> Category.UTILITIES
            TRANSPORT.any { haystack.contains(it) } -> Category.TRANSPORT
            SHOPPING.any { haystack.contains(it) } -> Category.SHOPPING
            // Person-to-person: phone-like digits or generic "sent to"/"received from" without a business keyword
            PERSON_TO_PERSON.any { haystack.contains(it) } -> Category.PERSON_TO_PERSON
            else -> Category.OTHER
        }
    }

    private val AIRTIME = listOf(
        "airtime", "safaricom", "buy goods airtime", "data bundle", "bundles"
    )
    private val UTILITIES = listOf(
        "kplc", "kenya power", "nairobi water", "nwscl", "dstv", "gotv",
        "zuku", "startimes", "nhif", "kra", "paybill"
    )
    private val TRANSPORT = listOf(
        "uber", "bolt", "little", "matatu", "sgr", "kenya airways", "jambojet"
    )
    private val SHOPPING = listOf(
        "naivas", "carrefour", "quickmart", "tuskys", "shoprite", "java",
        "kfc", "chicken inn", "dominos", "supermarket"
    )
    private val PERSON_TO_PERSON = listOf(
        "sent to", "received from", "withdrawn from"
    )
}
