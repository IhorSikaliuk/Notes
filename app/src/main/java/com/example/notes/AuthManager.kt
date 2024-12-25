package com.example.notes

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Registers a new user with email and password.
     *
     * @param email User's email.
     * @param password User's password.
     * @return Result of the registration process.
     */
    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logs in an existing user with email and password.
     *
     * @param email User's email.
     * @param password User's password.
     * @return Result of the login process.
     */
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logs out the current user.
     */
    fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return True if a user is logged in, false otherwise.
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Gets the currently logged-in user's email.
     *
     * @return Email of the logged-in user, or null if no user is logged in.
     */
    fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }
}