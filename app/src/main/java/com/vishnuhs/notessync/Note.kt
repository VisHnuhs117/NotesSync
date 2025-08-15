package com.vishnuhs.notessync

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Sync metadata
    val lastSyncedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val deviceId: String = android.os.Build.MODEL // Which device created this note
) {
    // Convert to Firebase format (removes Room annotations)
    fun toFirebaseMap(): Map<String, Any> = mapOf(
        "id" to id,
        "title" to title,
        "content" to content,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "deviceId" to deviceId,
        "isDeleted" to isDeleted
    )

    companion object {
        // Create Note from Firebase data
        fun fromFirebaseMap(data: Map<String, Any>): Note = Note(
            id = data["id"] as String,
            title = data["title"] as String,
            content = data["content"] as String,
            createdAt = data["createdAt"] as Long,
            updatedAt = data["updatedAt"] as Long,
            deviceId = data["deviceId"] as String,
            isDeleted = data["isDeleted"] as Boolean
        )
    }
}