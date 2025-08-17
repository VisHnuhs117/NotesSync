package com.vishnuhs.notessync

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [Note::class, Category::class],
    version = 4,  // Increment version
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        // Previous migrations...
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN deviceId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN category TEXT NOT NULL DEFAULT 'General'")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        name TEXT PRIMARY KEY NOT NULL,
                        color TEXT NOT NULL,
                        noteCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
                val defaultCategories = listOf("General", "Work", "Personal", "Ideas", "Important")
                defaultCategories.forEach { category ->
                    database.execSQL(
                        "INSERT OR REPLACE INTO categories (name, color, noteCount, createdAt) VALUES (?, ?, ?, ?)",
                        arrayOf(category, "#6200EE", 0, System.currentTimeMillis())
                    )
                }
            }
        }

        // New migration to add userId
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // Add new migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}