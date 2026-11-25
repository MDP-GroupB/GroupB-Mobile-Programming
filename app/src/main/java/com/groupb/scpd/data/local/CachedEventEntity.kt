package com.groupb.scpd.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room table storing event timing and points downloaded during Prepare Scanner,
 * so the scanner can verify check-in windows and queue attendance completely offline.
 */
@Entity(tableName = "cached_events")
data class CachedEventEntity(
    @PrimaryKey val eventId: String,
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val points: Int,
    val updatedAtMillis: Long
)