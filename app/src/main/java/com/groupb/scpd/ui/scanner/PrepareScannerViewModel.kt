package com.groupb.scpd.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.groupb.scpd.data.scanner.ScannerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            _uiState.update { it.copy(errorMessage = "Please enter an Event ID first.") }
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to download bookings."
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: ScannerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PrepareScannerViewModel(repository) as T
                }
            }
    }
}