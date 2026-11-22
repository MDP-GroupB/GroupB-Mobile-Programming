package com.groupb.scpd.domain.checkin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInWindowTest {

    private fun minutes(value: Long): Long = value * 60_000L

    private val eventStart = minutes(1000)

    @Test
    fun `scan exactly at event start is allowed`() {
        assertTrue(CheckInWindow.isOpen(eventStart, now = eventStart))
    }

    @Test
    fun `scan 10 minutes before start is allowed`() {
        val now = eventStart - minutes(10)
        assertTrue(CheckInWindow.isOpen(eventStart, now))
    }

    @Test
    fun `scan 11 minutes before start is rejected`() {
        val now = eventStart - minutes(11)
        assertFalse(CheckInWindow.isOpen(eventStart, now))
    }

    @Test
    fun `scan just before 30 minutes after start is allowed`() {
        val now = eventStart + minutes(30) - 1L
        assertTrue(CheckInWindow.isOpen(eventStart, now))
    }

    @Test
    fun `scan exactly 30 minutes after start is rejected`() {
        val now = eventStart + minutes(30)
        assertFalse(CheckInWindow.isOpen(eventStart, now))
    }

    @Test
    fun `scan 31 minutes after start is rejected`() {
        val now = eventStart + minutes(31)
        assertFalse(CheckInWindow.isOpen(eventStart, now))
    }
}