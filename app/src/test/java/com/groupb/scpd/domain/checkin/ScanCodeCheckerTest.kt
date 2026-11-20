
package com.groupb.scpd.domain.checkin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ScanCodeCheckerTest {

    private val eventId = "event_001"
    private val uid = "student_abc_123"
    private val secretSeed = "my_secure_device_seed_123"
    private val eventStartMillis = 1_700_000_000_000L
    private val checkinOpensMillis = CheckInWindow.opensAtMillis(eventStartMillis)

    private fun generateHmacCode(slot: Int): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretSeed.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal("$eventId|$uid|$slot".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private val slotHashes: List<String> = (0 until CheckInSlot.TOTAL_SLOTS).map { slot ->
        ScanCodeChecker.sha256Hex(generateHmacCode(slot))
    }

    private val bookingsMap = mapOf(
        uid to CachedBookingForCheck(
            eventId = eventId,
            userId = uid,
            studentName = "Ali Ahmad",
            maskedId = "203***12",
            slotHashes = slotHashes
        )
    )

    @Test
    fun `valid code in current slot returns Valid`() {
        val code = generateHmacCode(0)
        val qr = "$eventId|$uid|0|$code"
        val scanTime = checkinOpensMillis + 10_000L

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = emptySet()
        )

        assertTrue(result is ScanResult.Valid)
        assertEquals("Ali Ahmad", (result as ScanResult.Valid).studentName)
    }

    @Test
    fun `previous slot within 5s grace returns Valid`() {
        val code0 = generateHmacCode(0)
        val qr = "$eventId|$uid|0|$code0"
        val scanTime = checkinOpensMillis + 24_000L // 4s into slot 1

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = emptySet()
        )

        assertTrue(result is ScanResult.Valid)
    }

    @Test
    fun `previous slot at 5s into next slot returns Expired`() {
        val code0 = generateHmacCode(0)
        val qr = "$eventId|$uid|0|$code0"
        val scanTime = checkinOpensMillis + 25_000L // 5s into slot 1

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = emptySet()
        )

        assertEquals(ScanResult.Expired, result)
    }

    @Test
    fun `future slot returns Expired`() {
        val code1 = generateHmacCode(1)
        val qr = "$eventId|$uid|1|$code1"
        val scanTime = checkinOpensMillis + 10_000L // Still in slot 0

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = emptySet()
        )

        assertEquals(ScanResult.Expired, result)
    }

    @Test
    fun `malformed QR string returns InvalidFormat`() {
        val scanTime = checkinOpensMillis + 5_000L

        val result = ScanCodeChecker.verifyScan(
            rawQrString = "not-a-valid-qr-code",
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = emptySet()
        )

        assertEquals(ScanResult.InvalidFormat, result)
    }

    @Test
    fun `QR code for different event returns WrongEvent`() {
        val code = generateHmacCode(0)
        val qr = "other_event_999|$uid|0|$code"
        val scanTime = checkinOpensMillis + 5_000L

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = emptySet()
        )

        assertEquals(ScanResult.WrongEvent, result)
    }

    @Test
    fun `scan before check-in opens or after check-in closes returns OutsideWindow`() {
        val code = generateHmacCode(0)
        val qr = "$eventId|$uid|0|$code"
        val beforeOpen = checkinOpensMillis - 1L

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = beforeOpen,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = emptySet()
        )

        assertEquals(ScanResult.OutsideWindow, result)
    }

    @Test
    fun `tampered code returns InvalidHash`() {
        val wrongCode = "f".repeat(64)
        val qr = "$eventId|$uid|0|$wrongCode"
        val scanTime = checkinOpensMillis + 5_000L

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = emptySet()
        )

        assertEquals(ScanResult.InvalidHash, result)
    }

    @Test
    fun `forged code for already-scanned user still returns InvalidHash`() {
        val wrongCode = "f".repeat(64)
        val qr = "$eventId|$uid|0|$wrongCode"
        val scanTime = checkinOpensMillis + 5_000L

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = setOf(uid)
        )

        assertEquals(ScanResult.InvalidHash, result)
    }

    @Test
    fun `unbooked student returns NotFound`() {
        val code = generateHmacCode(0)
        val qr = "$eventId|unknown_user|0|$code"
        val scanTime = checkinOpensMillis + 5_000L

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = emptySet()
        )

        assertEquals(ScanResult.NotFound("unknown_user"), result)
    }

    @Test
    fun `valid code for already-scanned student returns AlreadyRecorded with student info`() {
        val code = generateHmacCode(0)
        val qr = "$eventId|$uid|0|$code"
        val scanTime = checkinOpensMillis + 5_000L

        val result = ScanCodeChecker.verifyScan(
            rawQrString = qr,
            activeEventId = eventId,
            eventStartMillis = eventStartMillis,
            scanTimeMillis = scanTime,
            bookingsByUserId = bookingsMap,
            alreadyScannedUserIds = setOf(uid)
        )

        assertEquals(ScanResult.AlreadyRecorded("Ali Ahmad", "203***12"), result)
    }
}