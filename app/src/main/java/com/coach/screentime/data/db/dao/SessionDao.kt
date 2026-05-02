package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.coach.screentime.data.db.entities.SessionEntity

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions WHERE dateLocal = :date ORDER BY startTs")
    suspend fun forDate(date: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE dateLocal IN (:dates) ORDER BY startTs")
    suspend fun forDates(dates: List<String>): List<SessionEntity>

    @Query("DELETE FROM sessions WHERE dateLocal < :cutoffDate")
    suspend fun pruneOlderThan(cutoffDate: String)

    @Query("SELECT COUNT(*) FROM sessions WHERE packageName = :pkg AND startTs >= :sinceTs")
    suspend fun openCountSince(pkg: String, sinceTs: Long): Int

    @Query("SELECT COUNT(*) FROM sessions WHERE packageName IN (:packages) AND startTs >= :sinceTs")
    suspend fun openCountForPackagesSince(packages: List<String>, sinceTs: Long): Int
}
