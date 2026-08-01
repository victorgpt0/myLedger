package com.finance.myledger.data.parser

/**
 * Strategy interface for SMS financial parsers.
 *
 * The canonical interface lives in domain ([com.finance.myledger.domain.sms.SmsParser]) so
 * use cases stay free of data-layer types. This file documents the data-layer home for
 * concrete parsers (M-PESA, bank alerts, …) and re-exports the type for local imports.
 */
typealias SmsParser = com.finance.myledger.domain.sms.SmsParser
