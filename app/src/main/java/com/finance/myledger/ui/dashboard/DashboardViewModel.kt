package com.finance.myledger.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.finance.myledger.data.sms.SmsBackfillWorker
import com.finance.myledger.domain.settings.PrivacySettings
import com.finance.myledger.domain.sms.SmsPermissionChecker
import com.finance.myledger.domain.usecase.CheckParserUpgradeUseCase
import com.finance.myledger.domain.usecase.ObserveDashboardSummaryUseCase
import com.finance.myledger.domain.usecase.SyncTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dashboard ViewModel — exposes a single [StateFlow] of [DashboardUiState].
 * Pull-to-refresh calls [SyncTransactionsUseCase]; Room Flows refresh the UI automatically.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeDashboardSummary: ObserveDashboardSummaryUseCase,
    private val syncTransactions: SyncTransactionsUseCase,
    private val permissionChecker: SmsPermissionChecker,
    private val workManager: WorkManager,
    private val privacySettings: PrivacySettings,
    private val checkParserUpgrade: CheckParserUpgradeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val isSyncing = MutableStateFlow(false)
    private var observeJob: Job? = null

    /**
     * Full import is started automatically, once per process, whenever SMS access is granted and
     * the ledger is empty: on first run, and after a database reset (e.g. a destructive
     * migration), where the incremental sync cursor would otherwise skip all older messages.
     * Once-per-process prevents a loop when the inbox genuinely has no M-PESA messages.
     */
    private var autoImportRequested = false

    init {
        refreshPermissionAndObserve()
    }

    fun onPermissionsResult() {
        refreshPermissionAndObserve()
    }

    /** Re-check on resume — the user may have granted or revoked SMS access in system settings. */
    fun onResume() {
        val showingPermissionScreen = _uiState.value == DashboardUiState.PermissionRequired
        if (showingPermissionScreen == permissionChecker.hasSmsPermissions()) {
            refreshPermissionAndObserve()
        }
    }

    fun startBackfill() {
        val request = OneTimeWorkRequestBuilder<SmsBackfillWorker>().build()
        workManager.enqueueUniqueWork(
            SmsBackfillWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun toggleBalanceHidden() {
        val current = (_uiState.value as? DashboardUiState.Success)?.balanceHidden ?: return
        viewModelScope.launch { privacySettings.setHideBalance(!current) }
    }

    /** Incremental sync for pull-to-refresh — O(new messages), not a full inbox rescan. */
    fun onPullToRefresh() {
        viewModelScope.launch {
            isSyncing.value = true
            try {
                syncTransactions()
            } finally {
                isSyncing.value = false
            }
        }
    }

    private fun refreshPermissionAndObserve() {
        if (!permissionChecker.hasSmsPermissions()) {
            observeJob?.cancel()
            _uiState.value = DashboardUiState.PermissionRequired
            return
        }
        observeJob?.cancel()
        viewModelScope.launch {
            // The app learned new message types since the last full import: re-import once.
            if (!autoImportRequested && checkParserUpgrade()) {
                autoImportRequested = true
                startBackfill()
            }
        }
        observeJob = viewModelScope.launch {
            combine(
                observeDashboardSummary(),
                workManager.getWorkInfosForUniqueWorkFlow(SmsBackfillWorker.WORK_NAME),
                isSyncing,
                privacySettings.hideBalance
            ) { summary, workInfos, syncing, balanceHidden ->
                val runningInfo = workInfos.firstOrNull {
                    it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                }
                val progress = runningInfo?.progress?.let { data ->
                    val processed = data.getInt(SmsBackfillWorker.KEY_PROCESSED, 0)
                    val total = data.getInt(SmsBackfillWorker.KEY_TOTAL, 0)
                    if (total > 0) processed.toFloat() / total else null
                }
                val isBackfilling = runningInfo != null
                val isEmpty = summary.recentTransactions.isEmpty() &&
                    summary.latestBalance == null &&
                    summary.monthlySpendTotal == 0.0

                if (isEmpty && !isBackfilling && !autoImportRequested) {
                    autoImportRequested = true
                    startBackfill()
                }

                when {
                    isEmpty && !isBackfilling -> DashboardUiState.Empty
                    else -> DashboardUiState.Success(
                        balance = summary.latestBalance,
                        recentTransactions = summary.recentTransactions,
                        monthlySpendTotal = summary.monthlySpendTotal,
                        categorySpend = summary.categorySpend,
                        feesPaidThisMonth = summary.feesPaidThisMonth,
                        isBackfilling = isBackfilling,
                        backfillProgress = progress,
                        isSyncing = syncing,
                        balanceHidden = balanceHidden
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
