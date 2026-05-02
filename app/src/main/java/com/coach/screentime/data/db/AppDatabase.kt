package com.coach.screentime.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.GoalRevisionDao
import com.coach.screentime.data.db.dao.InterventionDao
import com.coach.screentime.data.db.dao.NudgeDao
import com.coach.screentime.data.db.dao.PunishmentDao
import com.coach.screentime.data.db.dao.ReflectionDao
import com.coach.screentime.data.db.dao.ReportDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.SessionDao
import com.coach.screentime.data.db.dao.TaskDao
import com.coach.screentime.data.db.dao.TaskDelayDao
import com.coach.screentime.data.db.dao.TaskStateDao
import com.coach.screentime.data.db.entities.AiVerdictEntity
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.CategoryEntity
import com.coach.screentime.data.db.entities.DailyRollupEntity
import com.coach.screentime.data.db.entities.GoalEntity
import com.coach.screentime.data.db.entities.GoalRevisionEntity
import com.coach.screentime.data.db.entities.InterventionEntity
import com.coach.screentime.data.db.entities.NudgeEntity
import com.coach.screentime.data.db.entities.PunishmentEntity
import com.coach.screentime.data.db.entities.ReflectionEntity
import com.coach.screentime.data.db.entities.SessionEntity
import com.coach.screentime.data.db.entities.TaskDelayEntity
import com.coach.screentime.data.db.entities.TaskEntity
import com.coach.screentime.data.db.entities.TaskStateEntity
import com.coach.screentime.data.db.entities.WeeklyReportEntity

@Database(
    entities = [
        CategoryEntity::class,
        AppEntity::class,
        SessionEntity::class,
        DailyRollupEntity::class,
        InterventionEntity::class,
        AiVerdictEntity::class,
        WeeklyReportEntity::class,
        GoalEntity::class,
        ReflectionEntity::class,
        NudgeEntity::class,
        GoalRevisionEntity::class,
        TaskEntity::class,
        TaskStateEntity::class,
        TaskDelayEntity::class,
        PunishmentEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun categoryDao(): CategoryDao
    abstract fun sessionDao(): SessionDao
    abstract fun rollupDao(): RollupDao
    abstract fun interventionDao(): InterventionDao
    abstract fun reportDao(): ReportDao
    abstract fun goalDao(): GoalDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun nudgeDao(): NudgeDao
    abstract fun goalRevisionDao(): GoalRevisionDao
    abstract fun taskDao(): TaskDao
    abstract fun taskStateDao(): TaskStateDao
    abstract fun taskDelayDao(): TaskDelayDao
    abstract fun punishmentDao(): PunishmentDao

    companion object {
        /** Adds the goal_revisions table without touching any existing data. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goal_revisions (
                      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      generatedAt INTEGER NOT NULL,
                      oldGoal TEXT NOT NULL,
                      suggestedGoal TEXT NOT NULL,
                      rationale TEXT NOT NULL,
                      resolution TEXT NOT NULL,
                      resolvedGoal TEXT,
                      resolvedAt INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds tasks_mirror, task_states, task_delays, punishments. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tasks_mirror (
                      googleId TEXT PRIMARY KEY NOT NULL,
                      listId TEXT NOT NULL,
                      title TEXT NOT NULL,
                      notes TEXT NOT NULL,
                      dueDateMs INTEGER,
                      completed INTEGER NOT NULL,
                      etag TEXT NOT NULL,
                      fetchedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_mirror_dueDateMs ON tasks_mirror(dueDateMs)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_mirror_completed ON tasks_mirror(completed)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_states (
                      googleId TEXT PRIMARY KEY NOT NULL,
                      state TEXT NOT NULL,
                      lastPromptedAt INTEGER NOT NULL,
                      workingSinceAt INTEGER NOT NULL,
                      delayedUntilMs INTEGER NOT NULL,
                      lastJudgmentTs INTEGER NOT NULL,
                      silenceCheckedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_delays (
                      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      googleId TEXT NOT NULL,
                      ts INTEGER NOT NULL,
                      requestedDelayMs INTEGER NOT NULL,
                      granted INTEGER NOT NULL,
                      reason TEXT NOT NULL,
                      aiExplanation TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_delays_googleId ON task_delays(googleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_delays_ts ON task_delays(ts)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS punishments (
                      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      decidedAt INTEGER NOT NULL,
                      source TEXT NOT NULL,
                      taskGoogleId TEXT,
                      expiresAt INTEGER NOT NULL,
                      blockedPackagesJson TEXT NOT NULL,
                      capReductionPct INTEGER NOT NULL,
                      focusMinutes INTEGER NOT NULL,
                      mindfulPauseMultiplier REAL NOT NULL,
                      rationale TEXT NOT NULL,
                      severity TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_punishments_expiresAt ON punishments(expiresAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_punishments_decidedAt ON punishments(decidedAt)")
            }
        }
    }
}
