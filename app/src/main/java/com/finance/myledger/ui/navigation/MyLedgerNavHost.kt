package com.finance.myledger.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finance.myledger.ui.analytics.merchant.MerchantAnalyticsScreen
import com.finance.myledger.ui.analytics.time.TimeAnalyticsScreen
import com.finance.myledger.ui.dashboard.DashboardScreen
import com.finance.myledger.ui.transactions.TransactionsScreen
import com.finance.myledger.ui.update.UpdateUiState

/**
 * App navigation graph: 3 top-level destinations with bottom nav; transactions pushes on top.
 * The root Scaffold also hosts the "update ready — restart" snackbar for Play in-app updates.
 */
@Composable
fun MyLedgerNavHost(
    updateState: UpdateUiState = UpdateUiState.IDLE,
    onInstallUpdate: () -> Unit = {}
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(updateState) {
        if (updateState == UpdateUiState.READY_TO_INSTALL) {
            val result = snackbarHostState.showSnackbar(
                message = "An update is ready to install",
                actionLabel = "Restart",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) onInstallUpdate()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in BottomNavDestination.items.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavDestination.items.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onSeeAllTransactions = {
                        navController.navigate(Routes.TRANSACTIONS)
                    }
                )
            }
            composable(Routes.TIME_ANALYTICS) {
                TimeAnalyticsScreen()
            }
            composable(Routes.MERCHANT_ANALYTICS) {
                MerchantAnalyticsScreen()
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
