package com.finance.myledger.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.myledger.domain.usecase.ObserveTransactionsUseCase
import com.finance.myledger.domain.usecase.SyncTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * All-transactions ViewModel. Groups by calendar day for sticky headers.
 * Pull-to-refresh triggers incremental [SyncTransactionsUseCase]; Room Flow refreshes the list.
 */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val observeTransactions: ObserveTransactionsUseCase,
    private val syncTransactions: SyncTransactionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionsUiState>(TransactionsUiState.Loading)
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val isSyncing = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(observeTransactions(), isSyncing) { list, syncing ->
                if (list.isEmpty() && !syncing) {
                    TransactionsUiState.Empty
                } else if (list.isEmpty()) {
                    TransactionsUiState.Success(groups = emptyList(), isSyncing = true)
                } else {
                    TransactionsUiState.Success(groups = groupByDay(list), isSyncing = syncing)
                }
            }.collect { _uiState.value = it }
        }
    }

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

    private fun groupByDay(transactions: List<com.finance.myledger.domain.model.Transaction>): List<TransactionDayGroup> {
        val labelFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        return transactions
            .groupBy { dayStart(it.timestamp) }
            .toSortedMap(compareByDescending { it })
            .map { (dayStart, txs) ->
                TransactionDayGroup(
                    dayLabel = labelFormat.format(Date(dayStart)),
                    dayStartMillis = dayStart,
                    transactions = txs
                )
            }
    }

    private fun dayStart(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
