package com.groupb.scpd.data.local

import androidx.room.Entity
import com.groupb.scpd.domain.checkin.CachedBookingForCheck

/**
 * Local Room table storing an event's confirmed bookings downloaded before check-in opens,
 * allowing the committee scanner to verify student QR codes completely offline.
 */
@Entity(
    tableName = "cached_bookings",
    primaryKeys = ["eventId", "userId"]
)
data class CachedBookingEntity(
    val eventId: String,
    val userId: String,
    val studentName: String,
    val maskedId: String,
    val status: String,           // "confirmed"
    val slotHashesCsv: String     // 120 SHA-256 hex strings joined with ","
) {
    /** Converts this Room row into the pure Kotlin model used by ScanCodeChecker. */
    fun toDomainCheckModel(): CachedBookingForCheck {
        val hashes = if (slotHashesCsv.isBlank()) {
            emptyList()
        } else {
            slotHashesCsv.split(",")
        }
        return CachedBookingForCheck(
            eventId = eventId,
            userId = userId,
            studentName = studentName,
            maskedId = maskedId,
            slotHashes = hashes
        )
    }

    companion object {
        fun fromFirestoreBooking(
            eventId: String,
            userId: String,
            studentName: String,
            maskedId: String,
            status: String,
            slotHashes: List<String>
        ): CachedBookingEntity {
            return CachedBookingEntity(
                eventId = eventId,
                userId = userId,
                studentName = studentName,
                maskedId = maskedId,
                status = status,
                slotHashesCsv = slotHashes.joinToString(",")
            )
        }
    }
}