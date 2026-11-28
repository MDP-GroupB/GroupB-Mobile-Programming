package com.groupb.scpd.domain.proposal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProposalRulesTest {

    private val now = 1_800_000_000_000L          // a fixed "now", so tests never depend on the real clock
    private val hour = 3_600_000L

    /** Makes a text with exactly n words. */
    private fun words(n: Int) = List(n) { "word" }.joinToString(" ")

    // A proposal with every field filled in correctly (the Robotics Club test event).
    private val complete = ProposalDraft(
        title = "Intro to Robotics Workshop",
        eventType = EventType.ON_CAMPUS,
        eventScale = EventScale.MINOR,
        targetAttendees = 30,
        pricePerStudent = 0.0,
        startMillis = now + 50 * hour,        // 25 Nov 2026, 6:00 pm (Malaysia time)
        endMillis = now + 52 * hour,          // 25 Nov 2026, 8:00 pm
        venue = "BB80",
        backupVenue1 = "TCR 1 ",
        backupVenue2 = "F1A03",
        hasSafas = false,
        safasReason = "Small on-campus workshop for club members only; no SAFAS needed.",
        objectives = "Teach beginners to build and program a simple line-following robot.",
        description = "A hands-on workshop where students assemble a small robot and program it to follow a line.",
        requestedPoints = 2,
    )

    private fun problem(d: ProposalDraft) = ProposalRules.problemBeforeSubmit(d, now)

    @Test fun `complete proposal can be submitted`() = assertNull(problem(complete))

    @Test fun `missing title is reported`() =
        assertEquals(ProposalProblem.MISSING_TITLE, problem(complete.copy(title = "  ")))

    @Test fun `event scale must be chosen`() =
        assertEquals(ProposalProblem.MISSING_EVENT_SCALE, problem(complete.copy(eventScale = null)))

    @Test fun `end before start is refused`() = assertEquals(
        ProposalProblem.END_BEFORE_START,
        problem(complete.copy(endMillis = complete.startMillis)),
    )

    @Test fun `two backup venues are required`() =
        assertEquals(ProposalProblem.MISSING_BACKUP_VENUES, problem(complete.copy(backupVenue2 = "")))

    @Test fun `no SAFAS needs a reason`() = assertEquals(
        ProposalProblem.MISSING_SAFAS_REASON,
        problem(complete.copy(safasReason = "")),
    )

    @Test fun `with SAFAS no reason is needed`() =
        assertNull(problem(complete.copy(hasSafas = true, safasReason = "")))

    @Test fun `objectives must be under 20 words`() {
        assertNull(problem(complete.copy(objectives = words(19))))
        assertEquals(ProposalProblem.OBJECTIVES_TOO_LONG, problem(complete.copy(objectives = words(20))))
    }

    @Test fun `description must be under 100 words`() {
        assertNull(problem(complete.copy(description = words(99))))
        assertEquals(ProposalProblem.DESCRIPTION_TOO_LONG, problem(complete.copy(description = words(100))))
    }

    @Test fun `word count ignores extra spaces`() =
        assertEquals(3, ProposalRules.wordCount("  one   two\nthree  "))

    @Test fun `event times are worked out from the start`() {
        val start = now + 48 * hour
        val times = ProposalRules.eventTimes(start)
        assertEquals(start - 15 * 60_000L, times.bookingCutoffMillis)
        assertEquals(start - 10 * 60_000L, times.checkinOpensMillis)
        assertEquals(start + 30 * 60_000L, times.checkinClosesMillis)
    }
    @Test fun `each problem knows its page`() =
        assertEquals(FormSection.SAFAS, ProposalProblem.MISSING_SAFAS_REASON.section)

}