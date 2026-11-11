package com.groupb.scpd.domain.checkin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CheckInWindowTest {

    private val eventStart = Instant.parse("2026-11-20T18:00:00Z")

    @Test
    fun `scan exactly at event start is allowed`() {
        assertTrue(CheckInWindow.isOpen(eventStart, now = eventStart))
    }

    @Test
    fun `scan 10 minutes before start is allowed`() {
        val now = eventStart.minusSeconds(10 * 60)
        assertTrue(CheckInWindow.isOpen(eventStart, now))
    }

    @Test
    fun `scan 11 minutes before start is rejected`() {
        val now = eventStart.minusSeconds(11 * 60)
        assertFalse(CheckInWindow.isOpen(eventStart, now))
    }

    @Test
    fun `scan 30 minutes after start is allowed`() {
        val now = eventStart.plusSeconds(30 * 60)
        assertTrue(CheckInWindow.isOpen(eventStart, now))
    }

    @Test
    fun `scan 31 minutes after start is rejected`() {
        val now = eventStart.plusSeconds(31 * 60)
        assertFalse(CheckInWindow.isOpen(eventStart, now))
    }
}