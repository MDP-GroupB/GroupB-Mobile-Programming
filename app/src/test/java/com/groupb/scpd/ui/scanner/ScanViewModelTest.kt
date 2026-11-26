package com.groupb.scpd.ui.scanner

import com.groupb.scpd.data.local.CachedBookingEntity
import com.groupb.scpd.data.local.CachedEventEntity
import com.groupb.scpd.data.local.PendingAttendanceEntity
import com.groupb.scpd.data.scanner.ScannerRepository
import com.groupb.scpd.domain.checkin.CheckInWindow
import com.groupb.scpd.domain.checkin.ScanResult
import com.groupb.scpd.domain.pass.CodeGenerator
import com.groupb.scpd.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeScanRepository : ScannerRepository {
    var cachedEvent: CachedEventEntity? = null
    val bookings = mutableListOf<CachedBookingEntity>()
    val savedAttendance = MutableStateFlow<List<PendingAttendanceEntity>>(emptyList())

    override suspend fun downloadAndCacheBookings(eventId: String): Int = bookings.size
    override fun observeCachedBookingCount(eventId: String): Flow<Int> = MutableStateFlow(bookings.size)
    override suspend fun getCachedEvent(eventId: String): CachedEventEntity? = cachedEvent
    override suspend fun getCachedBookings(eventId: String): List<CachedBookingEntity> = bookings
    override suspend fun getRecordedUserIds(eventId: String): Set<String> =
        savedAttendance.value.map { it.userId }.toSet()

    override suspend fun savePendingAttendance(record: PendingAttendanceEntity): Boolean {
        if (savedAttendance.value.any { it.eventId == record.eventId && it.userId == record.userId }) {
            return false
        }
        savedAttendance.value = savedAttendance.value + record
        return true
    }

    override fun observePendingAttendance(eventId: String): Flow<List<PendingAttendanceEntity>> = savedAttendance
    override fun currentScannerUid(): String = "committee_uid_1"
}

class ScanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val eventId = "evt_golden_1"
    private val studentUid = "stu_001"
    private val secretSeed = "test_seed_123"
    private val eventStartMillis = 1_700_000_600_000L
    private val checkinOpensMillis = CheckInWindow.opensAtMillis(eventStartMillis)

    private fun setupRepo(): FakeScanRepository {
        val hashes = CodeGenerator.generateSlotHashes(secretSeed, eventId, studentUid)
        return FakeScanRepository().apply {
            cachedEvent = CachedEventEntity(
                eventId = eventId,
                title = "AI Workshop",
                startTimeMillis = eventStartMillis,
                endTimeMillis = eventStartMillis + 3_600_000L,
                points = 2,
                updatedAtMillis = checkinOpensMillis
            )
            bookings += CachedBookingEntity.fromFirestoreBooking(
                eventId = eventId,
                userId = studentUid,
                studentName = "Ali Ahmad",
                maskedId = "202***89",
                status = "confirmed",
                slotHashes = hashes
            )
        }
    }

    @Test
    fun `valid pasted code shows student name and saves attendance`() = runTest {
        val repo = setupRepo()
        val scanTime = checkinOpensMillis + 5_000L // slot 0
        val vm = ScanViewModel(eventId, repo) { scanTime }

        val payload = CodeGenerator.buildQrPayload(secretSeed, eventId, studentUid, slot = 0)
        vm.onCodeInputChanged(payload)
        vm.verifyPastedCode()

        val result = vm.uiState.value.lastResult
        assertTrue(result is ScanResult.Valid)
        result as ScanResult.Valid
        assertEquals("Ali Ahmad", result.studentName)
        assertEquals("202***89", result.maskedId)
        assertEquals(1, repo.savedAttendance.value.size)
        assertEquals("", vm.uiState.value.pastedCodeInput)
    }

    @Test
    fun `scanning same student twice returns AlreadyRecorded without duplicate row`() = runTest {
        val repo = setupRepo()
        val scanTime = checkinOpensMillis + 5_000L
        val vm = ScanViewModel(eventId, repo) { scanTime }

        val payload = CodeGenerator.buildQrPayload(secretSeed, eventId, studentUid, slot = 0)
        vm.onCodeInputChanged(payload)
        vm.verifyPastedCode()

        vm.onCodeInputChanged(payload)
        vm.verifyPastedCode()

        val result = vm.uiState.value.lastResult
        assertTrue(result is ScanResult.AlreadyRecorded)
        assertEquals(1, repo.savedAttendance.value.size)
    }

    @Test
    fun `wrong event payload returns WrongEvent and does not save`() = runTest {
        val repo = setupRepo()
        val scanTime = checkinOpensMillis + 5_000L
        val vm = ScanViewModel(eventId, repo) { scanTime }

        val payload = CodeGenerator.buildQrPayload(secretSeed, "other_event", studentUid, slot = 0)
        vm.onCodeInputChanged(payload)
        vm.verifyPastedCode()

        assertEquals(ScanResult.WrongEvent, vm.uiState.value.lastResult)
        assertEquals(0, repo.savedAttendance.value.size)
    }
}