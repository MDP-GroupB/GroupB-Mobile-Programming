package com.groupb.scpd.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScannerDaoTest {

    private lateinit var db: ScannerDatabase
    private lateinit var dao: ScannerDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ScannerDatabase::class.java
        ).build()
        dao = db.scannerDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun replaceBookingsForEvent_replacesOldRosterAndUpdatesCount() = runTest {
        val initial = listOf(
            CachedBookingEntity("evt_1", "u1", "Ali", "203***12", "confirmed", "h0,h1"),
            CachedBookingEntity("evt_1", "u2", "Sara", "204***88", "confirmed", "h0,h1")
        )
        dao.replaceBookingsForEvent("evt_1", initial)
        assertEquals(2, dao.observeBookingCountForEvent("evt_1").first())

        val updated = listOf(
            CachedBookingEntity("evt_1", "u3", "Omar", "205***44", "confirmed", "h0,h1")
        )
        dao.replaceBookingsForEvent("evt_1", updated)

        val rows = dao.getBookingsForEvent("evt_1")
        assertEquals(1, rows.size)
        assertEquals("u3", rows.first().userId)
    }

    @Test
    fun insertPendingAttendance_ignoresDuplicateForSameEventAndUser() = runTest {
        val firstScan = PendingAttendanceEntity(
            eventId = "evt_1",
            userId = "u1",
            studentName = "Ali",
            maskedId = "203***12",
            method = "qr",
            slot = 3,
            code = "a".repeat(64),
            scannedAtMillis = 1_000L,
            scannedByUid = "committee_1",
            points = 10,
            eventEndMillis = 5_000L
        )
        val duplicateScan = firstScan.copy(scannedAtMillis = 2_000L)

        val firstRowId = dao.insertPendingAttendance(firstScan)
        val secondRowId = dao.insertPendingAttendance(duplicateScan)

        assertNotEquals(-1L, firstRowId)
        assertEquals(-1L, secondRowId)
        assertEquals(listOf("u1"), dao.getRecordedUserIdsForEvent("evt_1"))
    }
}