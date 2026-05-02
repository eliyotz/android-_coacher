package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.coach.screentime.data.db.entities.GoalRevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalRevisionDao {
    @Insert
    suspend fun insert(revision: GoalRevisionEntity): Long

    @Update
    suspend fun update(revision: GoalRevisionEntity)

    @Query("SELECT * FROM goal_revisions WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): GoalRevisionEntity?

    @Query("SELECT * FROM goal_revisions WHERE resolution = 'pending' ORDER BY generatedAt DESC LIMIT 1")
    suspend fun pending(): GoalRevisionEntity?

    @Query("SELECT * FROM goal_revisions WHERE resolution = 'pending' ORDER BY generatedAt DESC LIMIT 1")
    fun observePending(): Flow<GoalRevisionEntity?>

    @Query("SELECT * FROM goal_revisions ORDER BY generatedAt DESC")
    suspend fun snapshot(): List<GoalRevisionEntity>
}
