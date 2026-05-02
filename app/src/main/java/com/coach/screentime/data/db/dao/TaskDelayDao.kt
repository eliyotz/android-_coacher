package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.coach.screentime.data.db.entities.TaskDelayEntity

@Dao
interface TaskDelayDao {
    @Insert
    suspend fun insert(delay: TaskDelayEntity): Long

    @Query("SELECT * FROM task_delays WHERE googleId = :id ORDER BY ts DESC")
    suspend fun forTask(id: String): List<TaskDelayEntity>

    @Query("SELECT COUNT(*) FROM task_delays WHERE googleId = :id AND granted = 1")
    suspend fun grantedCountForTask(id: String): Int
}
