package com.finance.myledger.ui.permission

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * SMS permission gate shown when runtime permissions are missing.
 *
 * Both READ_SMS and RECEIVE_SMS are requested:
 * - RECEIVE_SMS powers the live BroadcastReceiver for new messages
 * - READ_SMS powers the inbox backfill via ContentResolver
 */
@Composable
fun SmsPermissionScreen(
    onPermissionResult: (granted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        onPermissionResult(granted)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SMS access needed",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "myLedger reads M-PESA and bank SMS to build your spend dashboard. " +
                "Messages never leave this device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant SMS permission")
        }
    }
}
