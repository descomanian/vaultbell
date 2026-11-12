package io.github.descomanian.vaultbell.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.descomanian.vaultbell.VaultbellApplication
import io.github.descomanian.vaultbell.data.ScanOutcome
import java.util.concurrent.TimeUnit

class ScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as VaultbellApplication).container.repository
        return when (repo.scan(force = inputData.getBoolean(KEY_FORCE, false))) {
            is ScanOutcome.Failed -> if (runAttemptCount < 2) Result.retry() else Result.failure()
            else -> Result.success()
        }
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}

object ScanWork {
    private const val PERIODIC = "vault-scan-periodic"
    private const val ONE_SHOT = "vault-scan-now"

    /** Minimum interval is 15 minutes (WorkManager limit). No network constraint: we never use it. */
    fun schedulePeriodic(
        context: Context,
        intervalMinutes: Int
    ) {
        val request =
            PeriodicWorkRequestBuilder<ScanWorker>(intervalMinutes.coerceAtLeast(15).toLong(), TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun runNow(
        context: Context,
        force: Boolean = false
    ) {
        val request = OneTimeWorkRequestBuilder<ScanWorker>().setInputData(workDataOf(ScanWorker.KEY_FORCE to force)).build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_SHOT, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }
}
