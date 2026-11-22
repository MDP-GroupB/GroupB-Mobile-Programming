package com.groupb.scpd.domain.checkin

import java.security.MessageDigest

/**
 * representation of a downloaded booking row needed by the checker.
 */
data class CachedBookingForCheck(
    val eventId: String,
    val userId: String,
    val studentName: String,
    val maskedId: String,
    val slotHashes: List<String>
)

object ScanCodeChecker {

    data class ParsedQrPayload(
        val eventId: String,
        val uid: String,
        val slot: Int,
        val code: String
    )

    /**
     * Parses a pipe-delimited QR payload: "$eventId|$uid|$slot|$code"
     * Matches Area C's CodeGenerator.buildQrPayload format.
     */
    fun parsePayload(rawQrString: String): ParsedQrPayload? {
        val parts = rawQrString.trim().split("|")
        if (parts.size != 4) return null

        val eventId = parts[0].trim()
        val uid = parts[1].trim()
        val slot = parts[2].trim().toIntOrNull() ?: return null
        val code = parts[3].trim().lowercase()

        if (eventId.isEmpty() || uid.isEmpty()) return null
        if (slot !in 0 until CheckInSlot.TOTAL_SLOTS) return null
        if (code.length != 64) return null

        return ParsedQrPayload(
            eventId = eventId,
            uid = uid,
            slot = slot,
            code = code
        )
    }

    /**
     * Validates a scanned or pasted QR string against the event check-in window,
     * downloaded bookings, 20s slot + 5s grace window, SHA-256 slot hashes, and duplicate status.
     */
    fun verifyScan(
        rawQrString: String,
        activeEventId: String,
        eventStartMillis: Long,
        scanTimeMillis: Long,
        bookingsByUserId: Map<String, CachedBookingForCheck>,
        alreadyScannedUserIds: Set<String>
    ): ScanResult {
        // 1. Check QR string format
        val payload = parsePayload(rawQrString) ?: return ScanResult.InvalidFormat

        // 2. Check event ID matches the scanner's active event
        if (payload.eventId != activeEventId) return ScanResult.WrongEvent

        // 3. Check overall check-in window [T-10m, T+30m)
        if (!CheckInWindow.isOpen(eventStartMillis, scanTimeMillis)) {
            return ScanResult.OutsideWindow
        }

        // 4. Look up student booking in downloaded roster
        val booking = bookingsByUserId[payload.uid]
            ?: return ScanResult.NotFound(payload.uid)

        // 5. Verify 20-second slot (and 5-second grace period for previous slot)
        val checkinOpensAtMillis = CheckInWindow.opensAtMillis(eventStartMillis)
        if (!CheckInSlot.isSlotValidAt(checkinOpensAtMillis, scanTimeMillis, payload.slot)) {
            return ScanResult.Expired
        }

        // 6. Verify cryptographic hash: SHA-256(code) == booking.slotHashes[slot]
        if (payload.slot !in booking.slotHashes.indices) return ScanResult.InvalidHash
        val expectedHash = booking.slotHashes[payload.slot].lowercase()
        val actualHash = sha256Hex(payload.code)
        if (actualHash != expectedHash) return ScanResult.InvalidHash

        // 7. Only after proving the pass is authentic & in-slot, check if already recorded
        if (payload.uid in alreadyScannedUserIds) {
            return ScanResult.AlreadyRecorded(
                studentName = booking.studentName,
                maskedId = booking.maskedId
            )
        }

        return ScanResult.Valid(
            userId = payload.uid,
            studentName = booking.studentName,
            maskedId = booking.maskedId,
            slot = payload.slot,
            code = payload.code
        )
    }

    fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}