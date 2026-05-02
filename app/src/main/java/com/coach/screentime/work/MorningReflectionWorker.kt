package com.coach.screentime.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coach.screentime.R
import com.coach.screentime.data.db.dao.ReflectionDao
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.reflection.ReflectionActivity
import com.coach.screentime.tracking.NotifChannels
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Fires once a day at ~7 a.m. local time. Skipped if the user already saved
 * a reflection for yesterday, or if reflections are disabled in settings.
 */
@HiltWorker
class MorningReflectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsStore: SettingsStore,
    private val reflectionDao: ReflectionDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!settingsStore.reflectionEnabled.first()) return Result.success()
        val yesterday = LocalDate.now().minusDays(1).toString()
        if (reflectionDao.byDate(yesterday) != null) return Result.success()

        val openIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, ReflectionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(applicationContext, NotifChannels.REFLECTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("How was yesterday?")
            .setContentText("Quick reflection — feeds into Sunday's coach report.")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.notify(NotifChannels.REFLECTION_NOTIF_ID, notif)
        return Result.success()
    }
}
