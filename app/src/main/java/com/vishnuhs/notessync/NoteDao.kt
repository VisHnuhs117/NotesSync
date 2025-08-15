package com.vishnuhs.notessync

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    // Filter by category
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND category = :category ORDER BY updatedAt DESC")
    fun getNotesByCategory(category: String): Flow<List<Note>>

    // Search with category filter
    @Query("""
        SELECT * FROM notes 
        WHERE isDeleted = 0 
        AND (:category IS NULL OR category = :category)
        AND (title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchNotes(searchQuery: String, category: String? = null): Flow<List<Note>>

    // Get all categories used in notes
    @Query("SELECT DISTINCT category FROM notes WHERE isDeleted = 0")
    suspend fun getUsedCategories(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}