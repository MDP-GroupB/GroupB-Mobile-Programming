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
        val elapsedMillis = now - checkinOpensAt
        if (elapsedMillis < 0L) return null
        val slot = (elapsedMillis / SLOT_DURATION_MILLIS).toInt()
        return if (slot in 0 until TOTAL_SLOTS) slot else null
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
        if (candidateSlot !in 0 until TOTAL_SLOTS) return false
        val currentSlot = slotIndexFor(checkinOpensAt, now) ?: return false

        if (candidateSlot == currentSlot) return true

        val elapsedMillis = now - checkinOpensAt
        val millisIntoCurrentSlot = elapsedMillis % SLOT_DURATION_MILLIS
        return millisIntoCurrentSlot < GRACE_PERIOD_MILLIS &&
                candidateSlot == currentSlot - 1 &&
                candidateSlot >= 0
    }
}