package com.coach.screentime.work

import android.app.ActivityManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.tracking.ForegroundAppService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Belt-and-suspenders revival of [ForegroundAppService] on OEMs that aggressively
 * kill background services (Samsung, Xiaomi, Oppo, OnePlus, Realme). Runs every
 * 15 minutes via WorkManager. If the service isn't currently running, restarts it.
 */
@HiltWorker
class ServiceWatchdogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsStore: SettingsStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!settingsStore.onboarded.first()) return Result.success()
        if (!isServiceRunning(applicationContext, ForegroundAppService::class.java.name)) {
            ForegroundAppService.start(applicationContext)
        }
        return Result.success()
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, className: String): Boolean {
        // getRunningServices is deprecated for third-party use BUT still returns
        // your own services. That's exactly what we need.
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == className }
    }
}
