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
import kotlinx.coroutines.flow.first


class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NoteDatabase.getDatabase(application)
    private val noteDao = database.noteDao()
    private val categoryDao = database.categoryDao()
    private val firebaseSync = FirebaseSyncRepository()
    private val authRepository = AuthRepository()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

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

    init {
        // Ensure user is authenticated
        ensureUserAuthenticated()
    }

    private fun ensureUserAuthenticated() {
        viewModelScope.launch {
            _syncStatus.value = "Checking authentication..."

            if (!authRepository.isUserSignedIn()) {
                Log.d("NotesViewModel", "User not signed in, attempting anonymous sign-in...")
                val result = authRepository.signInAnonymously()

                if (result.isSuccess) {
                    val userId = authRepository.getCurrentUserId()
                    Log.d("NotesViewModel", "User signed in successfully: $userId")
                    _isAuthenticated.value = true
                    _syncStatus.value = "Ready"
                } else {
                    Log.e("NotesViewModel", "Failed to sign in user", result.exceptionOrNull())
                    _isAuthenticated.value = false
                    _syncStatus.value = "Authentication failed: ${result.exceptionOrNull()?.message}"
                }
            } else {
                val userId = authRepository.getCurrentUserId()
                val isAnon = authRepository.isAnonymousUser()
                Log.d("NotesViewModel", "User already signed in: $userId (anonymous: $isAnon)")

                // Update authentication state properly
                _isAuthenticated.value = true
                _syncStatus.value = if (isAnon) "Ready" else "Signed in as ${authRepository.getUserDisplayName()}"
            }
        }
    }

    // Add note with category
    fun addNote(title: String, content: String, category: String = "General") {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            Log.d("NotesViewModel", "Creating note with category: $category")
            val note = Note(
                title = title,
                content = content,
                category = category,
                userId = currentUserId

            )

            // Save locally first
            noteDao.insertNote(note)
            Log.d("NotesViewModel", "Saved locally: ${note.title} - Category: ${note.category}")

            // Update category in categories table
            categoryDao.insertCategory(
                Category(
                    name = category,
                    color = getColorForCategory(category)
                )
            )

            // Immediately upload to Firebase
            val result = firebaseSync.uploadNote(note)
            if (result.isSuccess) {
                Log.d("NotesViewModel", "Successfully uploaded to Firebase: ${note.title} - Category: ${note.category}")
                // Mark as synced
                noteDao.insertNote(note.copy(lastSyncedAt = System.currentTimeMillis()))
            } else {
                Log.e("NotesViewModel", "Failed to upload to Firebase", result.exceptionOrNull())
            }
        }
    }

    fun getUserDisplayName(): String = authRepository.getUserDisplayName()
    fun getUserEmail(): String = authRepository.getUserEmail()
    fun isAnonymousUser(): Boolean = authRepository.isAnonymousUser()

    // Google Sign-In function
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _syncStatus.value = "Signing in..."

            val result = if (isAnonymousUser()) {
                // Link anonymous account to preserve existing notes
                authRepository.linkAnonymousWithEmail(email, password)
            } else {
                // Direct email sign-in
                authRepository.signInWithEmail(email, password)
            }

            if (result.isSuccess) {
                val user = result.getOrNull()
                Log.d("NotesViewModel", "Email sign-in successful: ${user?.email}")
                _isAuthenticated.value = true
                _syncStatus.value = "Signed in as ${user?.email}"

                // Trigger a sync to merge any cloud notes
                syncAllNotes()
            } else {
                Log.e("NotesViewModel", "Email sign-in failed", result.exceptionOrNull())
                _syncStatus.value = "Sign-in failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _syncStatus.value = "Creating account..."

            val result = authRepository.signUpWithEmail(email, password)

            if (result.isSuccess) {
                val user = result.getOrNull()
                Log.d("NotesViewModel", "Email sign-up successful: ${user?.email}")
                _isAuthenticated.value = true
                _syncStatus.value = "Account created for ${user?.email}"

                // Sync any existing anonymous notes to new account
                syncAllNotes()
            } else {
                Log.e("NotesViewModel", "Email sign-up failed", result.exceptionOrNull())
                _syncStatus.value = "Sign-up failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    // Sign out function
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _isAuthenticated.value = false
            _syncStatus.value = "Signed out - switching to anonymous mode"

            // Sign in anonymously as fallback
            ensureUserAuthenticated()
        }
    }

    fun checkCurrentAuthStatus() {
        viewModelScope.launch {
            Log.d("NotesViewModel", "=== CURRENT AUTH STATUS ===")
            Log.d("NotesViewModel", "Is user signed in: ${authRepository.isUserSignedIn()}")
            Log.d("NotesViewModel", "Is anonymous: ${authRepository.isAnonymousUser()}")
            Log.d("NotesViewModel", "User ID: ${authRepository.getCurrentUserId()}")
            Log.d("NotesViewModel", "Display name: ${authRepository.getUserDisplayName()}")
            Log.d("NotesViewModel", "Email: ${authRepository.getUserEmail()}")
            Log.d("NotesViewModel", "_isAuthenticated.value: ${_isAuthenticated.value}")
            Log.d("NotesViewModel", "Sync status: ${_syncStatus.value}")
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
                // Get current local notes
                val currentLocalNotes = noteDao.getAllNotes().first()
                Log.d("NotesViewModel", "=== BEFORE SYNC ===")
                currentLocalNotes.forEach { note ->
                    Log.d("NotesViewModel", "Local note: ${note.title} - Category: ${note.category}")
                }

                // First, upload all local notes that are newer
                currentLocalNotes.forEach { localNote ->
                    Log.d("NotesViewModel", "Uploading note: ${localNote.title} - Category: ${localNote.category}")
                    val result = firebaseSync.uploadNote(localNote)
                    if (result.isSuccess) {
                        // Mark as synced locally
                        noteDao.insertNote(localNote.copy(lastSyncedAt = System.currentTimeMillis()))
                    } else {
                        Log.e("NotesViewModel", "Failed to upload: ${localNote.title}")
                    }
                }

                // Then download from Firebase
                val firebaseResult = firebaseSync.downloadNotes()

                if (firebaseResult.isSuccess) {
                    val firebaseNotes = firebaseResult.getOrNull() ?: emptyList()
                    Log.d("NotesViewModel", "=== FIREBASE DATA ===")
                    firebaseNotes.forEach { note ->
                        Log.d("NotesViewModel", "Firebase note: ${note.title} - Category: ${note.category}")
                    }

                    // SMART MERGE: Only add notes that don't exist locally
                    val localNoteIds = currentLocalNotes.map { it.id }.toSet()

                    firebaseNotes.forEach { firebaseNote ->
                        if (firebaseNote.id !in localNoteIds) {
                            // This is a new note from another device
                            Log.d("NotesViewModel", "Adding new note from Firebase: ${firebaseNote.title} - Category: ${firebaseNote.category}")
                            noteDao.insertNote(firebaseNote.copy(lastSyncedAt = System.currentTimeMillis()))
                        } else {
                            // Note exists locally - keep local version (since we just uploaded it)
                            Log.d("NotesViewModel", "Keeping local version: ${firebaseNote.title}")
                        }
                    }

                    _syncStatus.value = "Synced successfully"
                } else {
                    _syncStatus.value = "Sync failed: ${firebaseResult.exceptionOrNull()?.message}"
                }

                // Debug after sync
                val afterSyncNotes = noteDao.getAllNotes().first()
                Log.d("NotesViewModel", "=== AFTER SYNC ===")
                afterSyncNotes.forEach { note ->
                    Log.d("NotesViewModel", "Final note: ${note.title} - Category: ${note.category}")
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

    fun updateNote(noteId: String, title: String, content: String, category: String) {
        viewModelScope.launch {
            try {
                // Find the existing note
                val existingNotes = noteDao.getAllNotes().first()
                val noteToUpdate = existingNotes.find { it.id == noteId }

                if (noteToUpdate != null) {
                    val currentUserId = authRepository.getCurrentUserId()
                    Log.d("NotesViewModel", "Updating note: $title - Category: $category")

                    // Create updated note
                    val updatedNote = noteToUpdate.copy(
                        title = title,
                        content = content,
                        category = category,
                        userId = currentUserId,
                        updatedAt = System.currentTimeMillis(),
                        lastSyncedAt = 0L // Mark as needs sync
                    )

                    // Update locally
                    noteDao.updateNote(updatedNote)
                    Log.d("NotesViewModel", "Updated locally: ${updatedNote.title} - Category: ${updatedNote.category}")

                    // Update category if it's new
                    categoryDao.insertCategory(
                        Category(
                            name = category,
                            color = getColorForCategory(category)
                        )
                    )

                    // Sync to Firebase
                    val result = firebaseSync.uploadNote(updatedNote)
                    if (result.isSuccess) {
                        Log.d("NotesViewModel", "Successfully synced update to Firebase")
                        // Mark as synced
                        noteDao.updateNote(updatedNote.copy(lastSyncedAt = System.currentTimeMillis()))
                    } else {
                        Log.e("NotesViewModel", "Failed to sync update to Firebase", result.exceptionOrNull())
                    }
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Error updating note", e)
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

