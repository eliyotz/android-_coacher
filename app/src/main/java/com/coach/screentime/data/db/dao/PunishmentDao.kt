package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.coach.screentime.data.db.entities.PunishmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PunishmentDao {
    @Insert
    suspend fun insert(p: PunishmentEntity): Long

    @Query("SELECT * FROM punishments WHERE expiresAt > :nowMs ORDER BY decidedAt DESC")
    suspend fun activeSnapshot(nowMs: Long): List<PunishmentEntity>

    @Query("SELECT * FROM punishments WHERE expiresAt > :nowMs ORDER BY decidedAt DESC")
    fun observeActive(nowMs: Long): Flow<List<PunishmentEntity>>

    @Query("SELECT * FROM punishments ORDER BY decidedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<PunishmentEntity>

    @Query("DELETE FROM punishments WHERE expiresAt < :cutoffMs")
    suspend fun pruneOlderThan(cutoffMs: Long)
}
