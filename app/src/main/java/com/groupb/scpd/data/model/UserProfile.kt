package com.groupb.scpd.data.model

/** One users/{uid} document (see the Firestore schema doc). Defaults let Firestore build it. */
data class UserProfile(
    val name: String = "",
    val email: String = "",
    val maskedId: String = "",
    val role: String = "student",       // "student" or "admin"
    val managedClubId: String? = null,  // set by admin for event managers
)