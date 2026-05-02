package com.coach.screentime.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coach.screentime.ai.GeminiClient
import com.coach.screentime.ai.WeeklyReportPrompt
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.InterventionDao
import com.coach.screentime.data.db.dao.ReflectionDao
import com.coach.screentime.data.db.dao.ReportDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.entities.WeeklyReportEntity
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.insights.StreakCalculator
import com.coach.screentime.util.Time
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
    private val appDao: AppDao,
    private val reflectionDao: ReflectionDao,
    private val streakCalculator: StreakCalculator,
    private val gemini: GeminiClient,
    private val settingsStore: SettingsStore,
) : CoroutineWorker(appContext, params) {

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
                Result.success()
            },
            onFailure = { Result.retry() },
        )
    }
}
