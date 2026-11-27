package com.groupb.scpd.data.booking

interface BookingRepository {

    suspend fun bookEvent(
        eventId: String,
        studentName: String,
        maskedId: String
    ): Result<Unit>

}


