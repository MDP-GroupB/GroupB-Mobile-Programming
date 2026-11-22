package com.groupb.scpd.ui.signin

import com.groupb.scpd.data.AuthRepository
import com.groupb.scpd.data.model.UserProfile
import kotlinx.coroutines.CompletableDeferred

/** In-memory AuthRepository for tests: no Firebase, no internet. */
class FakeAuthRepository : AuthRepository {
    var profileToReturn = UserProfile(name = "Test Student", email = "student@test.com")
    var errorToThrow: Exception? = null
    var gate: CompletableDeferred<Unit>? = null   // lets a test hold sign-in "in progress"
    var signInCalls = 0
    var signOutCalls = 0

    override suspend fun signIn(email: String, password: String): UserProfile {
        signInCalls++
        gate?.await()
        errorToThrow?.let { throw it }
        return profileToReturn
    }

    override fun signOut() {
        signOutCalls++
    }
}