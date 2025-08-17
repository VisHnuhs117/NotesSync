package com.vishnuhs.notessync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseSyncRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val authRepository = AuthRepository()

    // Get user-specific notes collection
    private fun getUserNotesCollection() =
        firestore.collection("users")
            .document(authRepository.getCurrentUserId())
            .collection("notes")

    // Upload a note to Firebase (user-specific)
    suspend fun uploadNote(note: Note): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }

            Log.d("FirebaseSync", "Uploading note: ${note.title} for user: $currentUserId")

            val noteWithUserId = note.copy(userId = currentUserId)
            val noteData = noteWithUserId.toFirebaseMap()

            Log.d("FirebaseSync", "Note data being uploaded: $noteData")

            getUserNotesCollection()
                .document(note.id)
                .set(noteData)
                .await()

            Log.d("FirebaseSync", "Upload successful for note: ${note.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Upload failed for note: ${note.id}", e)
            Result.failure(e)
        }
    }

    // Download user's notes from Firebase
    suspend fun downloadNotes(): Result<List<Note>> {
        return try {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }

            Log.d("FirebaseSync", "Downloading notes for user: $currentUserId")
            val querySnapshot = getUserNotesCollection().get().await()
            Log.d("FirebaseSync", "Found ${querySnapshot.documents.size} documents")

            val notes = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Log.d("FirebaseSync", "Document: ${doc.id}, Data: $data")

                    Note.fromFirebaseMap(data)
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

    // Delete a note from Firebase (user-specific)
    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }

            getUserNotesCollection().document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}