package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.coach.screentime.data.db.entities.TaskStateEntity

@Dao
interface TaskStateDao {
    @Upsert
    suspend fun upsert(state: TaskStateEntity)

    @Query("SELECT * FROM task_states WHERE googleId = :id LIMIT 1")
    suspend fun byId(id: String): TaskStateEntity?

    @Query("SELECT * FROM task_states WHERE state = :state")
    suspend fun inState(state: String): List<TaskStateEntity>

    @Query("SELECT * FROM task_states WHERE state IN (:states)")
    suspend fun inStates(states: List<String>): List<TaskStateEntity>

    @Query("DELETE FROM task_states WHERE googleId = :id")
    suspend fun delete(id: String)
}
