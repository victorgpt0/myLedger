package com.finance.myledger.domain.usecase

import com.finance.myledger.data.parser.ParserRegistry
import com.finance.myledger.domain.sync.SyncCursorStore
import javax.inject.Inject

/**
 * True when the parsers recognise more message types than when the inbox was last fully
 * imported. Incremental sync only reads messages newer than its cursor, so messages that older
 * parser versions dropped would otherwise never be imported; a full re-import (deduped by
 * transaction id) picks them up.
 *
 * Like [ParseAndStoreSmsUseCase], depends on the data-layer [ParserRegistry] (single-module app).
 */
class CheckParserUpgradeUseCase @Inject constructor(
    private val syncCursor: SyncCursorStore,
    private val parserRegistry: ParserRegistry
) {
    suspend operator fun invoke(): Boolean =
        syncCursor.getImportedParserVersion() < parserRegistry.version
}
