package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.coach.screentime.data.db.entities.TaskDelayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDelayDao {
    @Insert
    suspend fun insert(delay: TaskDelayEntity): Long

    @Query("SELECT * FROM task_delays WHERE googleId = :id ORDER BY ts DESC")
    suspend fun forTask(id: String): List<TaskDelayEntity>

    @Query("SELECT COUNT(*) FROM task_delays WHERE googleId = :id AND granted = 1")
    suspend fun grantedCountForTask(id: String): Int

    @Query("SELECT googleId, COUNT(*) AS grantCount FROM task_delays WHERE granted = 1 GROUP BY googleId")
    fun observeGrantedCounts(): Flow<List<TaskGrantCount>>
}

/** Projection for the batch granted-count query. */
data class TaskGrantCount(val googleId: String, val grantCount: Int)
