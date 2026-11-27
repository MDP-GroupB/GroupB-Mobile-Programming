
package com.groupb.scpd.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groupb.scpd.domain.booking.Event
import java.text.DateFormat
import java.util.Date

@Composable
fun EventDetailsScreen(
    event: Event,
    onBack: () -> Unit,
    onBook: () -> Unit,
    bookingReady: Boolean = false
) {
    val placesLeft = (event.capacity - event.bookedCount)
        .coerceAtLeast(0)

    val dateFormatter = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT
    )

    val startDate = dateFormatter.format(Date(event.startTimeMillis))
    val endDate = dateFormatter.format(Date(event.endTimeMillis))

    val bookingOpen = event.status == "approved" &&
            placesLeft > 0 &&
            System.currentTimeMillis() <
            event.startTimeMillis - 15 * 60_000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to events")
        }

        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineMedium
        )

        HorizontalDivider()

        Text(
            text = "Organiser: ${event.clubName}"
        )

        Text(
            text = "Description",
            style = MaterialTheme.typography.titleMedium
        )
        Text(event.description.ifBlank { "No description provided." })

        Text("Venue: ${event.venue}")
        Text("Start: $startDate")
        Text("End: $endDate")

        Text(
            text = "Places left: $placesLeft / ${event.capacity}"
        )

        Text("Points: ${event.points ?: "TBD"}")

        Text(
            text = "Status: ${event.status}"
        )

        HorizontalDivider()

        if (!bookingOpen) {
            Text(
                text = "Booking is unavailable for this event.",
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onBook,
            enabled = bookingReady && bookingOpen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Book this event")
        }

        if (!bookingReady) {
            Text(
                text = "Booking will be enabled after Firebase integration.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
