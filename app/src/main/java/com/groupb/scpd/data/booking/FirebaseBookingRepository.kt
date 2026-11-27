package com.groupb.scpd.data.booking

import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseBookingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : BookingRepository {

    override suspend fun bookEvent(
        eventId: String,
        studentName: String,
        maskedId: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(
                IllegalStateException("Please sign in first")
            )

        if (eventId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Invalid event ID")
            )
        }

        return try {
            val eventRef = db.collection("events").document(eventId)
            val bookingRef = eventRef.collection("bookings").document(uid)

            db.runTransaction { transaction ->
                // Read all documents before writing.
                val eventDoc = transaction.get(eventRef)
                val bookingDoc = transaction.get(bookingRef)

                check(eventDoc.exists()) {
                    "Event not found"
                }

                check(!bookingDoc.exists()) {
                    "You have already booked this event"
                }

                check(eventDoc.getString("status") == "approved") {
                    "Event is not available for booking"
                }

                val capacity = eventDoc.getLong("capacity")
                    ?: error("Missing event capacity")

                val bookedCount = eventDoc.getLong("bookedCount")
                    ?: error("Missing booking count")

                check(capacity > 0 && bookedCount >= 0 &&
                        bookedCount < capacity) {
                    "This event is fully booked"
                }

                val cutoff = eventDoc.getTimestamp("bookingCutoff")
                    ?: error("Missing booking cutoff")

                val now = System.currentTimeMillis()

                check(now < cutoff.toDate().time) {
                    "Booking has closed"
                }

                val start = eventDoc.getTimestamp("startTime")
                    ?: error("Missing event start time")

                val end = eventDoc.getTimestamp("endTime")
                    ?: error("Missing event end time")

                val bookingData = mapOf(
                    "userId" to uid,
                    "studentName" to studentName,
                    "maskedId" to maskedId,
                    "eventTitle" to (eventDoc.getString("title") ?: ""),
                    "eventStart" to start,
                    "eventEnd" to end,
                    "status" to "confirmed",
                    "bookedAt" to FieldValue.serverTimestamp(),
                    "cancelledAt" to null,
                    "slotHashes" to emptyList<String>()
                )

                // Both writes are part of one atomic transaction.
                transaction.set(bookingRef, bookingData)
                transaction.update(
                    eventRef,
                    "bookedCount",
                    bookedCount + 1
                )
            }.await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
