package com.finance.myledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finance.myledger.ui.navigation.MyLedgerNavHost
import com.finance.myledger.ui.theme.MyLedgerTheme
import com.finance.myledger.ui.update.InAppUpdateController
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hosts the Compose NavHost and the Play in-app update check — no business logic.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var updateController: InAppUpdateController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateController = InAppUpdateController(this)
        enableEdgeToEdge()
        setContent {
            val updateState by updateController.state.collectAsStateWithLifecycle()
            MyLedgerTheme {
                MyLedgerNavHost(
                    updateState = updateState,
                    onInstallUpdate = updateController::completeUpdate
                )
            }
        }
    }
}
