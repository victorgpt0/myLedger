package com.finance.myledger.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.finance.myledger.data.parser.ParserRegistry
import com.finance.myledger.domain.usecase.ParseAndStoreSmsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin BroadcastReceiver adapter for SMS_RECEIVED.
 * Extracts sender/body and hands off to [ParseAndStoreSmsUseCase] via goAsync() + a
 * coroutine — receivers must not do long-running work on the main thread.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var parseAndStoreSms: ParseAndStoreSmsUseCase

    @Inject
    lateinit var parserRegistry: ParserRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: emptyArray()

        scope.launch {
            try {
                // Group PDU parts by originating address into a single body.
                messages
                    .groupBy { it.displayOriginatingAddress.orEmpty() }
                    .forEach { (sender, parts) ->
                        val body = parts.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
                        val timestamp = parts.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
                        // Personal messages are dropped here, before any processing.
                        if (body.isNotBlank() && parserRegistry.isKnownSender(sender)) {
                            parseAndStoreSms(sender, body, timestamp)
                        }
                    }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
