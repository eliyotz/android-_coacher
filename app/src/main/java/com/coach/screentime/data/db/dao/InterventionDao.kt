package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.coach.screentime.data.db.entities.AiVerdictEntity
import com.coach.screentime.data.db.entities.InterventionEntity

@Dao
interface InterventionDao {
    @Insert
    suspend fun insert(intervention: InterventionEntity): Long

    @Insert
    suspend fun insertVerdict(verdict: AiVerdictEntity): Long

    @Query("SELECT * FROM interventions WHERE ts >= :sinceTs ORDER BY ts DESC")
    suspend fun since(sinceTs: Long): List<InterventionEntity>

    @Query("SELECT * FROM interventions WHERE packageName = :pkg AND ts >= :sinceTs ORDER BY ts DESC LIMIT :limit")
    suspend fun recentForApp(pkg: String, sinceTs: Long, limit: Int): List<InterventionEntity>

    @Query("SELECT * FROM ai_verdicts WHERE interventionId IN (:interventionIds) ORDER BY ts")
    suspend fun verdictsFor(interventionIds: List<Long>): List<AiVerdictEntity>

    @Query(
        """
        SELECT v.* FROM ai_verdicts v
        INNER JOIN interventions i ON i.id = v.interventionId
        WHERE i.packageName = :pkg AND v.ts >= :sinceTs
        ORDER BY v.ts DESC LIMIT :limit
        """
    )
    suspend fun recentVerdictsForApp(pkg: String, sinceTs: Long, limit: Int): List<AiVerdictEntity>
}
