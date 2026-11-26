package com.finance.myledger.data.mapper

import com.finance.myledger.data.local.TransactionEntity
import com.finance.myledger.domain.model.Transaction

/**
 * Entity ↔ domain mappers.
 *
 * Why the split exists: Room entities carry persistence concerns (column types, indices,
 * conflict strategies). Domain models stay free of those so UI/tests/future remote APIs
 * can share one type without depending on Room.
 */
fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    source = source,
    amount = amount,
    direction = direction,
    type = type,
    kind = kind,
    counterparty = counterparty,
    category = category,
    balanceAfter = balanceAfter,
    feeAmount = feeAmount,
    timestamp = timestamp,
    rawMessage = rawMessage
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    source = source,
    amount = amount,
    direction = direction,
    type = type,
    kind = kind,
    counterparty = counterparty,
    category = category,
    balanceAfter = balanceAfter,
    feeAmount = feeAmount,
    timestamp = timestamp,
    rawMessage = rawMessage
)
