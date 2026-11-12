
package com.groupb.scpd.booking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingRulesTest {

    private fun minutes(value: Long): Long {
        return value * 60_000L
    }

    private fun event(
        id: String,
        start: Long,
        end: Long
    ): Event {
        return Event(
            id = id,
            startTimeMillis = minutes(start),
            endTimeMillis = minutes(end)
        )
    }

    @Test
    fun overlappingEventsShouldClash() {
        val existing = event("1", 840, 960)
        val newEvent = event("2", 900, 1020)

        assertTrue(
            BookingRules.hasTimeClash(
                newEvent,
                listOf(existing)
            )
        )
    }

    @Test
    fun nonOverlappingEventsShouldNotClash() {
        val existing = event("1", 840, 960)
        val newEvent = event("2", 960, 1080)

        assertFalse(
            BookingRules.hasTimeClash(
                newEvent,
                listOf(existing)
            )
        )
    }

    @Test
    fun bookingBeforeCutoffShouldBeAllowed() {
        assertTrue(
            BookingRules.isBookingOpen(
                minutes(884),
                minutes(900)
            )
        )
    }

    @Test
    fun bookingAtCutoffShouldBeRejected() {
        assertFalse(
            BookingRules.isBookingOpen(
                minutes(885),
                minutes(900)
            )
        )
    }

    @Test
    fun bookingAfterCutoffShouldBeRejected() {
        assertFalse(
            BookingRules.isBookingOpen(
                minutes(890),
                minutes(900)
            )
        )
    }
}
