package com.groupb.scpd.data.proposal

import com.groupb.scpd.domain.proposal.EventScale
import com.groupb.scpd.domain.proposal.EventType
import com.groupb.scpd.domain.proposal.ProposalDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class ProposalMapperTest {

    private val start = 1_800_180_000_000L          // a fixed start time
    private val minute = 60_000L

    private val draft = ProposalDraft(
        title = "Intro to Robotics Workshop",
        eventType = EventType.ON_CAMPUS,
        eventScale = EventScale.MINOR,
        targetAttendees = 30,
        pricePerStudent = 0.0,
        startMillis = start,
        endMillis = start + 120 * minute,
        venue = "BB80",
        backupVenue1 = "TCR1",
        backupVenue2 = "F1A03",
        hasSafas = false,
        safasReason = "Small on-campus workshop for club members only.",
        objectives = "Teach beginners to build a simple robot.",
        description = "A hands-on robotics workshop.",
        requestedPoints = 2,
    )

    @Test fun `choices are saved as lowercase codes`() {
        val fields = ProposalMapper.formFields(draft)
        assertEquals("on_campus", fields["eventType"])
        assertEquals("minor", fields["eventScale"])
    }

    @Test fun `target attendees is saved as capacity`() =
        assertEquals(30, ProposalMapper.formFields(draft)["capacity"])

    @Test fun `cut-off and check-in times are worked out`() {
        val fields = ProposalMapper.formFields(draft)
        assertEquals(Date(start - 15 * minute), fields["bookingCutoff"])
        assertEquals(Date(start - 10 * minute), fields["checkinOpens"])
        assertEquals(Date(start + 30 * minute), fields["checkinCloses"])
    }

    @Test fun `an empty draft saves nulls instead of crashing`() {
        val fields = ProposalMapper.formFields(ProposalDraft())
        assertNull(fields["startTime"])
        assertNull(fields["bookingCutoff"])
        assertNull(fields["eventScale"])
    }

    @Test fun `saved fields turn back into the same form`() =
        assertEquals(draft, ProposalMapper.toDraft(ProposalMapper.formFields(draft)))

    @Test fun `creation fields start with no bookings`() =
        assertEquals(0, ProposalMapper.creationFields("robotics-club", "Robotics Club", "uid1")["bookedCount"])
}