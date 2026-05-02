package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.coach.screentime.data.db.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("SELECT * FROM tasks_mirror WHERE googleId = :id LIMIT 1")
    suspend fun byId(id: String): TaskEntity?

    @Query("SELECT * FROM tasks_mirror WHERE completed = 0 ORDER BY dueDateMs IS NULL, dueDateMs")
    fun observeOpen(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks_mirror WHERE completed = 0 ORDER BY dueDateMs IS NULL, dueDateMs")
    suspend fun openSnapshot(): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks_mirror
        WHERE completed = 0
          AND dueDateMs IS NOT NULL
          AND dueDateMs < :nowMs
    """)
    suspend fun overdueSnapshot(nowMs: Long): List<TaskEntity>

    @Query("DELETE FROM tasks_mirror WHERE googleId NOT IN (:keepIds)")
    suspend fun pruneOthers(keepIds: List<String>)

    @Query("DELETE FROM tasks_mirror")
    suspend fun deleteAll()
}
