package com.groupb.scpd.domain.checkin

/**
 * Turns a scan time into "which one of the 120 twenty-second slots is this?"
 *
 * Slot 0 starts exactly when check-in opens (10 minutes before the event).
 * Slot 119 is the last slot before check-in closes (30 minutes after start).
 */
object CheckInSlot {
    const val SLOT_DURATION_SECONDS = 20L
    const val SLOT_DURATION_MILLIS = SLOT_DURATION_SECONDS * 1000L
    const val GRACE_PERIOD_SECONDS = 5L
    const val GRACE_PERIOD_MILLIS = GRACE_PERIOD_SECONDS * 1000L
    const val TOTAL_SLOTS = 120

    /**
     * @param checkinOpensAt the moment slot 0 begins (epoch millis)
     * @param now the time the scan happened (epoch millis)
     * @return the slot number (0..119) [now] falls into, or null if [now] is
     *         before check-in opens or after it closes
     */
    fun slotIndexFor(checkinOpensAt: Long, now: Long): Int? {
        val elapsedMillis = now - checkinOpensAt  // check how much time passed in millis
        if (elapsedMillis < 0L) return null  // return null if elapsed is -ve which means scan happened before checkin time
        val slot = (elapsedMillis / SLOT_DURATION_MILLIS).toInt()  // how we calculate slots. We divide the time elapsed by the duration of one slot.
        return if (slot in 0 until TOTAL_SLOTS) slot else null // If the slot number is between 0 and 119, return the slot number else return null.
    }

    /**
     * True if [candidateSlot] matches the current slot at [now],
     * OR matches the previous slot (currentSlot - 1) during the first 5 seconds of the new slot.
     */
    fun isSlotValidAt(
        checkinOpensAt: Long,
        now: Long,
        candidateSlot: Int
    ): Boolean {
        if (candidateSlot !in 0 until TOTAL_SLOTS) return false // if the slot num not in range return false
        val currentSlot = slotIndexFor(checkinOpensAt, now) ?: return false // if check-in hasnt started or has closed, this function rejects the candidate slot

        //above func basically means:
        // val currentSlot = slotIndexFor(checkinOpensAt, now)
        //
        //if (currentSlot == null) {
        //    return false
        //}

        if (candidateSlot == currentSlot) return true  // if the users slot matches our current slot then its accepted
        val elapsedMillis = now - checkinOpensAt
        // Calculate how far into the current slot we are
        val millisIntoCurrentSlot = elapsedMillis % SLOT_DURATION_MILLIS
        // checks 3 conditions to return true
        return millisIntoCurrentSlot < GRACE_PERIOD_MILLIS && candidateSlot == currentSlot - 1 && candidateSlot >= 0
    }
}