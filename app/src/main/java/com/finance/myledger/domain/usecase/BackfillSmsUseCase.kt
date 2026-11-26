package com.finance.myledger.domain.usecase

import com.finance.myledger.data.parser.ParserRegistry
import com.finance.myledger.domain.sms.SmsInboxReader
import com.finance.myledger.domain.sync.SyncCursorStore
import javax.inject.Inject

/**
 * One-shot full inbox backfill: read all SMS, parse/store via [ParseAndStoreSmsUseCase].
 * Invoked by [com.finance.myledger.data.sms.SmsBackfillWorker] so work survives process death.
 *
 * After finishing, advances [SyncCursorStore] so the next pull-to-refresh uses the cheap
 * incremental [SyncTransactionsUseCase] path automatically.
 *
 * @param onProgress optional callback (processed, total) for WorkManager setProgress.
 */
class BackfillSmsUseCase @Inject constructor(
    private val inboxReader: SmsInboxReader,
    private val parseAndStoreSms: ParseAndStoreSmsUseCase,
    private val syncCursor: SyncCursorStore,
    private val parserRegistry: ParserRegistry
) {
    suspend operator fun invoke(onProgress: suspend (processed: Int, total: Int) -> Unit = { _, _ -> }) {
        val messages = inboxReader.readInbox()
        val total = messages.size
        messages.forEachIndexed { index, sms ->
            parseAndStoreSms(sms.sender, sms.body, sms.timestamp)
            onProgress(index + 1, total)
        }
        // Recorded even for an empty inbox, so an up-to-date install never re-imports needlessly.
        syncCursor.setImportedParserVersion(parserRegistry.version)
        val newest = messages.maxOfOrNull { it.timestamp } ?: return
        val current = syncCursor.getLastSyncedTimestamp()
        if (newest > current) {
            syncCursor.setLastSyncedTimestamp(newest)
        }
    }
}
