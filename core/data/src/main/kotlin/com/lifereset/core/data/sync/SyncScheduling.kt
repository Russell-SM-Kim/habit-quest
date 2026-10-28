package com.lifereset.core.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifereset.core.data.DataContainerOwner
import java.util.concurrent.TimeUnit

/** 저장소가 '바뀌었으니 곧 동기화해 달라'고 알리는 통로. */
fun interface SyncRequester {
    fun requestSync()
}

/**
 * WorkManager 로 동기화를 예약한다.
 * - 앱을 열 때 바로 1회 ([syncNow])
 * - 변경이 생기면 5초 뒤 1회 (연달아 바뀌면 마지막 것만 실행)
 * - 그와 별개로 15분마다 주기 실행 (안드로이드가 허용하는 최소 간격)
 * 네트워크가 없으면 WorkManager 가 연결될 때까지 미뤄준다.
 */
class WorkManagerSyncScheduler(private val context: Context) : SyncRequester {

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun requestSync() = enqueue(delaySeconds = 5)

    /** 앱을 열었을 때 등: 기다리지 않고 바로 (네트워크가 없으면 연결될 때까지 미뤄짐). */
    fun syncNow() = enqueue(delaySeconds = 0)

    private fun enqueue(delaySeconds: Long) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val ONE_TIME = "life-reset-sync"
        const val PERIODIC = "life-reset-sync-periodic"
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val owner = applicationContext as? DataContainerOwner ?: return Result.failure()
        return when (owner.dataContainer.syncManager.sync()) {
            is SyncResult.Success, SyncResult.NotConfigured -> Result.success()
            is SyncResult.Failure -> if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}
