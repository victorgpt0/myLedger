package com.finance.myledger.data.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.finance.myledger.domain.usecase.BackfillSmsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager CoroutineWorker wrapping [BackfillSmsUseCase].
 * Survives process death and reports progress so the UI can show an indicator.
 * Triggered manually (not on app start) via [enqueue].
 */
@HiltWorker
class SmsBackfillWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backfillSms: BackfillSmsUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            backfillSms { processed, total ->
                setProgress(
                    Data.Builder()
                        .putInt(KEY_PROCESSED, processed)
                        .putInt(KEY_TOTAL, total)
                        .build()
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure(
                Data.Builder()
                    .putString(KEY_ERROR, e.message ?: "Backfill failed")
                    .build()
            )
        }
    }

    companion object {
        const val WORK_NAME = "sms_backfill"
        const val KEY_PROCESSED = "processed"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
    }
}
