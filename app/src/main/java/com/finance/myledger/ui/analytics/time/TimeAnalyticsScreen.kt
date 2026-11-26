package com.finance.myledger.ui.analytics.time

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finance.myledger.ui.analytics.time.components.FeesBreakdownCard
import com.finance.myledger.ui.analytics.time.components.MonthlyTrendChart
import com.finance.myledger.ui.analytics.time.components.PeriodSummaryCard
import com.finance.myledger.ui.analytics.time.components.WalletTransfersCard
import com.finance.myledger.ui.analytics.time.components.WeekdayAverageChart
import com.finance.myledger.ui.common.DateRangeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeAnalyticsScreen(
    viewModel: TimeAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Spending trends") }) }
    ) { padding ->
        when (val state = uiState) {
            TimeAnalyticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is TimeAnalyticsUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateRangeSelector(
                        selected = state.selectedRange,
                        onSelected = viewModel::onRangeSelected
                    )
                    val analytics = state.analytics
                    PeriodSummaryCard(
                        analytics = analytics,
                        range = state.selectedRange,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    MonthlyTrendChart(
                        months = analytics.monthlyTrend,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    WeekdayAverageChart(
                        days = analytics.weekdayAverages,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    FeesBreakdownCard(
                        fees = analytics.feesByKind,
                        expenseTotal = analytics.current.expense,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    WalletTransfersCard(
                        totals = analytics.current,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
