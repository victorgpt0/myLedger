package com.finance.myledger.domain.balance

import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.Direction
import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BalanceChainResolverTest {

    @Test
    fun picksChainTail_regardlessOfInputOrder() {
        val paid = tx("PAID", 535.0, Direction.OUT, after = 2487.2, fee = 0.0)
        val received = tx("RECV", 360.0, Direction.IN, after = 2847.2)
        val sent = tx("SENT", 10.0, Direction.OUT, after = 2837.2, fee = 0.0)

        listOf(
            listOf(paid, received, sent),
            listOf(sent, paid, received),
            listOf(received, sent, paid)
        ).forEach { assertThat(BalanceChainResolver.latest(it)!!.id).isEqualTo("SENT") }
    }

    @Test
    fun accountsForFees() {
        val first = tx("A", 100.0, Direction.OUT, after = 885.0, fee = 15.0)  // before 1000
        val second = tx("B", 200.0, Direction.OUT, after = 672.0, fee = 13.0) // before 885
        assertThat(BalanceChainResolver.latest(listOf(second, first))!!.id).isEqualTo("B")
        assertThat(BalanceChainResolver.latest(listOf(first, second))!!.id).isEqualTo("B")
    }

    @Test
    fun brokenChain_fallsBackToFirstCandidate() {
        val a = tx("A", 10.0, Direction.OUT, after = 100.0)
        val b = tx("B", 10.0, Direction.OUT, after = 500.0)
        assertThat(BalanceChainResolver.latest(listOf(b, a))!!.id).isEqualTo("B")
    }

    @Test
    fun emptyOrSingle() {
        assertThat(BalanceChainResolver.latest(emptyList())).isNull()
        val only = tx("A", 10.0, Direction.OUT, after = 100.0)
        assertThat(BalanceChainResolver.latest(listOf(only))).isEqualTo(only)
    }

    @Test
    fun orderNewestFirst_reordersOnlyTiedRuns() {
        val older = tx("OLD", 50.0, Direction.OUT, after = 2537.2, ts = 500L)
        val paid = tx("PAID", 535.0, Direction.OUT, after = 2487.2, fee = 0.0)
        val received = tx("RECV", 360.0, Direction.IN, after = 2847.2)
        val sent = tx("SENT", 10.0, Direction.OUT, after = 2837.2, fee = 0.0)
        val newer = tx("NEW", 37.2, Direction.OUT, after = 2800.0, ts = 2_000L)

        val ordered = BalanceChainResolver.orderNewestFirst(listOf(newer, paid, sent, received, older))

        assertThat(ordered.map { it.id }).containsExactly("NEW", "SENT", "RECV", "PAID", "OLD").inOrder()
    }

    @Test
    fun orderNewestFirst_rowsWithoutBalanceGoLastInRun() {
        val noBalance = tx("NOBAL", 5.0, Direction.OUT, after = null)
        val a = tx("A", 10.0, Direction.OUT, after = 90.0)  // before 100
        val b = tx("B", 10.0, Direction.OUT, after = 80.0)  // before 90

        val ordered = BalanceChainResolver.orderNewestFirst(listOf(noBalance, a, b))

        assertThat(ordered.map { it.id }).containsExactly("B", "A", "NOBAL").inOrder()
    }

    private fun tx(id: String, amount: Double, direction: Direction, after: Double?, fee: Double? = null, ts: Long = 1_000L) =
        Transaction(
            id = id,
            source = "MPESA",
            amount = amount,
            direction = direction,
            type = if (direction == Direction.IN) TransactionType.INCOME else TransactionType.EXPENSE,
            counterparty = "X",
            category = Category.OTHER,
            balanceAfter = after,
            feeAmount = fee,
            timestamp = ts,
            rawMessage = "raw"
        )
}
