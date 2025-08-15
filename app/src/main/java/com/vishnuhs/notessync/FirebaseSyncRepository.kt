package com.vishnuhs.notessync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseSyncRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val notesCollection = firestore.collection("notes")

    // Upload a note to Firebase
    suspend fun uploadNote(note: Note): Result<Unit> {
        return try {
            Log.d("FirebaseSync", "Uploading note: ${note.id} - ${note.title}")

            val noteData = mapOf(
                "id" to note.id,
                "title" to note.title,
                "content" to note.content,
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt,
                "deviceId" to note.deviceId,
                "isDeleted" to note.isDeleted
            )

            Log.d("FirebaseSync", "Note data: $noteData")

            notesCollection.document(note.id)
                .set(noteData)
                .await()

            Log.d("FirebaseSync", "Upload successful for note: ${note.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Upload failed for note: ${note.id}", e)
            Result.failure(e)
        }
    }

    // Download all notes from Firebase
    suspend fun downloadNotes(): Result<List<Note>> {
        return try {
            Log.d("FirebaseSync", "Downloading notes from Firebase")
            val querySnapshot = notesCollection.get().await()
            Log.d("FirebaseSync", "Found ${querySnapshot.documents.size} documents")

            val notes = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Log.d("FirebaseSync", "Document: ${doc.id}, Data: $data")

                    Note(
                        id = data["id"] as String,
                        title = data["title"] as String,
                        content = data["content"] as String,
                        createdAt = data["createdAt"] as Long,
                        updatedAt = data["updatedAt"] as Long,
                        deviceId = data["deviceId"] as String,
                        isDeleted = data["isDeleted"] as Boolean
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Error parsing document ${doc.id}", e)
                    null
                }
            }
            Log.d("FirebaseSync", "Converted to ${notes.size} notes")
            Result.success(notes)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Download failed", e)
            Result.failure(e)
        }
    }

    // Delete a note from Firebase
    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            notesCollection.document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}