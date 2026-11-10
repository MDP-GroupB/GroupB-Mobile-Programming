package com.groupb.scpd.domain.checkin

/**
 * Every possible outcome right after a committee member scans a code.
 * This is a closed list on purpose: the checker, the Room status column
 * and the scanner screen's colour/message all read from this one list,
 * so they can never disagree about what outcomes exist.
 */
sealed interface ScanResult {
    /** Green: the code matched a booking and this slot hasn't been scanned before. */
    data object Valid : ScanResult

    /** Still green-ish: this exact booking was already scanned. No duplicate is recorded. */
    data object AlreadyRecorded : ScanResult

    /** Red: the code's 20-second slot is too old to still be valid. */
    data object Expired : ScanResult

    /** Red: the code doesn't match anything in the downloaded booking list. */
    data object NotFound : ScanResult

    /** Red: the code is valid, but it's too early or too late relative to the event's check-in window. */
    data object OutsideWindow : ScanResult
}