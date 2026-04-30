package com.coach.screentime.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.InterventionDao
import com.coach.screentime.data.db.dao.ReportDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.SessionDao
import com.coach.screentime.data.db.entities.AiVerdictEntity
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.CategoryEntity
import com.coach.screentime.data.db.entities.DailyRollupEntity
import com.coach.screentime.data.db.entities.GoalEntity
import com.coach.screentime.data.db.entities.InterventionEntity
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
    ],
    version = 1,
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
}
