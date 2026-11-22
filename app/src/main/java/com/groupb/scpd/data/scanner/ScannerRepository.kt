package com.groupb.scpd.data.scanner

import com.google.firebase.firestore.FirebaseFirestore
import com.groupb.scpd.data.local.CachedBookingEntity
import com.groupb.scpd.data.local.ScannerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

interface ScannerRepository {
    /** Downloads all confirmed bookings for [eventId] from Firestore and caches them in Room. */
    suspend fun downloadAndCacheBookings(eventId: String): Int

    /** Observes how many bookings are currently cached offline in Room for [eventId]. */
    fun observeCachedBookingCount(eventId: String): Flow<Int>
}

class FirestoreScannerRepository(
    private val firestore: FirebaseFirestore,
    private val scannerDao: ScannerDao
) : ScannerRepository {

    override suspend fun downloadAndCacheBookings(eventId: String): Int {
        val trimmedId = eventId.trim()
        require(trimmedId.isNotEmpty()) { "Event ID cannot be empty" }

        val documents = try {
            fetchConfirmedBookings(trimmedId)
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

        val entities = documents.map { doc ->
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

    override fun observeCachedBookingCount(eventId: String): Flow<Int> {
        return scannerDao.observeBookingCountForEvent(eventId.trim())
    }

    private suspend fun fetchConfirmedBookings(eventId: String) =
        suspendCancellableCoroutine { continuation ->
            firestore.collection("events")
                .document(eventId)
                .collection("bookings")
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (continuation.isActive) {
                        continuation.resume(snapshot.documents)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }
}