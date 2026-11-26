package com.finance.myledger.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * User privacy preferences that persist across launches (e.g. hiding the balance when using the
 * app in public). Implemented with DataStore in the data layer.
 */
interface PrivacySettings {
    val hideBalance: Flow<Boolean>
    suspend fun setHideBalance(hidden: Boolean)
}
