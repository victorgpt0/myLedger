package com.finance.myledger.domain.usecase

import com.finance.myledger.domain.model.Transaction
import com.finance.myledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain use case: observe the full transaction list (Room is the single source of truth).
 * UI layer never queries the DAO directly.
 */
class ObserveTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = repository.observeAll()
}
