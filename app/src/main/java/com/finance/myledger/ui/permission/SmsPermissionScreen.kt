package com.finance.myledger.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.finance.myledger.ui.common.PRIVACY_POLICY_URL

private val SMS_PERMISSIONS = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)

/**
 * Prominent disclosure shown before the system SMS permission prompt (Google Play User Data
 * policy): states what is read, why, where it's stored, and how to revoke — and only asks after
 * the user taps "Allow". Keep this text in sync with the privacy policy and the Play
 * Permissions Declaration (docs/release/).
 *
 * If the user has permanently denied the permission ("Don't ask again"), the system prompt
 * won't appear any more, so the screen offers the app's settings page instead.
 */
@Composable
fun SmsPermissionScreen(
    onPermissionResult: (granted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (!granted) {
            // After a denial, no rationale flag means the system will no longer show the prompt.
            val activity = context.findActivity()
            permanentlyDenied = activity != null && SMS_PERMISSIONS.none {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        }
        onPermissionResult(granted)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Track spending from your M-PESA messages",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "myLedger needs permission to read SMS. Here is exactly how it's used:",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        DisclosurePoint(
            icon = Icons.Outlined.Sms,
            title = "Only M-PESA messages",
            body = "The app reads messages sent by M-PESA, now and as new ones arrive, to record " +
                "your balance, payments, income and fees. Personal messages are skipped and never read into the app."
        )
        DisclosurePoint(
            icon = Icons.Outlined.Lock,
            title = "Stays on this phone",
            body = "Transactions are stored only on this device. Nothing is uploaded, shared or backed up, " +
                "and it isn't used for advertising."
        )
        DisclosurePoint(
            icon = Icons.Outlined.Block,
            title = "You stay in control",
            body = "You can turn off SMS access at any time in your phone's settings. " +
                "Uninstalling the app deletes everything it stored."
        )

        Spacer(modifier = Modifier.height(24.dp))
        if (permanentlyDenied) {
            Text(
                text = "SMS access was turned off. To use myLedger, allow SMS in the app's settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { context.openAppSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = { launcher.launch(SMS_PERMISSIONS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow SMS access")
        }
        TextButton(
            onClick = { context.openUrl(PRIVACY_POLICY_URL) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Read the privacy policy")
        }
    }
}

@Composable
private fun DisclosurePoint(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } // No browser installed: nothing sensible to do.
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
