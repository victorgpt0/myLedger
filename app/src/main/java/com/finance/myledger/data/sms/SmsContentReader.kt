package com.finance.myledger.data.sms

import android.content.ContentResolver
import android.provider.Telephony
import com.finance.myledger.data.parser.ParserRegistry
import com.finance.myledger.domain.model.RawSms
import com.finance.myledger.domain.sms.SmsInboxReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ContentResolver access to content://sms/inbox.
 * Data-layer only — domain use cases depend on [SmsInboxReader], not Android APIs.
 *
 * Only messages from known financial senders ([ParserRegistry.senderIds]) are queried, so
 * personal SMS are never read into the app (Play SMS policy: data minimisation). LIKE without
 * wildcards is an exact, ASCII case-insensitive match. Rows are re-checked in Kotlin in case
 * the provider stores an address variant the SQL didn't catch.
 */
@Singleton
class SmsContentReader @Inject constructor(
    private val contentResolver: ContentResolver,
    private val parserRegistry: ParserRegistry
) : SmsInboxReader {

    private val senderClause: String =
        parserRegistry.senderIds.joinToString(" OR ", prefix = "(", postfix = ")") { "${Telephony.Sms.ADDRESS} LIKE ?" }

    override suspend fun readInbox(): List<RawSms> =
        queryInbox(selection = senderClause, args = parserRegistry.senderIds.toTypedArray())

    override suspend fun readInboxSince(afterTimestamp: Long): List<RawSms> =
        queryInbox(
            selection = "$senderClause AND ${Telephony.Sms.DATE} > ?",
            args = (parserRegistry.senderIds + afterTimestamp.toString()).toTypedArray()
        )

    private suspend fun queryInbox(selection: String, args: Array<String>): List<RawSms> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<RawSms>()
            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                args,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIdx).orEmpty()
                    val body = cursor.getString(bodyIdx).orEmpty()
                    val timestamp = cursor.getLong(dateIdx)
                    if (body.isNotBlank() && parserRegistry.isKnownSender(sender)) {
                        results += RawSms(sender = sender, body = body, timestamp = timestamp)
                    }
                }
            }
            results
        }
}
