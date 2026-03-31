package com.kitabu.app.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kitabu.app.data.KitabuDatabase
import java.util.concurrent.TimeUnit

/**
 * Periodically purges trashed notes older than 30 days.
 * Schedule this worker to run daily using WorkManager.
 */
class TrashWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            val dao = KitabuDatabase.getDatabase(applicationContext).noteDao()
            dao.purgeExpiredTrash(cutoff)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "kitabu_trash_purge"

        /**
         * Schedule daily trash purge check.
         * Call this from Application.onCreate().
         */
        fun schedule(context: Context) {
            try {
                val request = androidx.work.PeriodicWorkRequestBuilder<TrashWorker>(
                    1, java.util.concurrent.TimeUnit.DAYS
                ).build()
                androidx.work.WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        UNIQUE_WORK_NAME,
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        request
                    )
            } catch (_: Exception) {
                // WorkManager may not be available in all configurations
            }
        }
    }
}
