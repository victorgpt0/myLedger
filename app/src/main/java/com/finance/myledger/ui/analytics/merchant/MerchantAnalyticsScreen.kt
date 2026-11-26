package com.finance.myledger.ui.analytics.merchant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.finance.myledger.ui.analytics.merchant.components.RecurringExpensesCard
import com.finance.myledger.ui.analytics.merchant.components.TopMerchantsList
import com.finance.myledger.ui.common.DateRangeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantAnalyticsScreen(
    viewModel: MerchantAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Merchants") }) }
    ) { padding ->
        when (val state = uiState) {
            MerchantAnalyticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MerchantAnalyticsUiState.Success -> {
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
                    TopMerchantsList(
                        merchants = state.topMerchants,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    RecurringExpensesCard(
                        recurring = state.recurring,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
