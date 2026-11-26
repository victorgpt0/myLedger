package com.finance.myledger.ui.analytics.merchant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.usecase.ObserveMerchantAnalyticsUseCase
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
class MerchantAnalyticsViewModel @Inject constructor(
    private val observeMerchantAnalytics: ObserveMerchantAnalyticsUseCase
) : ViewModel() {

    private val selectedRange = MutableStateFlow<DateRange>(DateRange.Last6Months)

    private val _uiState = MutableStateFlow<MerchantAnalyticsUiState>(MerchantAnalyticsUiState.Loading)
    val uiState: StateFlow<MerchantAnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedRange
                .flatMapLatest { range ->
                    observeMerchantAnalytics(range).map { analytics ->
                        MerchantAnalyticsUiState.Success(
                            selectedRange = range,
                            topMerchants = analytics.topMerchants,
                            recurring = analytics.recurring
                        )
                    }
                }
                .collect { _uiState.value = it }
        }
    }

    fun onRangeSelected(range: DateRange) {
        selectedRange.value = range
    }
}
