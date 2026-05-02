package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.coach.screentime.data.db.entities.NudgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NudgeDao {
    @Insert
    suspend fun insert(nudge: NudgeEntity): Long

    @Query("UPDATE nudge_entries SET actionTaken = 1 WHERE id = :id")
    suspend fun markActionTaken(id: Long)

    @Query("SELECT * FROM nudge_entries WHERE ts >= :sinceTs ORDER BY ts DESC")
    suspend fun since(sinceTs: Long): List<NudgeEntity>

    @Query("SELECT COUNT(*) FROM nudge_entries WHERE ts >= :sinceTs")
    suspend fun countSince(sinceTs: Long): Int

    @Query("SELECT * FROM nudge_entries ORDER BY ts DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NudgeEntity>>
}
