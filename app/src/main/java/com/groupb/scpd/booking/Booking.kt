package com.groupb.scpd.booking

data class Booking(
    val id: String = "",
    val studentId: String = "",
    val eventId: String = "",
    val status: String = "confirmed",
    val slotHashes: List<String> = emptyList()
)
