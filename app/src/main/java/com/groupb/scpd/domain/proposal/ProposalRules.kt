package com.groupb.scpd.domain.proposal

import kotlinx.serialization.Serializable

/** EPF 1.0 Event Type: the two allowed answers. `code` is what we save in Firestore; `label` is what the user sees. */
enum class EventType(val code: String, val label: String) {
    ON_CAMPUS("on_campus", "On campus"),
    OFF_CAMPUS("off_campus", "Off campus"),
}

/** EPF 1.0: event scale. `code` is the value stored in Firestore. */
enum class EventScale(val code: String, val label: String) {
    MINOR("minor", "Minor event (open to CS members only)"),
    MAJOR("major", "Major event (open to CS members and non-members)"),
    HIGH_RISK("high_risk", "High risk event (open to CS members, non-members, public)"),
    COLLABORATION("collaboration", "Internal / external collaboration"),
    CSR("csr", "CSR"),
}

/**
 * The core of the Event Proposal Form,
 * plus the s-CPD points the club requests.
 * Times are milliseconds since 1970, the same as Areas B and D use.
 * Empty fields are null, so a draft can be saved half-finished.
 */
@Serializable   // lets ProposalDraftStore save the form as JSON on the phone
data class ProposalDraft(
    // 1.0 Summary of event proposal
    val title: String = "",
    val eventType: EventType? = null,
    val eventScale: EventScale? = null,
    val targetAttendees: Int? = null,      // also the booking capacity in the app
    val pricePerStudent: Double? = null,   // RM, 0 if free
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val venue: String = "",
    val backupVenue1: String = "",
    val backupVenue2: String = "",
    // Event SAFAS
    val hasSafas: Boolean? = null,
    val safasReason: String = "",          // required only when have SAFAS is false
    // Objectives + Description of event
    val objectives: String = "",
    val description: String = "",
    // s-CPD
    val requestedPoints: Int? = null,
)

/** The three times every event stores. The Security Rules check these exact values. */
data class EventTimes(
    val bookingCutoffMillis: Long,   // 15 min before start: last moment to book or cancel
    val checkinOpensMillis: Long,    // 10 min before start
    val checkinClosesMillis: Long,   // 30 min after start
)

/** The pages of the proposal form, one per EPF section. More are added in later steps. */
enum class FormSection(val title: String) {
    SUMMARY("1.0 Summary of event proposal"),
    SAFAS("1.1 Event SAFAS"),
    OBJECTIVES("2.0 Objectives and 2.1 Description"),
}

/** Each thing that can stop a submission: the page it is on, and the message the manager sees (C1). */
enum class ProposalProblem(val section: FormSection, val message: String) {
    MISSING_TITLE(FormSection.SUMMARY, "Enter the event title."),
    TITLE_TOO_LONG(FormSection.SUMMARY, "Keep the title to ${ProposalRules.TITLE_MAX} characters or fewer."),
    MISSING_EVENT_TYPE(FormSection.SUMMARY, "Choose on campus or off campus."),
    MISSING_EVENT_SCALE(FormSection.SUMMARY, "Choose the event scale."),
    BAD_ATTENDEES(FormSection.SUMMARY, "Enter the target number of attendees (at least 1)."),
    BAD_PRICE(FormSection.SUMMARY, "Enter the participation price per student (0 if free)."),
    MISSING_START(FormSection.SUMMARY, "Choose the starting date and time."),
    START_IN_PAST(FormSection.SUMMARY, "The starting time must be in the future."),
    MISSING_END(FormSection.SUMMARY, "Choose the ending date and time."),
    END_BEFORE_START(FormSection.SUMMARY, "The ending time must be after the starting time."),
    MISSING_VENUE(FormSection.SUMMARY, "Enter the venue."),
    MISSING_BACKUP_VENUES(FormSection.SUMMARY, "Enter two backup venues."),
    BAD_POINTS(FormSection.SUMMARY, "Enter the s-CPD points you are requesting (0 or more)."),
    MISSING_SAFAS(FormSection.SAFAS, "Choose whether there will be SAFAS."),
    MISSING_SAFAS_REASON(FormSection.SAFAS, "Explain why there will be no SAFAS."),
    MISSING_OBJECTIVES(FormSection.OBJECTIVES, "Enter the objectives."),
    OBJECTIVES_TOO_LONG(FormSection.OBJECTIVES, "Objectives must be under ${ProposalRules.OBJECTIVES_WORD_LIMIT} words."),
    MISSING_DESCRIPTION(FormSection.OBJECTIVES, "Enter the description."),
    DESCRIPTION_TOO_LONG(FormSection.OBJECTIVES, "Description must be under ${ProposalRules.DESCRIPTION_WORD_LIMIT} words."),
}

object ProposalRules {
    const val TITLE_MAX = 100
    const val OBJECTIVES_WORD_LIMIT = 20      // Objectives: fewer than 20 words
    const val DESCRIPTION_WORD_LIMIT = 100    // Description: fewer than 100 words
    private const val MINUTE = 60_000L

    /** Counts words, ignoring extra spaces and new lines. */
    fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    /** Works out the cut-off and check-in window from the start time. */
    fun eventTimes(startMillis: Long) = EventTimes(
        bookingCutoffMillis = startMillis - 15 * MINUTE,
        checkinOpensMillis = startMillis - 10 * MINUTE,
        checkinClosesMillis = startMillis + 30 * MINUTE,
    )

    /**
     * Checks a proposal before it is submitted.
     * Returns null when it is ready, or the first problem to show the manager.
     * A draft can be saved without passing this check.
     */
    fun problemBeforeSubmit(d: ProposalDraft, nowMillis: Long): ProposalProblem? = when {
        d.title.isBlank() -> ProposalProblem.MISSING_TITLE
        d.title.trim().length > TITLE_MAX -> ProposalProblem.TITLE_TOO_LONG
        d.eventType == null -> ProposalProblem.MISSING_EVENT_TYPE
        d.eventScale == null -> ProposalProblem.MISSING_EVENT_SCALE
        d.targetAttendees == null || d.targetAttendees < 1 -> ProposalProblem.BAD_ATTENDEES
        d.pricePerStudent == null || d.pricePerStudent < 0 -> ProposalProblem.BAD_PRICE
        d.startMillis == null -> ProposalProblem.MISSING_START
        d.startMillis <= nowMillis -> ProposalProblem.START_IN_PAST
        d.endMillis == null -> ProposalProblem.MISSING_END
        d.endMillis <= d.startMillis -> ProposalProblem.END_BEFORE_START
        d.venue.isBlank() -> ProposalProblem.MISSING_VENUE
        d.backupVenue1.isBlank() || d.backupVenue2.isBlank() -> ProposalProblem.MISSING_BACKUP_VENUES
        d.requestedPoints == null || d.requestedPoints < 0 -> ProposalProblem.BAD_POINTS
        d.hasSafas == null -> ProposalProblem.MISSING_SAFAS
        d.hasSafas == false && d.safasReason.isBlank() -> ProposalProblem.MISSING_SAFAS_REASON
        d.objectives.isBlank() -> ProposalProblem.MISSING_OBJECTIVES
        wordCount(d.objectives) >= OBJECTIVES_WORD_LIMIT -> ProposalProblem.OBJECTIVES_TOO_LONG
        d.description.isBlank() -> ProposalProblem.MISSING_DESCRIPTION
        wordCount(d.description) >= DESCRIPTION_WORD_LIMIT -> ProposalProblem.DESCRIPTION_TOO_LONG
        else -> null
    }
}