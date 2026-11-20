package com.groupb.scpd.ui.scanner

import com.groupb.scpd.data.scanner.ScannerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

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
    fun `prepareScanner shows error when repository fails`() = runTest {
        val fakeRepo = FakeScannerRepository().apply { shouldFail = true }
        val viewModel = PrepareScannerViewModel(fakeRepo)

        viewModel.onEventIdChanged("evt_001")
        viewModel.prepareScanner()

        val state = viewModel.uiState.value
        assertEquals("No network connection", state.errorMessage)
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