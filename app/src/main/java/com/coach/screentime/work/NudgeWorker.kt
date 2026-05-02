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
import com.coach.screentime.ai.GeminiClient
import com.coach.screentime.ai.NudgePrompt
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.NudgeDao
import com.coach.screentime.data.db.dao.ReflectionDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.SessionDao
import com.coach.screentime.data.db.entities.NudgeEntity
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.focus.FocusActionReceiver
import com.coach.screentime.insights.StreakCalculator
import com.coach.screentime.tracking.NotifChannels
import com.coach.screentime.ui.MainActivity
import com.coach.screentime.util.Time
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Periodic AI check-in. Runs every ~2 hours but only fires a notification when
 * the model returns `nudge: true`. Skipped during night hours and when nudges
 * are disabled in settings.
 */
@HiltWorker
class NudgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val geminiClient: GeminiClient,
    private val rollupDao: RollupDao,
    private val sessionDao: SessionDao,
    private val appDao: AppDao,
    private val goalDao: GoalDao,
    private val nudgeDao: NudgeDao,
    private val reflectionDao: ReflectionDao,
    private val settingsStore: SettingsStore,
    private val streakCalculator: StreakCalculator,
) : CoroutineWorker(context, params) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val nudgeAdapter = moshi.adapter(NudgeResponse::class.java)

    override suspend fun doWork(): Result {
        if (!settingsStore.nudgesEnabled.first()) return Result.success()
        val now = LocalDateTime.now(ZoneId.systemDefault())
        if (now.hour < DAY_START_HOUR || now.hour >= DAY_END_HOUR) return Result.success()

        val nowMs = System.currentTimeMillis()
        // Don't pester: skip if a nudge already fired in the last 90 minutes.
        if (nudgeDao.countSince(nowMs - 90 * 60_000L) > 0) return Result.success()

        val goal = goalDao.activeGoal()?.text.orEmpty()
        val streak = streakCalculator.current()
        val today = Time.todayString()
        val apps = appDao.snapshot().associateBy { it.packageName }
        val todayRollups = rollupDao.snapshotForDate(today)
        val flaggedRollups = todayRollups.filter { apps[it.packageName]?.isFlagged == true || it.totalSec >= 5 * 60 }
            .sortedByDescending { it.totalSec }
            .take(8)

        val rollupsCsv = flaggedRollups.joinToString("\n") { r ->
            val name = apps[r.packageName]?.displayName ?: r.packageName
            "$name,${r.totalSec / 60},${r.opensCount}"
        }

        // Opens in the last 4 hours, per flagged package.
        val opensSince = nowMs - 4 * 60 * 60_000L
        val flaggedPackages = apps.values.filter { it.isFlagged }.map { it.packageName }
        val opensCsv = flaggedPackages.mapNotNull { pkg ->
            val n = sessionDao.openCountSince(pkg, opensSince)
            if (n == 0) null else "${apps[pkg]?.displayName ?: pkg},$n"
        }.joinToString("\n")

        val reflections = reflectionDao.forDates(Time.lastNDaysStrings(7))
            .take(5)
            .joinToString("\n") { r ->
                "${r.dateLocal},${r.feeling},${r.triggerText.replace("\n", " ").replace(",", ";").take(120)}"
            }
        val recentNudges = nudgeDao.since(nowMs - 24 * 60 * 60_000L)
            .joinToString("\n") { "${it.ts},${it.title.replace(",", ";")},${it.actionKind}" }

        val systemPrompt = NudgePrompt.system
        val userPrompt = NudgePrompt.user(
            nowLocal = now,
            goal = goal,
            currentStreakDays = streak,
            recentOpensCsv = opensCsv,
            todayRollupsCsv = rollupsCsv,
            recentReflectionsCsv = reflections,
            recentNudgesCsv = recentNudges,
        )

        val response = geminiClient.generate(systemPrompt, userPrompt).getOrElse {
            return Result.retry()
        }
        val parsed = parseNudge(response) ?: return Result.success()
        if (!parsed.nudge) return Result.success()

        val title = parsed.title?.take(60) ?: return Result.success()
        val body = parsed.body?.take(220) ?: return Result.success()
        val action = parsed.action ?: "none"

        val nudgeId = nudgeDao.insert(
            NudgeEntity(
                ts = nowMs,
                title = title,
                body = body,
                actionKind = action,
                actionTaken = false,
                rawResponse = response,
            )
        )
        fireNotification(nudgeId, title, body, action)
        return Result.success()
    }

    private fun fireNotification(nudgeId: Long, title: String, body: String, action: String) {
        val openIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(applicationContext, NotifChannels.NUDGE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)

        if (action == "focus30") {
            val focusIntent = Intent(applicationContext, FocusActionReceiver::class.java).apply {
                this.action = FocusActionReceiver.ACTION_START
                putExtra(FocusActionReceiver.EXTRA_MINUTES, 30)
            }
            val pi = PendingIntent.getBroadcast(
                applicationContext,
                nudgeId.toInt(),
                focusIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(R.drawable.ic_launcher_foreground, "Lock 30 min", pi)
        }

        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(NotifChannels.NUDGE_NOTIF_ID + nudgeId.toInt(), builder.build())
    }

    private fun parseNudge(raw: String): NudgeResponse? {
        val trimmed = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return runCatching { nudgeAdapter.fromJson(trimmed) }.getOrNull()
    }

    @JsonClass(generateAdapter = true)
    data class NudgeResponse(
        val nudge: Boolean = false,
        val title: String? = null,
        val body: String? = null,
        val action: String? = null,
    )

    companion object {
        private const val DAY_START_HOUR = 8
        private const val DAY_END_HOUR = 22
    }
}
