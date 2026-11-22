package com.groupb.scpd.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.groupb.scpd.ScpdApp
import com.groupb.scpd.data.scanner.ScannerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class PrepareScannerUiState(
    val eventIdInput: String = "",
    val cachedCount: Int = 0,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class PrepareScannerViewModel(
    private val repository: ScannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrepareScannerUiState())
    val uiState: StateFlow<PrepareScannerUiState> = _uiState.asStateFlow()

    private var observeCountJob: Job? = null

    fun onEventIdChanged(newEventId: String) {
        _uiState.update {
            it.copy(
                eventIdInput = newEventId,
                statusMessage = null,
                errorMessage = null
            )
        }

        observeCountJob?.cancel()
        val trimmed = newEventId.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(cachedCount = 0) }
            return
        }

        observeCountJob = viewModelScope.launch {
            repository.observeCachedBookingCount(trimmed).collect { count ->
                _uiState.update { it.copy(cachedCount = count) }
            }
        }
    }

    fun prepareScanner() {
        val eventId = _uiState.value.eventIdInput.trim()
        if (eventId.isEmpty()) {
            _uiState.update { it.copy(errorMessage = MSG_EMPTY_EVENT_ID) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, statusMessage = null, errorMessage = null)
            }
            try {
                val count = repository.downloadAndCacheBookings(eventId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cachedCount = count,
                        statusMessage = "Ready for offline scanning: $count confirmed booking(s) cached."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: SecurityException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = MSG_PERMISSION_DENIED) }
            } catch (_: IOException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = MSG_OFFLINE) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = MSG_GENERIC_ERROR) }
            }
        }
    }

    companion object {
        const val MSG_EMPTY_EVENT_ID = "Please enter an Event ID first."
        const val MSG_PERMISSION_DENIED = "You're not on this event's team."
        const val MSG_OFFLINE = "No internet connection."
        const val MSG_GENERIC_ERROR = "Could not download bookings. Please try again."

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScpdApp
                PrepareScannerViewModel(app.container.scannerRepository)
            }
        }
    }
}