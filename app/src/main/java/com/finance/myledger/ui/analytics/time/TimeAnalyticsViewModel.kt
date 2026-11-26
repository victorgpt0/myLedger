package com.finance.myledger.ui.analytics.time

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.usecase.ObserveTimeAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimeAnalyticsViewModel @Inject constructor(
    private val observeTimeAnalytics: ObserveTimeAnalyticsUseCase
) : ViewModel() {

    private val selectedRange = MutableStateFlow<DateRange>(DateRange.Last6Months)

    private val _uiState = MutableStateFlow<TimeAnalyticsUiState>(TimeAnalyticsUiState.Loading)
    val uiState: StateFlow<TimeAnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedRange
                .flatMapLatest { range ->
                    observeTimeAnalytics(range).map { analytics ->
                        TimeAnalyticsUiState.Success(selectedRange = range, analytics = analytics)
                    }
                }
                .collect { _uiState.value = it }
        }
    }

    fun onRangeSelected(range: DateRange) {
        selectedRange.value = range
    }
}
