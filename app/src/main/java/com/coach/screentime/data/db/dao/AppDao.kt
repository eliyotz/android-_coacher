package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.coach.screentime.data.db.entities.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Upsert
    suspend fun upsert(app: AppEntity)

    @Upsert
    suspend fun upsertAll(apps: List<AppEntity>)

    @Update
    suspend fun update(app: AppEntity)

    @Query("SELECT * FROM apps ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isFlagged = 1")
    fun observeFlagged(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isFlagged = 1")
    suspend fun flaggedSnapshot(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE packageName = :pkg LIMIT 1")
    suspend fun byPackage(pkg: String): AppEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM apps WHERE packageName = :pkg AND isFlagged = 1)")
    suspend fun isFlagged(pkg: String): Boolean

    @Query("SELECT packageName FROM apps WHERE categoryId = :categoryId")
    suspend fun packagesInCategory(categoryId: String): List<String>

    @Query("SELECT * FROM apps")
    suspend fun snapshot(): List<AppEntity>
}
