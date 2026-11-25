package com.groupb.scpd.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.groupb.scpd.domain.checkin.ScanResult

@Composable
fun ScanScreen(
    eventId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ScanViewModel = viewModel(
        key = "scan_$eventId",
        factory = ScanViewModel.provideFactory(eventId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScanContent(
        uiState = uiState,
        onCodeInputChanged = viewModel::onCodeInputChanged,
        onVerifyClick = viewModel::verifyPastedCode,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun ScanContent(
    uiState: ScanUiState,
    onCodeInputChanged: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Scan Attendance",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = uiState.eventTitle.ifBlank { uiState.eventId },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }

        OutlinedTextField(
            value = uiState.pastedCodeInput,
            onValueChange = onCodeInputChanged,
            label = { Text("Paste student pass code") },
            placeholder = { Text("eventId|uid|slot|code") },
            enabled = !uiState.isVerifying,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pasteCodeInput")
        )

        Button(
            onClick = onVerifyClick,
            enabled = uiState.pastedCodeInput.isNotBlank() && !uiState.isVerifying,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("verifyCodeButton")
        ) {
            if (uiState.isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Verify & Check In")
            }
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        uiState.lastResult?.let { result ->
            ScanResultBanner(result = result)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Saved offline on this device (${uiState.recentScans.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (uiState.recentScans.isEmpty()) {
            Text(
                text = "No students checked in yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.recentScans, key = { "${it.eventId}_${it.userId}" }) { record ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = record.studentName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "ID: ${record.maskedId}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = record.syncState,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanResultBanner(result: ScanResult) {
    val (containerColor, title, details) = when (result) {
        is ScanResult.Valid -> Triple(
            Color(0xFFDFF5E1),
            "Check-in Recorded (Offline)",
            "${result.studentName} (${result.maskedId})"
        )
        is ScanResult.AlreadyRecorded -> Triple(
            Color(0xFFFFF4D6),
            "Already Checked In",
            "${result.studentName} (${result.maskedId}) was already scanned."
        )
        ScanResult.InvalidFormat -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Invalid Code Format",
            "The pasted text is not a valid student pass code."
        )
        ScanResult.WrongEvent -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Wrong Event",
            "This pass belongs to a different event."
        )
        ScanResult.OutsideWindow -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Check-in Window Closed",
            "Scanning is only allowed from 10 minutes before start until 30 minutes after."
        )
        is ScanResult.NotFound -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Booking Not Found",
            "No confirmed booking found for this student in the downloaded roster."
        )
        ScanResult.Expired -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Code Expired",
            "This 20-second pass code has expired. Ask the student to show the current code."
        )
        ScanResult.InvalidHash -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Invalid Pass Code",
            "The pass code could not be verified against the student's booking."
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanResultBanner")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = details, style = MaterialTheme.typography.bodyMedium)
        }
    }
}