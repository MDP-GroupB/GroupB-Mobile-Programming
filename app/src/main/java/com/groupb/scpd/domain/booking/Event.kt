
package com.groupb.scpd.domain.booking

data class Event(
    val id: String = "",
    val clubId: String = "",
    val clubName: String = "",
    val title: String = "",
    val description: String = "",
    val venue: String = "",
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val capacity: Int = 0,
    val bookedCount: Int = 0,
    val points: Int? = null,
    val status: String = ""
)
