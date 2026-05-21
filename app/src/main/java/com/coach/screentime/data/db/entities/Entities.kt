package com.coach.screentime.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dailyMinutesCap: Int? = null,
    val weeklyMinutesCap: Int? = null,
)

@Entity(
    tableName = "apps",
    indices = [Index("categoryId")]
)
data class AppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val categoryId: String,
    val isFlagged: Boolean = false,
    val perAppDailyMinutesCap: Int? = null,
    val hardLockEnabled: Boolean = false,
    val hardLockToggleAt: Long = 0L,
)

@Entity(
    tableName = "sessions",
    indices = [Index("packageName"), Index("startTs")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startTs: Long,
    val endTs: Long,
    val durationSec: Int,
    val dateLocal: String,
)

@Entity(
    tableName = "daily_rollups",
    primaryKeys = ["dateLocal", "packageName"],
    indices = [Index("dateLocal")]
)
data class DailyRollupEntity(
    val dateLocal: String,
    val packageName: String,
    val totalSec: Int,
    val opensCount: Int,
    val longestSessionSec: Int,
)

@Entity(
    tableName = "interventions",
    indices = [Index("ts"), Index("packageName")]
)
data class InterventionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val packageName: String,
    val layer: String,
    val outcome: String,
    val triggerKind: String,
    val triggerValue: String,
)

@Entity(
    tableName = "ai_verdicts",
    indices = [Index("interventionId")]
)
data class AiVerdictEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val interventionId: Long,
    val reasonText: String,
    val verdict: String,
    val explanation: String,
    val rawResponse: String,
    val ts: Long,
)

@Entity(tableName = "weekly_reports")
data class WeeklyReportEntity(
    @PrimaryKey val weekStart: String,
    val markdownBody: String,
    val generatedAt: Long,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val active: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "reflection_entries",
    indices = [Index(value = ["dateLocal"], unique = true)]
)
data class ReflectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateLocal: String,
    val feeling: Int,
    val triggerText: String,
    val createdAt: Long,
)

@Entity(
    tableName = "nudge_entries",
    indices = [Index("ts")]
)
data class NudgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val title: String,
    val body: String,
    val actionKind: String,
    val actionTaken: Boolean,
    val rawResponse: String,
)

@Entity(tableName = "goal_revisions")
data class GoalRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val generatedAt: Long,
    val oldGoal: String,
    val suggestedGoal: String,
    val rationale: String,
    val resolution: String, // "pending" | "accepted" | "edited" | "dismissed"
    val resolvedGoal: String?,
    val resolvedAt: Long?,
)

/** Local mirror of a Google Tasks task. Authoritative copy is in Google's API. */
@Entity(
    tableName = "tasks_mirror",
    indices = [Index("dueDateMs"), Index("completed")]
)
data class TaskEntity(
    @PrimaryKey val googleId: String,
    val listId: String,
    val title: String,
    val notes: String,
    val dueDateMs: Long?,
    val completed: Boolean,
    val etag: String,
    val fetchedAt: Long,
)

@Entity(tableName = "task_states")
data class TaskStateEntity(
    @PrimaryKey val googleId: String,
    val state: String, // untouched | prompted | working | delayed | dismissed_pending | judged_done
    val lastPromptedAt: Long,
    val workingSinceAt: Long,
    val delayedUntilMs: Long,
    val lastJudgmentTs: Long,
    val silenceCheckedAt: Long, // last time silence was evaluated; 0 if never
    val repromptCount: Int = 0,             // how many re-prompts have fired today (resets daily)
    val repromptCountResetDay: String = "", // "YYYY-MM-DD" of last reset
)

@Entity(
    tableName = "task_delays",
    indices = [Index("googleId"), Index("ts")]
)
data class TaskDelayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val googleId: String,
    val ts: Long,
    val requestedDelayMs: Long,
    val granted: Boolean,
    val reason: String,
    val aiExplanation: String,
)

@Entity(
    tableName = "punishments",
    indices = [Index("expiresAt"), Index("decidedAt")]
)
data class PunishmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val decidedAt: Long,
    val source: String, // "task" | "manual"
    val taskGoogleId: String?,
    val expiresAt: Long,
    val blockedPackagesJson: String, // JSON array of package names
    val capReductionPct: Int,
    val focusMinutes: Int,
    val mindfulPauseMultiplier: Float,
    val rationale: String,
    val severity: String, // "light" | "medium" | "harsh"
)
