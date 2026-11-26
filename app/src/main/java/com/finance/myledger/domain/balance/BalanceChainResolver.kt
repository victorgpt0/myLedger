package com.finance.myledger.domain.balance

import com.finance.myledger.domain.model.Direction
import com.finance.myledger.domain.model.Transaction
import kotlin.math.roundToLong

/**
 * Picks the most recent transaction among ones sharing the same timestamp.
 *
 * M-PESA bodies only carry minute precision, so several transactions in one minute tie on
 * timestamp and SQL ordering alone is arbitrary. Each message reports its post-transaction
 * balance, so the true order can be recovered from the balance chain:
 *
 *   balanceBefore = balanceAfter − signedAmount + fee   (OUT is negative, fee always deducted)
 *
 * The latest transaction is the one whose balanceAfter is not any other candidate's
 * balanceBefore. Amounts are compared in cents to avoid floating-point mismatch.
 *
 * Falls back to the first candidate (caller's order) when the chain is ambiguous or broken,
 * e.g. a missing SMS or a cycle such as send 10 / receive 10.
 */
object BalanceChainResolver {

    fun latest(candidates: List<Transaction>): Transaction? {
        val withBalance = candidates.filter { it.balanceAfter != null }
        if (withBalance.size <= 1) return withBalance.firstOrNull()

        val befores = withBalance.map { it.id to balanceBeforeCents(it) }
        val tails = withBalance.filter { tx ->
            val after = cents(tx.balanceAfter!!)
            befores.none { (id, before) -> id != tx.id && before == after }
        }
        return tails.singleOrNull() ?: withBalance.first()
    }

    /**
     * Reorders a newest-first list so that runs sharing a timestamp follow the balance chain.
     * Input must already be sorted by timestamp DESC; only ties are reordered. Within a run,
     * rows without a balance keep their relative order after the chained ones.
     */
    fun orderNewestFirst(transactions: List<Transaction>): List<Transaction> {
        if (transactions.size <= 1) return transactions
        val result = ArrayList<Transaction>(transactions.size)
        var start = 0
        while (start < transactions.size) {
            var end = start + 1
            while (end < transactions.size && transactions[end].timestamp == transactions[start].timestamp) end++
            val run = transactions.subList(start, end)
            if (run.size == 1) result += run else result += orderRun(run)
            start = end
        }
        return result
    }

    // Repeatedly peel off the chain tail. Tie runs are a handful of rows, so O(n²) is fine.
    private fun orderRun(run: List<Transaction>): List<Transaction> {
        val remaining = run.toMutableList()
        val ordered = ArrayList<Transaction>(run.size)
        while (remaining.isNotEmpty()) {
            val next = latest(remaining) ?: break
            ordered += next
            remaining.remove(next)
        }
        return ordered + remaining
    }

    private fun balanceBeforeCents(tx: Transaction): Long {
        val signed = if (tx.direction == Direction.IN) tx.amount else -tx.amount
        return cents(tx.balanceAfter!! - signed + (tx.feeAmount ?: 0.0))
    }

    private fun cents(value: Double): Long = (value * 100).roundToLong()
}
