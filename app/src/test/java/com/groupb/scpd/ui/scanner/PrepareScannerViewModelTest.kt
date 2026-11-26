package com.groupb.scpd.ui.scanner

import com.groupb.scpd.data.local.CachedBookingEntity
import com.groupb.scpd.data.local.CachedEventEntity
import com.groupb.scpd.data.local.PendingAttendanceEntity
import com.groupb.scpd.data.scanner.ScannerRepository
import com.groupb.scpd.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

private class FakeScannerRepository : ScannerRepository {
    var shouldFail = false
    var bookingsToReturn = 5
    private val counts = mutableMapOf<String, MutableStateFlow<Int>>()

    override suspend fun downloadAndCacheBookings(eventId: String): Int {
        if (shouldFail) throw IOException("No network connection")
        counts.getOrPut(eventId) { MutableStateFlow(0) }.value = bookingsToReturn
        return bookingsToReturn
    }

    override fun observeCachedBookingCount(eventId: String): Flow<Int> {
        return counts.getOrPut(eventId) { MutableStateFlow(0) }
    }

    override suspend fun getCachedEvent(eventId: String): CachedEventEntity? = null
    override suspend fun getCachedBookings(eventId: String): List<CachedBookingEntity> = emptyList()
    override suspend fun getRecordedUserIds(eventId: String): Set<String> = emptySet()
    override suspend fun savePendingAttendance(record: PendingAttendanceEntity): Boolean = true
    override fun observePendingAttendance(eventId: String): Flow<List<PendingAttendanceEntity>> =
        MutableStateFlow(emptyList())
    override fun currentScannerUid(): String = ""
}

class PrepareScannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `prepareScanner downloads bookings and updates cachedCount`() = runTest {
        val fakeRepo = FakeScannerRepository().apply { bookingsToReturn = 12 }
        val viewModel = PrepareScannerViewModel(fakeRepo)

        viewModel.onEventIdChanged("evt_001")
        viewModel.prepareScanner()

        val state = viewModel.uiState.value
        assertEquals(12, state.cachedCount)
        assertNull(state.errorMessage)
        assertNotNull(state.statusMessage)
    }

    @Test
    fun `prepareScanner shows offline message when repository throws IOException`() = runTest {
        val fakeRepo = FakeScannerRepository().apply { shouldFail = true }
        val viewModel = PrepareScannerViewModel(fakeRepo)

        viewModel.onEventIdChanged("evt_001")
        viewModel.prepareScanner()

        val state = viewModel.uiState.value
        assertEquals(PrepareScannerViewModel.MSG_OFFLINE, state.errorMessage)
        assertNull(state.statusMessage)
    }

    @Test
    fun `prepareScanner rejects blank eventId`() = runTest {
        val viewModel = PrepareScannerViewModel(FakeScannerRepository())

        viewModel.onEventIdChanged("   ")
        viewModel.prepareScanner()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }
}