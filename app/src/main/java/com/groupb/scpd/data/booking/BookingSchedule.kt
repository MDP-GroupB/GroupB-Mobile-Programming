package com.groupb.scpd.data.booking

// One confirmed event in a student's booking schedule.
data class ScheduledEvent(
    val eventId: String = "",
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L
)

// Shared schedule for a single student.
// Future booking transactions will read and update
// this document to coordinate concurrent requests.
data class BookingSchedule(
    val userId: String = "",
    val confirmedEvents: List<ScheduledEvent> = emptyList()
)


