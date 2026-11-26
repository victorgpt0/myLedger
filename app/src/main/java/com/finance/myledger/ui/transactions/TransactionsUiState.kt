package com.finance.myledger.ui.transactions

import com.finance.myledger.domain.model.Transaction

/**
 * UiState for the all-transactions screen.
 *
 * Left extensible for a future filter field (e.g. `filterQuery`, `selectedCategory`)
 * without breaking the Compose contract — add optional fields to [Success] when needed.
 */
sealed class TransactionsUiState {
    data object Loading : TransactionsUiState()
    data object Empty : TransactionsUiState()

    data class Success(
        val groups: List<TransactionDayGroup>,
        val isSyncing: Boolean = false
        // Future: val filterQuery: String = "", val selectedCategory: Category? = null
    ) : TransactionsUiState()
}

data class TransactionDayGroup(
    val dayLabel: String,
    val dayStartMillis: Long,
    val transactions: List<Transaction>
)
