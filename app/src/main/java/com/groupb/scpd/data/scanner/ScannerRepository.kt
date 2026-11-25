package com.groupb.scpd.data.scanner

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.groupb.scpd.data.local.CachedBookingEntity
import com.groupb.scpd.data.local.CachedEventEntity
import com.groupb.scpd.data.local.PendingAttendanceEntity
import com.groupb.scpd.data.local.ScannerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.io.IOException

interface ScannerRepository {
    /** Downloads event metadata and confirmed bookings for [eventId] from Firestore into Room. */
    suspend fun downloadAndCacheBookings(eventId: String): Int

    /** Observes how many bookings are currently cached offline in Room for [eventId]. */
    fun observeCachedBookingCount(eventId: String): Flow<Int>

    suspend fun getCachedEvent(eventId: String): CachedEventEntity?
    suspend fun getCachedBookings(eventId: String): List<CachedBookingEntity>
    suspend fun getRecordedUserIds(eventId: String): Set<String>

    /** Saves a verified scan to Room. Returns true if inserted, false if already recorded. */
    suspend fun savePendingAttendance(record: PendingAttendanceEntity): Boolean

    fun observePendingAttendance(eventId: String): Flow<List<PendingAttendanceEntity>>
    fun currentScannerUid(): String
}

class FirestoreScannerRepository(
    private val firestore: FirebaseFirestore,
    private val scannerDao: ScannerDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ScannerRepository {

    override suspend fun downloadAndCacheBookings(eventId: String): Int {
        val trimmedId = eventId.trim()
        require(trimmedId.isNotEmpty()) { "Event ID cannot be empty" }

        val (eventDoc, bookingDocs) = try {
            val eventSnapshot = firestore.collection("events")
                .document(trimmedId)
                .get()
                .await()

            val bookingsSnapshot = firestore.collection("events")
                .document(trimmedId)
                .collection("bookings")
                .whereEqualTo("status", "confirmed")
                .get()
                .await()

            eventSnapshot to bookingsSnapshot.documents
        } catch (e: FirebaseNetworkException) {
            throw IOException("No connection", e)
        } catch (e: FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    throw SecurityException("Permission denied", e)
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    throw IOException("No connection", e)
                else -> throw e
            }
        }

        if (eventDoc.exists()) {
            val cachedEvent = CachedEventEntity(
                eventId = trimmedId,
                title = eventDoc.getString("title") ?: trimmedId,
                startTimeMillis = eventDoc.getTimestamp("startTime")?.toDate()?.time ?: 0L,
                endTimeMillis = eventDoc.getTimestamp("endTime")?.toDate()?.time ?: 0L,
                points = (eventDoc.getLong("points") ?: eventDoc.getLong("requestedPoints") ?: 0L).toInt(),
                updatedAtMillis = System.currentTimeMillis()
            )
            scannerDao.upsertEvent(cachedEvent)
        }

        val entities = bookingDocs.map { doc ->
            val userId = doc.getString("userId") ?: doc.id
            val studentName = doc.getString("studentName") ?: "Unknown Student"
            val maskedId = doc.getString("maskedId") ?: "***"
            val status = doc.getString("status") ?: "confirmed"
            val slotHashes = (doc.get("slotHashes") as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()

            CachedBookingEntity.fromFirestoreBooking(
                eventId = trimmedId,
                userId = userId,
                studentName = studentName,
                maskedId = maskedId,
                status = status,
                slotHashes = slotHashes
            )
        }

        scannerDao.replaceBookingsForEvent(trimmedId, entities)
        return entities.size
    }

    override fun observeCachedBookingCount(eventId: String): Flow<Int> =
        scannerDao.observeBookingCountForEvent(eventId.trim())

    override suspend fun getCachedEvent(eventId: String): CachedEventEntity? =
        scannerDao.getEvent(eventId.trim())

    override suspend fun getCachedBookings(eventId: String): List<CachedBookingEntity> =
        scannerDao.getBookingsForEvent(eventId.trim())

    override suspend fun getRecordedUserIds(eventId: String): Set<String> =
        scannerDao.getRecordedUserIdsForEvent(eventId.trim()).toSet()

    override suspend fun savePendingAttendance(record: PendingAttendanceEntity): Boolean =
        scannerDao.insertPendingAttendance(record) != -1L

    override fun observePendingAttendance(eventId: String): Flow<List<PendingAttendanceEntity>> =
        scannerDao.observePendingAttendanceForEvent(eventId.trim())

    override fun currentScannerUid(): String = auth.currentUser?.uid.orEmpty()
}