package com.groupb.scpd.data.proposal

import com.groupb.scpd.domain.proposal.EventScale
import com.groupb.scpd.domain.proposal.EventType
import com.groupb.scpd.domain.proposal.ProposalDraft
import com.groupb.scpd.domain.proposal.ProposalRules
import java.util.Date

/**
 * Converts between the proposal form and the fields saved on events/{id} (see the Firestore schema).
 * Plain Kotlin with no Firebase classes, so it can be unit-tested.
 * Times are saved as Date, which Firestore stores as a timestamp.
 */
object ProposalMapper {

    /** The form's own fields. Saved on every draft save and on submit. Empty answers are saved as null. */
    fun formFields(d: ProposalDraft): Map<String, Any?> {
        val times = d.startMillis?.let { ProposalRules.eventTimes(it) }
        return mapOf(
            "title" to d.title.trim(),
            "eventType" to d.eventType?.code,
            "eventScale" to d.eventScale?.code,
            "capacity" to d.targetAttendees,           // target attendees = booking capacity
            "pricePerStudent" to d.pricePerStudent,
            "startTime" to d.startMillis?.let { Date(it) },
            "endTime" to d.endMillis?.let { Date(it) },
            "bookingCutoff" to times?.let { Date(it.bookingCutoffMillis) },
            "checkinOpens" to times?.let { Date(it.checkinOpensMillis) },
            "checkinCloses" to times?.let { Date(it.checkinClosesMillis) },
            "venue" to d.venue.trim(),
            "backupVenues" to listOf(d.backupVenue1.trim(), d.backupVenue2.trim()),
            "hasSafas" to d.hasSafas,
            "safasReason" to d.safasReason.trim(),
            "objectives" to d.objectives.trim(),
            "description" to d.description.trim(),
            "requestedPoints" to d.requestedPoints,
        )
    }

    /** Fields set only once, on the first save. The repository adds createdAt (server time). */
    fun creationFields(clubId: String, clubName: String, managerUid: String): Map<String, Any?> = mapOf(
        "clubId" to clubId,
        "clubName" to clubName,
        "createdBy" to managerUid,
        "bookedCount" to 0,
    )

    /** Turns a saved event back into the form, to continue a draft or fix a rejected proposal. */
    fun toDraft(data: Map<String, Any?>): ProposalDraft {
        val backups = (data["backupVenues"] as? List<*>)?.map { it as? String ?: "" }.orEmpty()
        return ProposalDraft(
            title = data["title"] as? String ?: "",
            eventType = EventType.entries.find { it.code == data["eventType"] },
            eventScale = EventScale.entries.find { it.code == data["eventScale"] },
            targetAttendees = (data["capacity"] as? Number)?.toInt(),
            pricePerStudent = (data["pricePerStudent"] as? Number)?.toDouble(),
            startMillis = (data["startTime"] as? Date)?.time,
            endMillis = (data["endTime"] as? Date)?.time,
            venue = data["venue"] as? String ?: "",
            backupVenue1 = backups.getOrElse(0) { "" },
            backupVenue2 = backups.getOrElse(1) { "" },
            hasSafas = data["hasSafas"] as? Boolean,
            safasReason = data["safasReason"] as? String ?: "",
            objectives = data["objectives"] as? String ?: "",
            description = data["description"] as? String ?: "",
            requestedPoints = (data["requestedPoints"] as? Number)?.toInt(),
        )
    }
}