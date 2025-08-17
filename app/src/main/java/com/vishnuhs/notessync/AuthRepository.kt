package com.vishnuhs.notessync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.tasks.await
import android.util.Log

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    init {
        Log.d("AuthRepository", "Firebase Auth initialized")
    }

    // Get current user
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Get current user ID
    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    // Get user display info
    fun getUserDisplayName(): String = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "User"
    fun getUserEmail(): String = auth.currentUser?.email ?: ""

    // Sign in anonymously (fallback)
    suspend fun signInAnonymously(): Result<FirebaseUser?> {
        return try {
            Log.d("AuthRepository", "Attempting anonymous sign-in")
            val result = auth.signInAnonymously().await()
            Log.d("AuthRepository", "Anonymous sign-in successful: ${result.user?.uid}")
            Result.success(result.user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Anonymous sign-in failed", e)
            Result.failure(e)
        }
    }

    // Sign up with email and password
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            Log.d("AuthRepository", "Attempting sign-up with email: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Log.d("AuthRepository", "Sign-up successful: ${result.user?.email}")
            Result.success(result.user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign-up failed", e)
            Result.failure(e)
        }
    }

    // Sign in with email and password
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            Log.d("AuthRepository", "Attempting sign-in with email: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Log.d("AuthRepository", "Sign-in successful: ${result.user?.email}")
            Result.success(result.user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign-in failed", e)
            Result.failure(e)
        }
    }

    // Link anonymous account with email/password
    suspend fun linkAnonymousWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            if (!isAnonymousUser()) {
                return Result.failure(Exception("User is not anonymous"))
            }

            Log.d("AuthRepository", "Linking anonymous account with email: $email")
            val credential = EmailAuthProvider.getCredential(email, password)
            val result = auth.currentUser?.linkWithCredential(credential)?.await()
            Log.d("AuthRepository", "Account linking successful: ${result?.user?.email}")
            Result.success(result?.user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Account linking failed", e)
            Result.failure(e)
        }
    }

    // Check if user is signed in
    fun isUserSignedIn(): Boolean = auth.currentUser != null

    // Check if user is anonymous
    fun isAnonymousUser(): Boolean {
        val user = auth.currentUser
        val hasEmail = !user?.email.isNullOrEmpty()
        val isAnon = user?.isAnonymous == true && !hasEmail
        Log.d("AuthRepository", "Is anonymous user: $isAnon")
        return isAnon
    }

    // Sign out
    fun signOut() {
        Log.d("AuthRepository", "Signing out user")
        auth.signOut()
    }
}