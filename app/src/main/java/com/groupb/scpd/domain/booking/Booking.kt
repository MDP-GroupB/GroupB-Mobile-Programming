package com.groupb.scpd.domain.booking

data class Booking(
    val userId: String = "",
    val studentName: String = "",
    val maskedId: String = "",
    val eventTitle: String = "",
    val eventStart: Long = 0L,
    val eventEnd: Long = 0L,
    val status: String = "confirmed",
    val bookedAt: Long? = null,
    val cancelledAt: Long? = null,
    val slotHashes: List<String> = emptyList()
)
