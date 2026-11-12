package com.groupb.scpd.domain.checkin

/**
 * Every possible outcome right after a committee member scans a code.
 * This is a closed list on purpose: the checker, the Room status column,
 * and the scanner screen's colour/message all read from this one list.
 */
sealed interface ScanResult {
    /** Green: the code matched a booking and this student hasn't been scanned before. */
    data class Valid(
        val userId: String,
        val studentName: String,
        val maskedId: String,
        val slot: Int,
        val code: String
    ) : ScanResult

    /** Green-ish / Info: this student's booking was already scanned. No duplicate is recorded. */
    data class AlreadyRecorded(val studentName: String, val maskedId: String) : ScanResult

    /** Red: the scanned or pasted string is not formatted as a valid QR payload. */
    data object InvalidFormat : ScanResult

    /** Red: the QR pass is for a different event than the scanner is currently checking. */
    data object WrongEvent : ScanResult

    /** Red: scan happened too early (< T-10m) or too late (>= T+30m). */
    data object OutsideWindow : ScanResult

    /** Red: the student UID is not in the downloaded confirmed booking list. */
    data class NotFound(val scannedUserId: String) : ScanResult

    /** Red: the code's 20-second slot (plus 5-second grace period) has expired or is in the future. */
    data object Expired : ScanResult

    /** Red: the code's hash does not match the booking's pre-committed slotHashes[slot]. */
    data object InvalidHash : ScanResult
}