package com.finance.myledger.di

import com.finance.myledger.data.local.SyncPreferences
import com.finance.myledger.data.parser.MpesaParser
import com.finance.myledger.data.repository.TransactionRepositoryImpl
import com.finance.myledger.data.sms.AndroidSmsPermissionChecker
import com.finance.myledger.data.sms.SmsContentReader
import com.finance.myledger.domain.repository.TransactionRepository
import com.finance.myledger.domain.sms.SmsInboxReader
import com.finance.myledger.domain.sms.SmsParser
import com.finance.myledger.domain.sms.SmsPermissionChecker
import com.finance.myledger.domain.sync.SyncCursorStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Binds domain ports to data-layer implementations.
 * Adding a new SMS parser later: create the class and add another @IntoSet @Binds method —
 * [com.finance.myledger.data.parser.ParserRegistry] picks it up automatically.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindSmsInboxReader(impl: SmsContentReader): SmsInboxReader

    @Binds
    @Singleton
    abstract fun bindSmsPermissionChecker(impl: AndroidSmsPermissionChecker): SmsPermissionChecker

    @Binds
    @Singleton
    abstract fun bindSyncCursorStore(impl: SyncPreferences): SyncCursorStore

    @Binds
    @IntoSet
    abstract fun bindMpesaParser(impl: MpesaParser): SmsParser
}
