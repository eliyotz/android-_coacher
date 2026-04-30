package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.coach.screentime.data.db.entities.DailyRollupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RollupDao {
    @Upsert
    suspend fun upsertAll(rollups: List<DailyRollupEntity>)

    @Query("SELECT * FROM daily_rollups WHERE dateLocal = :date")
    fun observeForDate(date: String): Flow<List<DailyRollupEntity>>

    @Query("SELECT * FROM daily_rollups WHERE dateLocal = :date")
    suspend fun snapshotForDate(date: String): List<DailyRollupEntity>

    @Query("SELECT * FROM daily_rollups WHERE dateLocal IN (:dates) ORDER BY dateLocal")
    suspend fun snapshotForDates(dates: List<String>): List<DailyRollupEntity>

    @Query("SELECT COALESCE(SUM(totalSec), 0) FROM daily_rollups WHERE dateLocal = :date AND packageName IN (:packages)")
    suspend fun totalSecondsForPackages(date: String, packages: List<String>): Int

    @Query("SELECT COALESCE(SUM(totalSec), 0) FROM daily_rollups WHERE dateLocal = :date AND packageName = :pkg")
    suspend fun totalSecondsForPackage(date: String, pkg: String): Int
}
