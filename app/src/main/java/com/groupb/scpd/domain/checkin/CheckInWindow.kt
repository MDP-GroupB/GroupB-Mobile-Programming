package com.groupb.scpd.domain.checkin


/**
 * The rule for when scanning is allowed, relative to an event's start time.
 * the function takes an event’s start time and the current time and answers: is scanning allowed rn?
 */

// opens 10 minutes before the event starts, closes 30 minutes after (could be modified later)

//In milliseconds
object CheckInWindow {
    const val OPENS_BEFORE: Long = 10 * 60 * 1000L  // 600,000 millis = 10 min
    const val CLOSES_AFTER: Long = 30 * 60 * 1000L  // 1,800,000 millis = 30 min

    /** Timestamp (epoch millis) when check-in opens (T - 10 min). */
    fun opensAtMillis(eventStart: Long): Long = eventStart - OPENS_BEFORE
    /** Timestamp (epoch millis) when check-in closes (T + 30 min). */
    fun closesAtMillis(eventStart: Long): Long = eventStart + CLOSES_AFTER

    /**
     * @param eventStart the event's official start time
     * @param now the time the scan happened
     * @return true if [now] falls inside the allowed window for [eventStart]
     */

    // The eventStart time is provided by the event data (set when the event is created)

    fun isOpen(eventStart: Long, now: Long): Boolean {
        val opensAt = opensAtMillis(eventStart)
        val closesAt = closesAtMillis(eventStart)
        return now in opensAt until closesAt
    }
}