package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.coach.screentime.data.db.entities.TaskStateEntity
import kotlinx.coroutines.flow.Flow

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

    /** Atomically stamps silenceCheckedAt only if it hasn't been stamped since the last prompt.
     *  Returns the number of rows updated (1 = claimed, 0 = already claimed by another worker). */
    @Query("UPDATE task_states SET silenceCheckedAt = :now WHERE googleId = :id AND silenceCheckedAt < lastPromptedAt")
    suspend fun tryClaimSilenceCheck(id: String, now: Long): Int

    @Query("SELECT * FROM task_states")
    fun observeAll(): Flow<List<TaskStateEntity>>
}
