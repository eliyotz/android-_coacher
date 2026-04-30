package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.coach.screentime.data.db.entities.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Query("UPDATE goals SET active = 0")
    suspend fun deactivateAll()

    @Query("SELECT * FROM goals WHERE active = 1 LIMIT 1")
    suspend fun activeGoal(): GoalEntity?

    @Query("SELECT * FROM goals WHERE active = 1 LIMIT 1")
    fun observeActiveGoal(): Flow<GoalEntity?>
}
