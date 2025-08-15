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
    private val categoryDao = database.categoryDao()
    private val firebaseSync = FirebaseSyncRepository()

    // Search and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Categories flow
    val categories = categoryDao.getAllCategories()

    // All notes from local database with search and category filter
    val allNotes = combine(searchQuery, selectedCategory) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        when {
            query.isBlank() && category == null -> noteDao.getAllNotes()
            query.isBlank() && category != null -> noteDao.getNotesByCategory(category)
            else -> noteDao.searchNotes(query, category)
        }
    }

    // Sync status
    private val _syncStatus = MutableStateFlow("Ready")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // Update search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Update selected category filter
    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    // Add note with category
    fun addNote(title: String, content: String, category: String = "General") {
        viewModelScope.launch {
            val note = Note(
                title = title,
                content = content,
                category = category
            )

            // Save locally first
            noteDao.insertNote(note)

            // Update category if it's new
            categoryDao.insertCategory(
                Category(
                    name = category,
                    color = getColorForCategory(category)
                )
            )

            // Then sync to Firebase
            syncNoteToFirebase(note)
        }
    }

    // Get color for category (you can customize this)
    private fun getColorForCategory(category: String): String {
        return when (category) {
            "Work" -> "#FF5722"
            "Personal" -> "#2196F3"
            "Ideas" -> "#FF9800"
            "Important" -> "#F44336"
            "To-Do" -> "#4CAF50"
            "Shopping" -> "#9C27B0"
            "Travel" -> "#00BCD4"
            "Health" -> "#8BC34A"
            "Finance" -> "#FFC107"
            else -> "#6200EE"
        }
    }

    // Rest of your existing functions remain the same...
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
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