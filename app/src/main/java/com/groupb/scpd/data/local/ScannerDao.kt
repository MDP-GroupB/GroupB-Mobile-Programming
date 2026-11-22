package com.groupb.scpd.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Data Access Object (DAO) is an interface that defines all the database operations
 *  (like inserting, querying, updating, and deleting rows) for our local SQLite tables
 *  (CachedBookingEntity and PendingAttendanceEntity) we use it instead of writing directly to SQLite
 *  (main benefit is strings fail at build time instead of crashing at runtime) */


@Dao
interface ScannerDao {

    // --- Cached Bookings (Prepare Scanner Roster) ---

    @Upsert
    suspend fun upsertBookings(bookings: List<CachedBookingEntity>)

    @Query("DELETE FROM cached_bookings WHERE eventId = :eventId")
    suspend fun clearBookingsForEvent(eventId: String)

    /** Replaces the cached booking list for an event in a single atomic transaction. */
    @Transaction
    suspend fun replaceBookingsForEvent(eventId: String, bookings: List<CachedBookingEntity>) {
        clearBookingsForEvent(eventId)
        upsertBookings(bookings)
    }

    @Query("SELECT * FROM cached_bookings WHERE eventId = :eventId")
    suspend fun getBookingsForEvent(eventId: String): List<CachedBookingEntity>

    @Query("SELECT COUNT(*) FROM cached_bookings WHERE eventId = :eventId")
    fun observeBookingCountForEvent(eventId: String): Flow<Int>

    // --- Pending Attendance Outbox (Offline Scans) ---

    /**
     * Inserts a new scan if one does not already exist for (eventId, userId).
     * Returns -1L if a row for this student & event was already recorded locally.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingAttendance(record: PendingAttendanceEntity): Long

    @Query("SELECT userId FROM pending_attendance WHERE eventId = :eventId")
    suspend fun getRecordedUserIdsForEvent(eventId: String): List<String>

    @Query("SELECT * FROM pending_attendance WHERE eventId = :eventId ORDER BY scannedAtMillis DESC")
    fun observePendingAttendanceForEvent(eventId: String): Flow<List<PendingAttendanceEntity>>

    @Query("SELECT * FROM pending_attendance WHERE syncState = 'PENDING'")
    suspend fun getAllUnsyncedAttendance(): List<PendingAttendanceEntity>

    @Query("UPDATE pending_attendance SET syncState = :newState WHERE eventId = :eventId AND userId = :userId")
    suspend fun updateSyncState(eventId: String, userId: String, newState: String)
}