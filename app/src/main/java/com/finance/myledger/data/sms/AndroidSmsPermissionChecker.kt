package com.finance.myledger.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.finance.myledger.domain.sms.SmsPermissionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [SmsPermissionChecker].
 * Keeps Context/PackageManager out of ViewModels and domain use cases.
 */
@Singleton
class AndroidSmsPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : SmsPermissionChecker {

    override fun hasSmsPermissions(): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
        val receive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
        return read == PackageManager.PERMISSION_GRANTED &&
            receive == PackageManager.PERMISSION_GRANTED
    }
}
