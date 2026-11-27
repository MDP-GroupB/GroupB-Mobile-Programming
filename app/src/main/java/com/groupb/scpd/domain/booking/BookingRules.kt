package com.groupb.scpd.domain.booking

object BookingRules {

    private const val CUTOFF_MILLIS = 15 * 60 * 1000L

    // Check if a new event overlaps with existing booked events.
    fun hasTimeClash(
        newEvent: Event,
        bookedEvents: List<Event>
    ): Boolean {
        return bookedEvents.any { booked ->
            newEvent.startTimeMillis < booked.endTimeMillis &&
                    booked.startTimeMillis < newEvent.endTimeMillis
        }
    }

    // Booking closes 15 minutes before the event starts.
    fun isBookingOpen(
        currentTimeMillis: Long,
        eventStartMillis: Long
    ): Boolean {
        return currentTimeMillis < eventStartMillis - CUTOFF_MILLIS
    }

    // Check whether a new event conflicts with
    // any existing confirmed booking.
    // Cancelled bookings do not block new bookings.
    fun hasConfirmedBookingClash(
        newEvent: Event,
        existingBookings: List<Booking>
    ): Boolean {
        return existingBookings.any { booking ->
            booking.status == "confirmed" &&
                    newEvent.startTimeMillis < booking.eventEnd &&
                    booking.eventStart < newEvent.endTimeMillis
        }
    }
}
