package com.finance.myledger.ui.dashboard

import com.finance.myledger.domain.model.CategorySpend
import com.finance.myledger.domain.model.Transaction

/**
 * Single UiState sealed hierarchy for the dashboard.
 * Extensible later (e.g. sync status) without breaking the Compose contract —
 * add fields to [Success] or new sealed variants as needed.
 */
sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data object PermissionRequired : DashboardUiState()
    data object Empty : DashboardUiState()

    data class Success(
        val balance: Double?,
        val recentTransactions: List<Transaction>,
        val monthlySpendTotal: Double,
        val categorySpend: List<CategorySpend>,
        val feesPaidThisMonth: Double = 0.0,
        val isBackfilling: Boolean = false,
        val backfillProgress: Float? = null,
        val isSyncing: Boolean = false
    ) : DashboardUiState()
}
