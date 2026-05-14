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
import java.time.ZoneId
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
                        // User has ignored the prompt long enough — treat as silence.
                        taskStateDao.upsert(state.copy(silenceCheckedAt = now))
                        judge(task, state, userReason = "", userResponded = false)
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
                    val stillOverdue = (task.dueDateMs ?: Long.MAX_VALUE) < now
                    if (!stillBeingPunished && stillOverdue) {
                        firePrompt(task, state, followUp = true)
                    }
                }
            }
        }
    }

    /** Mark a task done as far as we can tell (next sync may flip it). */
    suspend fun markCompleted(googleId: String) {
        val state = taskStateDao.byId(googleId) ?: return
        taskStateDao.upsert(state.copy(state = TasksRepository.STATE_JUDGED_DONE))
        cancelTaskNotification(googleId)
    }

    /** User tapped "Yes, I'm doing it" on the prompt. */
    suspend fun markWorking(googleId: String) {
        val now = System.currentTimeMillis()
        val state = taskStateDao.byId(googleId) ?: return
        taskStateDao.upsert(
            state.copy(
                state = TasksRepository.STATE_WORKING,
                workingSinceAt = now,
            )
        )
        cancelTaskNotification(googleId)
    }

    /** User tapped "No, ask the coach". */
    suspend fun submitReason(googleId: String, reason: String) {
        val task = taskDao.byId(googleId) ?: return
        val state = taskStateDao.byId(googleId) ?: return
        cancelTaskNotification(googleId)
        judge(task, state, userReason = reason, userResponded = true)
    }

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

    private suspend fun judge(
        task: TaskEntity,
        state: TaskStateEntity,
        userReason: String,
        userResponded: Boolean,
    ) {
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

        val priorDelaysCsv = priorDelays.joinToString("\n") { d ->
            val reason = d.reason.replace("\n", " ").replace(",", ";").take(140)
            "${d.ts},${d.granted},$reason"
        }
        val priorJudgmentsCsv = priorDelays
            .filter { it.aiExplanation.isNotBlank() }
            .joinToString("\n") { it.aiExplanation.replace("\n", " ").replace(",", ";").take(200) }

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

        val raw = gemini.generate(systemPrompt, userPrompt).getOrNull()
        val verdict = raw?.let { parseVerdict(it) } ?: deterministicFallback(severityFloor, rollups, apps)

        applyVerdict(task, state, verdict, userReason)
    }

    private fun parseVerdict(raw: String): TaskVerdict? {
        val trimmed = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return runCatching { verdictAdapter.fromJson(trimmed) }.getOrNull()
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
            "Coach unreachable. Forcing focus mode for ${forcedFocusMinutes} min — all flagged apps locked."
        } else {
            "Coach unreachable. Default punishment: blocking your top apps for ${durationH}h."
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

    private suspend fun applyVerdict(
        task: TaskEntity,
        state: TaskStateEntity,
        verdict: TaskVerdict,
        userReason: String,
    ) {
        val now = System.currentTimeMillis()
        when (verdict.decision) {
            "allow_delay" -> {
                val until = verdict.delay?.untilIso?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                    ?: (now + TimeUnit.HOURS.toMillis(4)) // fallback: 4h delay
                taskDelayDao.insert(
                    TaskDelayEntity(
                        googleId = task.googleId,
                        ts = now,
                        requestedDelayMs = max(0L, until - now),
                        granted = true,
                        reason = userReason,
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
                    focusMinutes = p.focusMinutes,
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
                        reason = userReason,
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
        val due = task.dueDateMs ?: return false
        return due < nowMs
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
    }
}
