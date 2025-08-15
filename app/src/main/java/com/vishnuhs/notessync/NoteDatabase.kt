package com.vishnuhs.notessync

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [Note::class, Category::class], // Add Category entity
    version = 3,  // Increment version again
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao // Add CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE notes ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE notes ADD COLUMN deviceId TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // Migration from version 2 to 3 (add category)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add category column to notes
                database.execSQL(
                    "ALTER TABLE notes ADD COLUMN category TEXT NOT NULL DEFAULT 'General'"
                )

                // Create categories table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories (
                        name TEXT PRIMARY KEY NOT NULL,
                        color TEXT NOT NULL,
                        noteCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """
                )

                // Insert default categories
                val defaultCategories = listOf("General", "Work", "Personal", "Ideas", "Important")
                defaultCategories.forEach { category ->
                    database.execSQL(
                        "INSERT OR REPLACE INTO categories (name, color, noteCount, createdAt) VALUES (?, ?, ?, ?)",
                        arrayOf(category, "#6200EE", 0, System.currentTimeMillis())
                    )
                }
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Add both migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}