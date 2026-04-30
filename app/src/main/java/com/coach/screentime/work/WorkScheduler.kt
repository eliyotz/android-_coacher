package com.coach.screentime.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scheduleAll() {
        scheduleWeeklyReport()
        scheduleDailyPrune()
    }

    private fun scheduleWeeklyReport() {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val nextSun8am = now
            .with(TemporalAdjusters.next(java.time.DayOfWeek.SUNDAY))
            .withHour(8).withMinute(0).withSecond(0).withNano(0)
        val initialDelay = Duration.between(now, nextSun8am).toMillis().coerceAtLeast(0)

        val req = PeriodicWorkRequestBuilder<WeeklyReportWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "weekly_report",
            ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }

    private fun scheduleDailyPrune() {
        val req = PeriodicWorkRequestBuilder<DailyRollupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_prune",
            ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }
}
