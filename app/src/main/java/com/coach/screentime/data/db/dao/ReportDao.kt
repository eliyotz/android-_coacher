package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.coach.screentime.data.db.entities.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Upsert
    suspend fun upsert(report: WeeklyReportEntity)

    @Query("SELECT * FROM weekly_reports ORDER BY weekStart DESC")
    fun observeAll(): Flow<List<WeeklyReportEntity>>

    @Query("SELECT * FROM weekly_reports ORDER BY weekStart DESC LIMIT 1")
    fun observeLatest(): Flow<WeeklyReportEntity?>

    @Query("SELECT * FROM weekly_reports WHERE weekStart = :weekStart LIMIT 1")
    suspend fun byWeek(weekStart: String): WeeklyReportEntity?
}
