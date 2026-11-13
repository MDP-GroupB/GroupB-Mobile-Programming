package com.groupb.scpd.domain.booking

object BookingRules {

    private const val CUTOFF_MILLIS = 15 * 60 * 1000L

    // Check if the new event overlaps with existing bookings
    fun hasTimeClash(
        newEvent: Event,
        bookedEvents: List<Event>
    ): Boolean {
        return bookedEvents.any { booked ->
            newEvent.startTimeMillis < booked.endTimeMillis &&
                    booked.startTimeMillis < newEvent.endTimeMillis
        }
    }

    // Booking closes 15 minutes before the event starts
    fun isBookingOpen(
        currentTimeMillis: Long,
        eventStartMillis: Long
    ): Boolean {
        return currentTimeMillis < eventStartMillis - CUTOFF_MILLIS
    }
}

