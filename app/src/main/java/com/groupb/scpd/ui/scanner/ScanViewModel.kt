package com.groupb.scpd.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.groupb.scpd.ScpdApp
import com.groupb.scpd.data.local.PendingAttendanceEntity
import com.groupb.scpd.data.scanner.ScannerRepository
import com.groupb.scpd.domain.checkin.ScanCodeChecker
import com.groupb.scpd.domain.checkin.ScanResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val eventId: String = "",
    val eventTitle: String = "",
    val pastedCodeInput: String = "",
    val isVerifying: Boolean = false,
    val lastResult: ScanResult? = null,
    val errorMessage: String? = null,
    val recentScans: List<PendingAttendanceEntity> = emptyList()
)

class ScanViewModel(
    private val eventId: String,
    private val repository: ScannerRepository,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState(eventId = eventId))
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cachedEvent = repository.getCachedEvent(eventId)
            _uiState.update {
                it.copy(eventTitle = cachedEvent?.title ?: eventId)
            }
        }
        viewModelScope.launch {
            repository.observePendingAttendance(eventId).collect { records ->
                _uiState.update { it.copy(recentScans = records) }
            }
        }
    }

    fun onCodeInputChanged(newInput: String) {
        _uiState.update {
            it.copy(pastedCodeInput = newInput, errorMessage = null)
        }
    }

    fun verifyPastedCode() {
        val rawCode = _uiState.value.pastedCodeInput.trim()
        if (rawCode.isEmpty()) {
            _uiState.update { it.copy(errorMessage = MSG_EMPTY_CODE) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, errorMessage = null) }
            try {
                val cachedEvent = repository.getCachedEvent(eventId)
                if (cachedEvent == null || cachedEvent.startTimeMillis == 0L) {
                    _uiState.update {
                        it.copy(isVerifying = false, errorMessage = MSG_EVENT_NOT_PREPARED)
                    }
                    return@launch
                }

                val bookingsMap = repository.getCachedBookings(eventId)
                    .associate { it.userId to it.toDomainCheckModel() }
                val alreadyScanned = repository.getRecordedUserIds(eventId)
                val nowMillis = clockMillis()

                val result = ScanCodeChecker.verifyScan(
                    rawQrString = rawCode,
                    activeEventId = eventId,
                    eventStartMillis = cachedEvent.startTimeMillis,
                    scanTimeMillis = nowMillis,
                    bookingsByUserId = bookingsMap,
                    alreadyScannedUserIds = alreadyScanned
                )

                val finalResult = if (result is ScanResult.Valid) {
                    val inserted = repository.savePendingAttendance(
                        PendingAttendanceEntity(
                            eventId = eventId,
                            userId = result.userId,
                            studentName = result.studentName,
                            maskedId = result.maskedId,
                            method = "qr",
                            slot = result.slot,
                            code = result.code,
                            scannedAtMillis = nowMillis,
                            scannedByUid = repository.currentScannerUid(),
                            points = cachedEvent.points,
                            eventEndMillis = cachedEvent.endTimeMillis,
                            syncState = "PENDING"
                        )
                    )
                    if (inserted) {
                        result
                    } else {
                        ScanResult.AlreadyRecorded(result.studentName, result.maskedId)
                    }
                } else {
                    result
                }

                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        lastResult = finalResult,
                        pastedCodeInput = if (finalResult is ScanResult.Valid) "" else it.pastedCodeInput
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isVerifying = false, errorMessage = MSG_SCAN_ERROR)
                }
            }
        }
    }

    companion object {
        const val MSG_EMPTY_CODE = "Please paste a student pass code first."
        const val MSG_EVENT_NOT_PREPARED = "Event details are missing. Go back and download the roster again."
        const val MSG_SCAN_ERROR = "Could not verify code. Please try again."

        fun provideFactory(eventId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScpdApp
                ScanViewModel(
                    eventId = eventId,
                    repository = app.container.scannerRepository
                )
            }
        }
    }
}