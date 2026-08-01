package com.finance.myledger

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry — Hilt root + WorkManager Configuration.Provider so
 * [com.finance.myledger.data.sms.SmsBackfillWorker] can be Hilt-assisted.
 */
@HiltAndroidApp
class MyLedgerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
