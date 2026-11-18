package com.groupb.scpd.domain.pass

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay

/**
 * Convert the string payload into an ImageBitmap that is usable by Compose.
 */
fun generateQrBitmap(content: String, width: Int = 512, height: Int = 512): ImageBitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Integrate 20-second automatic refresh and dynamic slot calculation
 */
@Composable
fun PassScreen(
    modifier: Modifier = Modifier,
    eventId: String = "evt_default_01",
    uid: String = "user_kaiqi_001",     //Temporary placeholder fake data
    secretSeed: String = "groupb_secure_secret"
) {
    var timeLeft by remember { mutableStateOf(20) }
    var currentPayload by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // The QR code refreshes every 20 seconds
    LaunchedEffect(Unit) {
        while (true) {
            // Calculate the slot corresponding to the current time
            val currentEpochSeconds = System.currentTimeMillis() / 1000
            val currentSlot = ((currentEpochSeconds / 20) % 120).toInt()

            // Call CodeGenerator
            val payload = CodeGenerator.buildQrPayload(
                eventId = eventId,
                uid = uid,
                slot = currentSlot,
                secretSeed = secretSeed
            )
            currentPayload = payload
            qrBitmap = generateQrBitmap(payload)

            // 20-second countdown loop
            timeLeft = 20
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "s-CPD Student Pass",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // QR code display area
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!,
                    contentDescription = "Pass QR Code",
                    modifier = Modifier.size(260.dp)
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(60.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Auto-refreshes in: ${timeLeft}s",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Current Payload (Debug):", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = currentPayload.ifEmpty { "Generating..." }, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}