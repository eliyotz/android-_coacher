package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coach.screentime.data.db.entities.ReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ReflectionEntity): Long

    @Query("SELECT * FROM reflection_entries WHERE dateLocal = :date LIMIT 1")
    suspend fun byDate(date: String): ReflectionEntity?

    @Query("SELECT * FROM reflection_entries WHERE dateLocal IN (:dates) ORDER BY dateLocal")
    suspend fun forDates(dates: List<String>): List<ReflectionEntity>

    @Query("SELECT * FROM reflection_entries ORDER BY dateLocal DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ReflectionEntity>>
}
