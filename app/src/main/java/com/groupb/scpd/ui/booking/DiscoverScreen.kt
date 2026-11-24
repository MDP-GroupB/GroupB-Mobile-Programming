
package com.groupb.scpd.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.groupb.scpd.domain.booking.Event
import java.text.DateFormat
import java.util.Date

@Composable
fun DiscoverScreen() {

    var searchQuery by remember { mutableStateOf("") }
    var selectedClub by remember { mutableStateOf("All clubs") }

    // Temporary demo data. Firebase will replace this later.
    val events = remember {
        val now = System.currentTimeMillis()

        listOf(
            Event(
                id = "event1",
                clubId = "cs",
                clubName = "Computer Science Club",
                title = "AI Workshop",
                description = "Introduction to AI",
                venue = "Computer Lab",
                startTimeMillis = now + 86_400_000L,
                endTimeMillis = now + 93_600_000L,
                capacity = 50,
                bookedCount = 20,
                points = 2,
                status = "approved"
            ),
            Event(
                id = "event2",
                clubId = "sports",
                clubName = "Sports Club",
                title = "Basketball Tournament",
                description = "Inter-club basketball event",
                venue = "Sports Complex",
                startTimeMillis = now + 172_800_000L,
                endTimeMillis = now + 180_000_000L,
                capacity = 100,
                bookedCount = 60,
                points = 3,
                status = "approved"
            ),
            Event(
                id = "event3",
                clubId = "cs",
                clubName = "Computer Science Club",
                title = "Python Programming",
                description = "Python basics for beginners",
                venue = "Room B12",
                startTimeMillis = now + 259_200_000L,
                endTimeMillis = now + 266_400_000L,
                capacity = 40,
                bookedCount = 10,
                points = 2,
                status = "approved"
            )
        )
    }

    // Only approved events are visible to students.
    val approvedEvents = events.filter {
        it.status == "approved"
    }

    val clubs = listOf("All clubs") +
            approvedEvents.map { it.clubName }.distinct().sorted()

    // Search by event title or club/organiser.
    val filteredEvents = approvedEvents.filter { event ->
        val matchesSearch =
            event.title.contains(searchQuery, ignoreCase = true) ||
                    event.clubName.contains(searchQuery, ignoreCase = true)

        val matchesClub =
            selectedClub == "All clubs" ||
                    event.clubName == selectedClub

        matchesSearch && matchesClub
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()   // keep content below the status bar (edge-to-edge)
            .padding(16.dp)
    ) {
        Text(
            text = "Discover Events",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Sample events. Live events and booking arrive in the final release.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search events or clubs") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Filter by organiser",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(clubs) { club ->
                FilterChip(
                    selected = selectedClub == club,
                    onClick = { selectedClub = club },
                    label = { Text(club) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredEvents.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events found.\nTry another search or filter.",
                    textAlign = TextAlign.Center
                )
            }

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredEvents, key = { it.id }) { event ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Organiser: ${event.clubName}")
                            Text("Venue: ${event.venue}")

                            val date = DateFormat
                                .getDateTimeInstance(
                                    DateFormat.MEDIUM,
                                    DateFormat.SHORT
                                )
                                .format(Date(event.startTimeMillis))

                            Text("Date: $date")

                            Text(
                                "Places left: " +
                                        "${event.capacity - event.bookedCount}"
                            )

                            Text(
                                text = "Points: ${event.points ?: "TBD"}"
                            )
                        }
                    }
                }
            }
        }
    }
}

