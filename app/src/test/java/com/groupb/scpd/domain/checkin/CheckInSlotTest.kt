package com.groupb.scpd.domain.checkin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInSlotTest {

    private val opensAt = 1_000_000_000_000L
    private fun seconds(sec: Long): Long = sec * 1_000L

    @Test
    fun `slot 0 starts at checkinOpensAt`() {
        assertEquals(0, CheckInSlot.slotIndexFor(opensAt, opensAt))
        assertEquals(0, CheckInSlot.slotIndexFor(opensAt, opensAt + seconds(19)))
    }

    @Test
    fun `slot 1 starts at 20 seconds`() {
        assertEquals(1, CheckInSlot.slotIndexFor(opensAt, opensAt + seconds(20)))
    }

    @Test
    fun `slot 119 is the last valid slot and 2400 seconds returns null`() {
        assertEquals(119, CheckInSlot.slotIndexFor(opensAt, opensAt + seconds(2399)))
        assertNull(CheckInSlot.slotIndexFor(opensAt, opensAt + seconds(2400)))
    }

    @Test
    fun `previous slot is accepted within first 5 seconds of new slot`() {
        val scanTime = opensAt + seconds(24) // 4 seconds into slot 1
        assertTrue(CheckInSlot.isSlotValidAt(opensAt, scanTime, candidateSlot = 0))
        assertTrue(CheckInSlot.isSlotValidAt(opensAt, scanTime, candidateSlot = 1))
    }

    @Test
    fun `previous slot is rejected at 5 seconds into new slot`() {
        val scanTime = opensAt + seconds(25) // 5 seconds into slot 1
        assertFalse(CheckInSlot.isSlotValidAt(opensAt, scanTime, candidateSlot = 0))
    }

    @Test
    fun `future slot is rejected`() {
        val scanTime = opensAt + seconds(19) // Still in slot 0
        assertFalse(CheckInSlot.isSlotValidAt(opensAt, scanTime, candidateSlot = 1))
    }
}