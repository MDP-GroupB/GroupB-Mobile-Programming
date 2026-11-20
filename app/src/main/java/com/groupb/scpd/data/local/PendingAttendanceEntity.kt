package com.groupb.scpd.data.local

import androidx.room.Entity

/**
 * Local Room outbox table holding attendance records scanned offline,
 * queued to be uploaded to events/{eventId}/attendance/{userId}.
 */
@Entity(
    tableName = "pending_attendance",
    primaryKeys = ["eventId", "userId"]
)
data class PendingAttendanceEntity(
    val eventId: String,
    val userId: String,
    val studentName: String,
    val maskedId: String,
    val method: String,          // "qr" or "manual"
    val slot: Int?,              // 0..119 for "qr", null for "manual"
    val code: String?,           // 64-char hex code for "qr", null for "manual"
    val scannedAtMillis: Long,
    val scannedByUid: String,
    val points: Int,
    val eventEndMillis: Long,
    val reason: String? = null,
    val idCheckMethod: String? = null,
    val syncState: String = "PENDING" // "PENDING", "VALIDATED", "DUPLICATE", "REJECTED"
)