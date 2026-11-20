package com.groupb.scpd.ui.pass

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.groupb.scpd.domain.pass.CodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Integrate 20-second automatic refresh aligned to slot boundaries and dynamic slot calculation.
 */
@Composable
fun PassScreen(
    modifier: Modifier = Modifier,
    eventId: String = "evt_default_01",
    uid: String = "user_kaiqi_001", // Temporary placeholder fake data
    secretSeed: String = "groupb_secure_secret",
    checkinOpensMillis: Long = System.currentTimeMillis() - 10_000L
) {
    var timeLeft by remember { mutableIntStateOf(20) }
    var currentPayload by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(eventId, uid, secretSeed, checkinOpensMillis) {
        while (true) {
            val nowMillis = System.currentTimeMillis()
            val elapsedMillis = (nowMillis - checkinOpensMillis).coerceAtLeast(0L)
            val currentSlot = (elapsedMillis / 20_000L).toInt()
            val elapsedSeconds = elapsedMillis / 1000L
            val remainingInSlot = (20L - (elapsedSeconds % 20L)).toInt().coerceIn(1, 20)

            val payload = CodeGenerator.buildQrPayload(
                eventId = eventId,
                uid = uid,
                slot = currentSlot,
                secretSeed = secretSeed
            )
            val bitmap = withContext(Dispatchers.Default) {
                generateQrBitmap(payload)
            }

            currentPayload = payload
            qrBitmap = bitmap
            timeLeft = remainingInSlot

            val afterWorkMillis = System.currentTimeMillis()
            val millisUntilNextSecond = 1000L - (afterWorkMillis % 1000L)
            delay(millisUntilNextSecond.coerceIn(50L, 1000L))
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
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
                    Text(
                        text = "Current Payload (Debug):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentPayload.ifEmpty { "Generating..." },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}