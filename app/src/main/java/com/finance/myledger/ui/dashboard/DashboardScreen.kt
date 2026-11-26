package com.finance.myledger.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finance.myledger.ui.dashboard.components.BalanceCard
import com.finance.myledger.ui.dashboard.components.MonthlySpendChart
import com.finance.myledger.ui.dashboard.components.RecentActivityCard
import com.finance.myledger.ui.dashboard.components.formatKes
import com.finance.myledger.ui.permission.SmsPermissionScreen

/**
 * Dashboard screen — core baseline ("what's happening right now").
 * Historic / merchant analysis lives on the Time and Merchants tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSeeAllTransactions: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    when (val state = uiState) {
        DashboardUiState.PermissionRequired -> {
            SmsPermissionScreen(
                onPermissionResult = { viewModel.onPermissionsResult() }
            )
        }
        DashboardUiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
        DashboardUiState.Empty -> {
            Scaffold(
                topBar = { TopAppBar(title = { Text("myLedger") }) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No M-PESA messages were found on this phone. New ones are added automatically as they arrive.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.startBackfill() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scan again")
                    }
                }
            }
        }
        is DashboardUiState.Success -> {
            Scaffold(
                topBar = { TopAppBar(title = { Text("myLedger") }) }
            ) { padding ->
                PullToRefreshBox(
                    isRefreshing = state.isSyncing,
                    onRefresh = { viewModel.onPullToRefresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BalanceCard(
                            balance = state.balance,
                            hidden = state.balanceHidden,
                            onToggleHidden = viewModel::toggleBalanceHidden
                        )

                        Text(
                            text = "Fees paid this month: ${formatKes(state.feesPaidThisMonth)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        RecentActivityCard(
                            transactions = state.recentTransactions,
                            onSeeAll = onSeeAllTransactions
                        )

                        MonthlySpendChart(
                            monthlyTotal = state.monthlySpendTotal,
                            categorySpend = state.categorySpend
                        )

                        if (state.isBackfilling) {
                            Column {
                                Text(
                                    text = "Importing SMS…",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (state.backfillProgress != null) {
                                    LinearProgressIndicator(
                                        progress = { state.backfillProgress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startBackfill() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Full SMS rescan")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
