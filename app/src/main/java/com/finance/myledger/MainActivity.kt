package com.finance.myledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finance.myledger.ui.navigation.MyLedgerNavHost
import com.finance.myledger.ui.theme.MyLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hosts the Compose NavHost only — no business logic.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyLedgerTheme {
                MyLedgerNavHost()
            }
        }
    }
}
