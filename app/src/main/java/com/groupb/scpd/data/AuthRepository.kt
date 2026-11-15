package com.groupb.scpd.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.groupb.scpd.data.model.UserProfile
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Handles signing in and loading the user's profile from Firestore.
 * It is an interface so that unit tests can swap in a fake version
 * that needs no internet or Firebase.
 */
interface AuthRepository {
    /** Signs in and returns the user's profile  (users/{uid}) record. Shows a message if it fails. */
    suspend fun signIn(email: String, password: String): UserProfile
    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): UserProfile {
        try {
            val result = auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()
            val uid = result.user?.uid ?: throw IllegalStateException("No user")
            // The role lives in users/{uid}; the Security Rules let a user read only their own profile.
            val snapshot = db.collection("users").document(uid).get().await()
            return snapshot.toObject(UserProfile::class.java)
                ?: throw IllegalStateException("No user record for this account")
        } catch (e: FirebaseNetworkException) {
            throw IOException("No connection", e)   // No internet: re-throw as IOException so the ViewModel can show
                                                    // "No internet connection" instead of "Check your password".
        }
    }

    override fun signOut() = auth.signOut()
}