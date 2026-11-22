package com.groupb.scpd.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PrepareScannerScreen(
    modifier: Modifier = Modifier,
    onStartScanning: (String) -> Unit = {}
) {
    val viewModel: PrepareScannerViewModel = viewModel(factory = PrepareScannerViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PrepareScannerContent(
        uiState = uiState,
        onEventIdChanged = viewModel::onEventIdChanged,
        onPrepareClick = viewModel::prepareScanner,
        onStartScanning = { onStartScanning(uiState.eventIdInput.trim()) },
        modifier = modifier
    )
}

@Composable
fun PrepareScannerContent(
    uiState: PrepareScannerUiState,
    onEventIdChanged: (String) -> Unit,
    onPrepareClick: () -> Unit,
    onStartScanning: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Prepare Committee Scanner",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Download confirmed student bookings before check-in opens so QR verification works offline.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = uiState.eventIdInput,
            onValueChange = onEventIdChanged,
            label = { Text("Event ID") },
            singleLine = true,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("eventIdInput")
        )

        Button(
            onClick = onPrepareClick,
            enabled = uiState.eventIdInput.isNotBlank() && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prepareScannerButton")
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Download Roster for Offline Scanning")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Offline Roster Status",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Cached bookings in Room: ${uiState.cachedCount}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (uiState.statusMessage != null) {
            Text(
                text = uiState.statusMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (uiState.cachedCount > 0 && uiState.eventIdInput.isNotBlank()) {
            Button(
                onClick = onStartScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Proceed to Scanner")
            }
        }
    }
}