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
