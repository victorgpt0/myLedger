package com.finance.myledger.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level bottom-nav destinations. Detail routes (e.g. transactions) still push on top.
 */
sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Dashboard : BottomNavDestination(
        route = Routes.DASHBOARD,
        label = "Home",
        icon = Icons.Outlined.Home
    )

    data object TimeAnalytics : BottomNavDestination(
        route = Routes.TIME_ANALYTICS,
        label = "Time",
        icon = Icons.Outlined.Analytics
    )

    data object MerchantAnalytics : BottomNavDestination(
        route = Routes.MERCHANT_ANALYTICS,
        label = "Merchants",
        icon = Icons.Outlined.Storefront
    )

    companion object {
        val items = listOf(Dashboard, TimeAnalytics, MerchantAnalytics)
    }
}

object Routes {
    const val DASHBOARD = "dashboard"
    const val TIME_ANALYTICS = "time_analytics"
    const val MERCHANT_ANALYTICS = "merchant_analytics"
    const val TRANSACTIONS = "transactions"
}
