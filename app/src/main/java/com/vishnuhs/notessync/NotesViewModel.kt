package com.vishnuhs.notessync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.util.Log
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest


class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NoteDatabase.getDatabase(application)
    private val noteDao = database.noteDao()
    private val firebaseSync = FirebaseSyncRepository()

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // All notes from local database with search
    val allNotes = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            noteDao.getAllNotes()
        } else {
            noteDao.searchNotes(query)
        }
    }

    // Sync status
    private val _syncStatus = MutableStateFlow("Ready")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // Add this function to update search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Rest of your existing functions remain the same...
    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val note = Note(
                title = title,
                content = content
            )

            // Save locally first
            noteDao.insertNote(note)

            // Then sync to Firebase
            syncNoteToFirebase(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            // Delete locally
            noteDao.deleteNote(note)

            // Delete from Firebase
            firebaseSync.deleteNote(note.id)
        }
    }

    fun syncAllNotes() {
        viewModelScope.launch {
            _syncStatus.value = "Syncing..."

            try {
                // Download notes from Firebase
                val result = firebaseSync.downloadNotes()

                if (result.isSuccess) {
                    val firebaseNotes = result.getOrNull() ?: emptyList()

                    // Simple merge: add Firebase notes to local database
                    firebaseNotes.forEach { firebaseNote ->
                        try {
                            noteDao.insertNote(firebaseNote.copy(lastSyncedAt = System.currentTimeMillis()))
                        } catch (e: Exception) {
                            Log.e("NotesViewModel", "Error inserting note from Firebase", e)
                        }
                    }

                    _syncStatus.value = "Synced successfully"
                } else {
                    _syncStatus.value = "Sync failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _syncStatus.value = "Sync failed: ${e.message}"
                Log.e("NotesViewModel", "Sync error", e)
            }
        }
    }

    fun testFirebaseConnection() {
        viewModelScope.launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                // Simple test data
                val testData = mapOf(
                    "test" to "Hello Firebase!",
                    "timestamp" to System.currentTimeMillis()
                )

                Log.d("FirebaseTest", "Attempting to write test data...")

                firestore.collection("test")
                    .document("test-doc")
                    .set(testData)
                    .addOnSuccessListener {
                        Log.d("FirebaseTest", "Test write SUCCESS!")
                        _syncStatus.value = "Firebase connected!"
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseTest", "Test write FAILED", exception)
                        _syncStatus.value = "Firebase connection failed: ${exception.message}"
                    }

            } catch (e: Exception) {
                Log.e("FirebaseTest", "Firebase test error", e)
                _syncStatus.value = "Test failed: ${e.message}"
            }
        }
    }

    private fun syncNoteToFirebase(note: Note) {
        viewModelScope.launch {
            try {
                val result = firebaseSync.uploadNote(note)
                if (result.isFailure) {
                    Log.e("NotesViewModel", "Failed to upload note", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Error uploading note", e)
            }
        }
    }
}