package com.coach.screentime.enforcement

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.coach.screentime.R
import com.coach.screentime.ai.GeminiClient
import com.coach.screentime.ai.TaskJudgePrompt
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.TaskDao
import com.coach.screentime.data.db.dao.TaskDelayDao
import com.coach.screentime.data.db.dao.TaskStateDao
import com.coach.screentime.data.db.entities.TaskDelayEntity
import com.coach.screentime.data.db.entities.TaskEntity
import com.coach.screentime.data.db.entities.TaskStateEntity
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.intervention.OverlayManager
import com.coach.screentime.punishment.PunishmentManager
import com.coach.screentime.tasks.TasksRepository
import com.coach.screentime.tracking.NotifChannels
import com.coach.screentime.util.Time
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Drives the per-task escalation state machine and the AI judgment that
 * fires when the user says "no" or stays silent.
 *
 * State transitions (managed by this class together with the
 * TaskCheckActionReceiver which receives notification button taps):
 *
 *   untouched → prompted (when worker fires the prompt)
 *   prompted → working   (user tapped "Yes")
 *   working → prompted   (30-min follow-up worker, still incomplete)
 *   prompted → delayed   (AI grants delay)
 *   prompted → dismissed_pending (AI punishes; punishment lives in punishments table)
 *   prompted → (silence path) → AI judgment with userResponded=false → either delayed/dismissed
 *   any      → judged_done (next sync sees task is completed in Google)
 */
@Singleton
class TaskEscalationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val taskStateDao: TaskStateDao,
    private val taskDelayDao: TaskDelayDao,
    private val rollupDao: RollupDao,
    private val appDao: AppDao,
    private val goalDao: GoalDao,
    private val settingsStore: SettingsStore,
    private val gemini: GeminiClient,
    private val punishmentManager: PunishmentManager,
    private val overlayManager: OverlayManager,
) {
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val verdictAdapter = moshi.adapter(TaskVerdict::class.java)

    /**
     * Called by the [com.coach.screentime.work.TaskCheckWorker] every 15 min.
     * For each candidate task, decides whether to fire a fresh prompt or
     * escalate a stale one to AI judgment for silence.
     */
    suspend fun runCycle() {
        val now = System.currentTimeMillis()
        val open = taskDao.openSnapshot()
        for (task in open) {
            val state = taskStateDao.byId(task.googleId) ?: continue
            when (state.state) {
                TasksRepository.STATE_UNTOUCHED -> {
                    if (isOverdue(task, now)) firePrompt(task, state)
                }
                TasksRepository.STATE_DELAYED -> {
                    if (state.delayedUntilMs > 0 && state.delayedUntilMs <= now) {
                        firePrompt(task, state)
                    }
                }
                TasksRepository.STATE_PROMPTED -> {
                    val sinceMs = now - state.lastPromptedAt
                    if (sinceMs > SILENCE_TIMEOUT_MS && state.silenceCheckedAt < state.lastPromptedAt) {
                        // Atomically claim the silence check — only the first concurrent worker wins.
                        val claimed = taskStateDao.tryClaimSilenceCheck(task.googleId, now)
                        if (claimed > 0) judge(task, state, userReason = "", userResponded = false)
                    }
                }
                TasksRepository.STATE_WORKING -> {
                    if (now - state.workingSinceAt > FOLLOWUP_MS) {
                        // 30 min later, still uncompleted — re-prompt.
                        firePrompt(task, state, followUp = true)
                    }
                }
                TasksRepository.STATE_DISMISSED_PENDING -> {
                    // The user "served their sentence" — once every punishment
                    // tied to this task has expired, prompt them again. Without
                    // this branch the task is effectively forgotten after the
                    // first dismissal, which defeats the whole "can't ignore me"
                    // design.
                    val stillBeingPunished = punishmentManager.activeNow(now)
                        .any { it.taskGoogleId == task.googleId }
                    val stillOverdue = isOverdue(task, now)
                    if (!stillBeingPunished && stillOverdue) {
                        val today = java.time.LocalDate.now().toString()
                        val countToday = if (state.repromptCountResetDay == today) state.repromptCount else 0
                        if (countToday < MAX_REPROMPTS_PER_DAY) {
                            val updatedState = state.copy(
                                repromptCount = countToday + 1,
                                repromptCountResetDay = today,
                            )
                            taskStateDao.upsert(updatedState)
                            firePrompt(task, updatedState, followUp = true)
                        }
                    }
                }
            }
        }
    }

    /** Mark a task done as far as we can tell (next sync may flip it). */
    suspend fun markCompleted(googleId: String) {
        val now = System.currentTimeMillis()
        val state = taskStateDao.byId(googleId) ?: defaultState(googleId, now)
        taskStateDao.upsert(state.copy(state = TasksRepository.STATE_JUDGED_DONE))
        cancelTaskNotification(googleId)
    }

    /** User tapped "Yes, I'm doing it" on the prompt. */
    suspend fun markWorking(googleId: String) {
        val now = System.currentTimeMillis()
        val state = taskStateDao.byId(googleId) ?: defaultState(googleId, now)
        taskStateDao.upsert(
            state.copy(
                state = TasksRepository.STATE_WORKING,
                workingSinceAt = now,
            )
        )
        cancelTaskNotification(googleId)
    }

    /**
     * User tapped "No, ask the coach" in the bottom sheet.
     * Returns the coach's explanation so the UI can show it immediately.
     */
    suspend fun submitReason(googleId: String, reason: String): String? {
        val task = taskDao.byId(googleId) ?: return null
        val now = System.currentTimeMillis()
        // A task that was never prompted by the background worker has no state row yet.
        // Create a default "prompted" state so judge() can proceed.
        val state = taskStateDao.byId(googleId) ?: defaultState(googleId, now).also { taskStateDao.upsert(it) }
        cancelTaskNotification(googleId)
        return judge(task, state, userReason = reason, userResponded = true)
    }

    /** Synthesize a first-time state row for a task that was tapped before the engine ever prompted it. */
    private fun defaultState(googleId: String, now: Long) = TaskStateEntity(
        googleId = googleId,
        state = TasksRepository.STATE_PROMPTED,
        lastPromptedAt = now,
        workingSinceAt = 0L,
        delayedUntilMs = 0L,
        lastJudgmentTs = 0L,
        silenceCheckedAt = 0L,
    )

    private suspend fun firePrompt(task: TaskEntity, state: TaskStateEntity, followUp: Boolean = false) {
        val now = System.currentTimeMillis()
        taskStateDao.upsert(
            state.copy(
                state = TasksRepository.STATE_PROMPTED,
                lastPromptedAt = now,
            )
        )

        val noIntent = pi(
            TaskCheckActionReceiver.ACTION_NO,
            mapOf(TaskCheckActionReceiver.EXTRA_TASK_ID to task.googleId),
            requestCode = task.googleId.hashCode(),
        )
        val yesIntent = pi(
            TaskCheckActionReceiver.ACTION_YES,
            mapOf(TaskCheckActionReceiver.EXTRA_TASK_ID to task.googleId),
            requestCode = task.googleId.hashCode() xor 1,
        )

        val title = if (followUp) "Still on \"${task.title}\"?" else "Are you doing this?"
        val body = if (followUp)
            "It's been 30 min and \"${task.title}\" still isn't done."
        else
            "\"${task.title}\" is overdue. Are you doing it now?"
        val n = NotificationCompat.Builder(context, NotifChannels.INTERVENTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_launcher_foreground, "Yes, doing it", yesIntent)
            .addAction(R.drawable.ic_launcher_foreground, "No — ask coach", noIntent)
            .build()
        nm().notify(taskNotificationId(task.googleId), n)
    }

    /** Returns the coach's explanation text so callers can surface it in the UI. */
    private suspend fun judge(
        task: TaskEntity,
        state: TaskStateEntity,
        userReason: String,
        userResponded: Boolean,
    ): String {
        val priorDelays = taskDelayDao.forTask(task.googleId)
        val grantedCount = priorDelays.count { it.granted }
        val severityFloor = when {
            grantedCount >= 2 -> "harsh"
            grantedCount >= 1 || !userResponded -> "medium"
            else -> "light"
        }

        val settings = settingsStore.snapshot()
        val goal = goalDao.activeGoal()?.text.orEmpty()
        val today = Time.todayString()
        val rollups = rollupDao.snapshotForDate(today)
        val apps = appDao.snapshot().associateBy { it.packageName }
        val topApps = rollups
            .filter { apps[it.packageName]?.isFlagged == true }
            .sortedByDescending { it.totalSec }
            .take(8)
            .joinToString("\n") { r ->
                val a = apps[r.packageName]
                "${r.packageName},${a?.displayName ?: r.packageName},${r.totalSec / 60}"
            }

        val recentDelays = priorDelays.take(5) // DAO returns DESC; keep most recent 5
        val priorDelaysCsv = recentDelays.joinToString("\n") { d ->
            val reason = d.reason.replace("\n", " ").replace(",", ";").take(140)
            "${d.ts},${d.granted},$reason"
        }.take(1200)
        val priorJudgmentsCsv = recentDelays
            .filter { it.aiExplanation.isNotBlank() }
            .joinToString("\n") { it.aiExplanation.replace("\n", " ").replace(",", ";").take(200) }
            .take(1000)

        val daysOverdue = task.dueDateMs?.let {
            val due = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            (LocalDate.now().toEpochDay() - due.toEpochDay()).coerceAtLeast(0L).toInt()
        } ?: 0
        val dueIso = task.dueDateMs?.let { Instant.ofEpochMilli(it).toString() }
        val nowLocal = LocalDateTime.now().withSecond(0).withNano(0).toString()

        val systemPrompt = TaskJudgePrompt.system(settings.strictness)
        val userPrompt = TaskJudgePrompt.user(
            nowLocal = nowLocal,
            taskTitle = task.title,
            taskNotes = task.notes,
            dueIso = dueIso,
            daysOverdue = daysOverdue,
            userReason = userReason,
            userResponded = userResponded,
            priorDelaysCsv = priorDelaysCsv,
            priorJudgmentsCsv = priorJudgmentsCsv,
            topAppsCsv = topApps,
            goal = goal,
            strictness = settings.strictness.name,
        )

        val geminiResult = gemini.generate(systemPrompt, userPrompt)
        if (geminiResult.isFailure) {
            android.util.Log.e("TaskCoach", "Gemini call failed for task '${task.title}'", geminiResult.exceptionOrNull())
        }
        val raw = geminiResult.getOrNull()
        if (raw != null) android.util.Log.d("TaskCoach", "Gemini raw response: $raw")
        val parsedVerdict = raw?.let { parseVerdict(it) }
        val fromFallback = parsedVerdict == null
        val verdict = parsedVerdict ?: deterministicFallback(severityFloor, rollups, apps)

        return applyVerdict(task, state, verdict, userReason, fromFallback)
    }

    private fun parseVerdict(raw: String): TaskVerdict? {
        val trimmed = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return runCatching { verdictAdapter.fromJson(trimmed) }.getOrNull()
    }

    /**
     * The AI sometimes returns a fully-qualified Instant ("2026-05-24T09:00:00Z"), sometimes an
     * offset datetime ("…+03:00"), and sometimes a naive local datetime ("2026-05-24T09:00:00").
     * Try them in order; naive strings are interpreted in the device's local zone.
     */
    private fun parseFlexibleDateTimeMs(iso: String): Long? {
        runCatching { return Instant.parse(iso).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
        runCatching { return ZonedDateTime.parse(iso).toInstant().toEpochMilli() }
        runCatching { return LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        return null
    }

    private fun deterministicFallback(
        severity: String,
        rollups: List<com.coach.screentime.data.db.entities.DailyRollupEntity>,
        apps: Map<String, com.coach.screentime.data.db.entities.AppEntity>,
    ): TaskVerdict {
        val targets = rollups
            .filter { apps[it.packageName]?.isFlagged == true }
            .sortedByDescending { it.totalSec }
            .take(3)
            .map { it.packageName }
        val durationH = when (severity) { "harsh" -> 8; "medium" -> 3; else -> 1 }
        // If we have no usage data to target specific apps (new user, sparse
        // rollups), an empty-block punishment is invisible to the user — they
        // would experience "nothing happened" and feel safe ignoring the coach
        // forever. Force Focus mode for the same window so there's a real
        // consequence regardless of usage history.
        val noTargets = targets.isEmpty()
        val forcedFocusMinutes = if (noTargets) (durationH * 60).coerceAtMost(180) else 0
        val explanation = if (noTargets) {
            "Entering focus mode for ${forcedFocusMinutes} min — all flagged apps locked."
        } else {
            "Applying default enforcement: blocking your top apps for ${durationH}h."
        }
        return TaskVerdict(
            decision = "punish",
            explanation = explanation,
            delay = null,
            punishment = TaskPunishment(
                blockedPackages = targets,
                capReductionPct = if (severity == "harsh") 30 else 10,
                focusMinutes = forcedFocusMinutes,
                mindfulPauseMultiplier = if (severity == "harsh") 2.0f else 1.5f,
                durationHours = durationH,
                severity = severity,
            ),
        )
    }

    /** Applies the verdict, posts the explanation notification, and returns the explanation text. */
    private suspend fun applyVerdict(
        task: TaskEntity,
        state: TaskStateEntity,
        verdict: TaskVerdict,
        userReason: String,
        fromFallback: Boolean = false,
    ): String {
        // If the AI never saw the reason (fallback triggered because Gemini was unreachable),
        // don't record it — it shouldn't count toward future AI judgment history.
        val reasonToRecord = if (fromFallback) "" else userReason
        val now = System.currentTimeMillis()
        when (verdict.decision) {
            "allow_delay" -> {
                val parsedIso = verdict.delay?.untilIso?.let(::parseFlexibleDateTimeMs)
                if (verdict.delay?.untilIso != null && parsedIso == null) {
                    android.util.Log.w("TaskCoach", "Could not parse delay.untilIso='${verdict.delay.untilIso}', falling back to 4h")
                }
                val until = parsedIso ?: (now + TimeUnit.HOURS.toMillis(4)) // fallback: 4h delay
                taskDelayDao.insert(
                    TaskDelayEntity(
                        googleId = task.googleId,
                        ts = now,
                        requestedDelayMs = max(0L, until - now),
                        granted = true,
                        reason = reasonToRecord,
                        aiExplanation = verdict.explanation.take(400),
                    )
                )
                taskStateDao.upsert(
                    state.copy(
                        state = TasksRepository.STATE_DELAYED,
                        delayedUntilMs = until,
                        lastJudgmentTs = now,
                    )
                )
                postExplanationNotification(task, verdict, accepted = true)
            }
            else -> {
                // punish (default if decision is anything else or null)
                val p = verdict.punishment ?: deterministicFallback(
                    severity = "light",
                    rollups = emptyList(),
                    apps = emptyMap(),
                ).punishment!!
                val durationMs = TimeUnit.HOURS.toMillis(p.durationHours.coerceIn(1, 24).toLong())
                punishmentManager.apply(
                    source = "task",
                    taskGoogleId = task.googleId,
                    blockedPackages = p.blockedPackages.orEmpty(),
                    capReductionPct = p.capReductionPct,
                    focusMinutes = p.focusMinutes.coerceIn(0, 240),
                    mindfulPauseMultiplier = p.mindfulPauseMultiplier,
                    durationMs = durationMs,
                    rationale = verdict.explanation,
                    severity = p.severity,
                )
                taskDelayDao.insert(
                    TaskDelayEntity(
                        googleId = task.googleId,
                        ts = now,
                        requestedDelayMs = 0L,
                        granted = false,
                        reason = reasonToRecord,
                        aiExplanation = verdict.explanation.take(400),
                    )
                )
                taskStateDao.upsert(
                    state.copy(
                        state = TasksRepository.STATE_DISMISSED_PENDING,
                        lastJudgmentTs = now,
                    )
                )
                postExplanationNotification(task, verdict, accepted = false)
            }
        }
        return verdict.explanation
    }

    private fun postExplanationNotification(task: TaskEntity, verdict: TaskVerdict, accepted: Boolean) {
        val title = if (accepted) "Coach: delay granted" else "Coach: punishment in effect"
        val body = verdict.explanation.take(220)
        val n = NotificationCompat.Builder(context, NotifChannels.INTERVENTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        nm().notify(NotifChannels.NUDGE_NOTIF_ID + 50_000 + task.googleId.hashCode(), n)
    }

    private fun cancelTaskNotification(googleId: String) {
        nm().cancel(taskNotificationId(googleId))
    }

    private fun nm() = context.getSystemService(NotificationManager::class.java)

    private fun pi(
        action: String,
        extras: Map<String, String>,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, TaskCheckActionReceiver::class.java).apply {
            this.action = action
            extras.forEach { (k, v) -> putExtra(k, v) }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun taskNotificationId(googleId: String): Int =
        NotifChannels.NUDGE_NOTIF_ID + 60_000 + googleId.hashCode()

    private fun isOverdue(task: TaskEntity, nowMs: Long): Boolean {
        if (task.dueDateMs == null) {
            // No due date set: treat as overdue after 48 h in the mirror so undated
            // tasks can't silently bypass enforcement forever.
            return (nowMs - task.fetchedAt) > 48 * 3_600_000L
        }
        return task.dueDateMs < nowMs
    }

    @JsonClass(generateAdapter = true)
    data class TaskVerdict(
        val decision: String,
        val explanation: String = "",
        val delay: TaskDelay? = null,
        val punishment: TaskPunishment? = null,
    )

    @JsonClass(generateAdapter = true)
    data class TaskDelay(val untilIso: String? = null)

    @JsonClass(generateAdapter = true)
    data class TaskPunishment(
        val blockedPackages: List<String>? = null,
        val capReductionPct: Int = 0,
        val focusMinutes: Int = 0,
        val mindfulPauseMultiplier: Float = 1f,
        val durationHours: Int = 1,
        val severity: String = "light",
    )

    private companion object {
        const val SILENCE_TIMEOUT_MS = 15 * 60_000L
        const val FOLLOWUP_MS = 30 * 60_000L
        const val MAX_REPROMPTS_PER_DAY = 3
    }
}
