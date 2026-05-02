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
import com.coach.screentime.ai.GoalRevisionPrompt
import com.coach.screentime.ai.WeeklyReportPrompt
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.GoalRevisionDao
import com.coach.screentime.data.db.dao.InterventionDao
import com.coach.screentime.data.db.dao.ReflectionDao
import com.coach.screentime.data.db.dao.ReportDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.entities.GoalRevisionEntity
import com.coach.screentime.data.db.entities.WeeklyReportEntity
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.goal.GoalRevisionActivity
import com.coach.screentime.insights.StreakCalculator
import com.coach.screentime.tracking.NotifChannels
import com.coach.screentime.util.Time
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.format.DateTimeFormatter

/**
 * Generates the AI weekly report. Designed to be safe to run multiple times —
 * idempotent on the (weekStart) row.
 */
@HiltWorker
class WeeklyReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val rollupDao: RollupDao,
    private val interventionDao: InterventionDao,
    private val reportDao: ReportDao,
    private val goalDao: GoalDao,
    private val goalRevisionDao: GoalRevisionDao,
    private val appDao: AppDao,
    private val reflectionDao: ReflectionDao,
    private val streakCalculator: StreakCalculator,
    private val gemini: GeminiClient,
    private val settingsStore: SettingsStore,
) : CoroutineWorker(appContext, params) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val revisionAdapter = moshi.adapter(GoalRevisionResponse::class.java)

    override suspend fun doWork(): Result {
        val weekStart = Time.lastCompletedWeekStart()
        val dates = (0..6).map { weekStart.plusDays(it.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE) }

        val rollups = rollupDao.snapshotForDates(dates)
        if (rollups.isEmpty()) return Result.success()

        val apps = appDao.snapshot().associateBy { it.packageName }
        val rollupCsv = rollups.joinToString("\n") { r ->
            val name = apps[r.packageName]?.displayName ?: r.packageName
            "${r.dateLocal},$name,${r.totalSec},${r.opensCount},${r.longestSessionSec}"
        }

        val sinceTs = Time.startOfDayMillis(weekStart)
        val interventions = interventionDao.since(sinceTs).filter { it.ts < Time.startOfDayMillis(weekStart.plusDays(7)) }
        val interventionsCsv = interventions.joinToString("\n") { i -> "${i.ts},${i.packageName},${i.layer},${i.outcome}" }

        val verdicts = interventionDao.verdictsFor(interventions.map { it.id })
        val verdictsCsv = verdicts.joinToString("\n") { v ->
            val r = v.reasonText.replace("\n", " ").replace(",", ";").take(160)
            val e = v.explanation.replace("\n", " ").replace(",", ";").take(200)
            "${v.ts},${v.verdict},$r,$e"
        }

        val goal = goalDao.activeGoal()?.text.orEmpty()
        val streak = streakCalculator.current()
        val reflections = reflectionDao.forDates(dates)
        val reflectionsCsv = reflections.joinToString("\n") { r ->
            val trigger = r.triggerText.replace("\n", " ").replace(",", ";").take(200)
            "${r.dateLocal},${r.feeling},$trigger"
        }
        val systemPrompt = WeeklyReportPrompt.system
        val userPrompt = WeeklyReportPrompt.user(
            weekStart = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
            goal = goal,
            currentStreakDays = streak,
            rollupCsv = rollupCsv,
            interventionsCsv = interventionsCsv,
            verdictsCsv = verdictsCsv,
            reflectionsCsv = reflectionsCsv,
        )

        return gemini.generate(systemPrompt, userPrompt).fold(
            onSuccess = { body ->
                reportDao.upsert(
                    WeeklyReportEntity(
                        weekStart = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        markdownBody = body.trim(),
                        generatedAt = System.currentTimeMillis(),
                    )
                )
                settingsStore.setLastWeeklyReportAt(System.currentTimeMillis())

                // Every 4 reports, ask the AI whether the user's goal still fits.
                // Skipped if a pending revision already exists (don't pile up).
                val totalReports = reportDao.snapshot().size
                if (totalReports > 0 && totalReports % 4 == 0 && goalRevisionDao.pending() == null) {
                    runCatching { maybeRunGoalRevision(goal) }
                }

                Result.success()
            },
            onFailure = { Result.retry() },
        )
    }

    private suspend fun maybeRunGoalRevision(currentGoal: String) {
        val recentReports = reportDao.recent(4)
        if (recentReports.isEmpty()) return
        val concatenated = recentReports.joinToString("\n---\n") { r ->
            "Week of ${r.weekStart}\n${r.markdownBody}"
        }

        val raw = gemini.generate(
            GoalRevisionPrompt.system,
            GoalRevisionPrompt.user(currentGoal, concatenated),
        ).getOrNull() ?: return

        val parsed = parseRevision(raw) ?: return
        val suggested = parsed.suggestedGoal?.trim().orEmpty()
        if (suggested.isBlank()) return

        val id = goalRevisionDao.insert(
            GoalRevisionEntity(
                generatedAt = System.currentTimeMillis(),
                oldGoal = currentGoal,
                suggestedGoal = suggested.take(200),
                rationale = parsed.rationale.orEmpty().take(400),
                resolution = "pending",
                resolvedGoal = null,
                resolvedAt = null,
            )
        )
        fireRevisionNotification(id, suggested)
    }

    private fun parseRevision(raw: String): GoalRevisionResponse? {
        val trimmed = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return runCatching { revisionAdapter.fromJson(trimmed) }.getOrNull()
    }

    private fun fireRevisionNotification(id: Long, suggested: String) {
        val openIntent = PendingIntent.getActivity(
            applicationContext,
            id.toInt(),
            Intent(applicationContext, GoalRevisionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(GoalRevisionActivity.EXTRA_REVISION_ID, id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(applicationContext, NotifChannels.NUDGE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to rethink your goal?")
            .setContentText(suggested.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(suggested))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(NotifChannels.NUDGE_NOTIF_ID + 99_000 + id.toInt(), notif)
    }

    @JsonClass(generateAdapter = true)
    data class GoalRevisionResponse(
        val currentGoal: String? = null,
        val suggestedGoal: String? = null,
        val rationale: String? = null,
    )
}
