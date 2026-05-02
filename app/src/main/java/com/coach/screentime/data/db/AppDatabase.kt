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
import com.coach.screentime.data.db.dao.ReflectionDao
import com.coach.screentime.data.db.dao.ReportDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.SessionDao
import com.coach.screentime.data.db.entities.AiVerdictEntity
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.CategoryEntity
import com.coach.screentime.data.db.entities.DailyRollupEntity
import com.coach.screentime.data.db.entities.GoalEntity
import com.coach.screentime.data.db.entities.GoalRevisionEntity
import com.coach.screentime.data.db.entities.InterventionEntity
import com.coach.screentime.data.db.entities.NudgeEntity
import com.coach.screentime.data.db.entities.ReflectionEntity
import com.coach.screentime.data.db.entities.SessionEntity
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
    ],
    version = 3,
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
    }
}
