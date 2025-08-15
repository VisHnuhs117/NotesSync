package com.vishnuhs.notessync

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT category, COUNT(*) as count FROM notes WHERE isDeleted = 0 GROUP BY category")
    suspend fun getCategoryCounts(): List<CategoryCount>
}

// Data class for category counts
data class CategoryCount(
    val category: String,
    val count: Int
)