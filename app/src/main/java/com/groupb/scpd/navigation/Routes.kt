package com.groupb.scpd.navigation

/** Route names for the top-level screens. Each area adds its own routes here. */
object Routes {
    const val HOME = "home"                 // Area A: the menu after sign-in. Its buttons depend on the user's role.
    const val SIGN_IN = "sign_in"           // Area A: the sign-in form
    const val STUDENT_HOME = "student_home" // Area B: Discover events
    const val CLUB_HOME = "club_home"       // Area A: Manage events
    const val SCANNER_HOME = "scanner_home" // Area D: Scanner
    const val PASS = "pass"                 // Area C: Pass QR screen

    const val SCAN = "scan/{eventId}"       // Area D: QR & paste-code scan screen
    fun scanRoute(eventId: String): String = "scan/${android.net.Uri.encode(eventId)}"
}