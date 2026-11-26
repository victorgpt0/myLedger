package com.finance.myledger.domain.usecase

import com.finance.myledger.domain.sms.SmsInboxReader
import com.finance.myledger.domain.sync.SyncCursorStore
import javax.inject.Inject

/**
 * Cheap incremental sync path — distinct from full backfill.
 *
 * Reads only SMS newer than [SyncCursorStore.getLastSyncedTimestamp], parses via the
 * existing parse/store pipeline, then advances the cursor to the newest message timestamp.
 * Pull-to-refresh should call this; first-install full scan stays on [BackfillSmsUseCase].
 */
class SyncTransactionsUseCase @Inject constructor(
    private val inboxReader: SmsInboxReader,
    private val parseAndStoreSms: ParseAndStoreSmsUseCase,
    private val syncCursor: SyncCursorStore
) {
    /**
     * @return number of inbox messages examined (not necessarily stored — unmatched are ignored).
     */
    suspend operator fun invoke(): Int {
        val lastSynced = syncCursor.getLastSyncedTimestamp()
        val messages = inboxReader.readInboxSince(lastSynced)
        if (messages.isEmpty()) return 0

        messages.forEach { sms ->
            parseAndStoreSms(sms.sender, sms.body, sms.timestamp)
        }

        val newest = messages.maxOf { it.timestamp }
        if (newest > lastSynced) {
            syncCursor.setLastSyncedTimestamp(newest)
        }
        return messages.size
    }
}
