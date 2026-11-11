package com.groupb.scpd.domain.checkin

import java.time.Duration
import java.time.Instant

/**
 * The rule for when scanning is allowed, relative to an event's start time.
 * the function takes an event’s start time and the current time and answers: is scanning allowed rn?
 */

// opens 10 minutes before the event starts, closes 30 minutes after (could be modified later)

object CheckInWindow {
    private val OPENS_BEFORE: Duration = Duration.ofMinutes(10)
    private val CLOSES_AFTER: Duration = Duration.ofMinutes(30)

    /**
     * @param eventStart the event's official start time
     * @param now the time the scan happened
     * @return true if [now] falls inside the allowed window for [eventStart]
     */

    // The eventStart time is provided by the event data (set when the event is created)

    fun isOpen(eventStart: Instant, now: Instant): Boolean {
        val opensAt = eventStart.minus(OPENS_BEFORE)  // Opening time = event start time - opening window
        val closesAt = eventStart.plus(CLOSES_AFTER) // Closing time = event start time + closing window
        return now >= opensAt && now <= closesAt
    }
}