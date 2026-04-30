package com.coach.screentime.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.coach.screentime.data.db.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun snapshot(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): CategoryEntity?
}
