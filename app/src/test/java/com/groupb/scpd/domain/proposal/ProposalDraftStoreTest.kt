package com.groupb.scpd.data.proposal

import com.groupb.scpd.domain.proposal.EventScale
import com.groupb.scpd.domain.proposal.EventType
import com.groupb.scpd.domain.proposal.ProposalDraft
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProposalDraftStoreTest {

    // A fresh empty folder for each test, deleted afterwards.
    @get:Rule val tempFolder = TemporaryFolder()

    private val draft = ProposalDraft(
        title = "Intro to Robotics Workshop",
        eventType = EventType.ON_CAMPUS,
        eventScale = EventScale.MINOR,
        targetAttendees = 30,
        venue = "BB80",
        hasSafas = false,
        safasReason = "Small on-campus workshop for club members only.",
    )

    @Test fun `saved draft can be read back`() = runTest {
        val store = ProposalDraftStore(tempFolder.root, StandardTestDispatcher(testScheduler))
        store.save("event1", draft)
        assertEquals(draft, store.load("event1"))
    }

    @Test fun `no saved draft returns null`() = runTest {
        val store = ProposalDraftStore(tempFolder.root, StandardTestDispatcher(testScheduler))
        assertNull(store.load("missing"))
    }

    @Test fun `saving again replaces the old draft`() = runTest {
        val store = ProposalDraftStore(tempFolder.root, StandardTestDispatcher(testScheduler))
        store.save("event1", draft)
        store.save("event1", draft.copy(title = "Robotics Workshop 2"))
        assertEquals("Robotics Workshop 2", store.load("event1")?.title)
    }

    @Test fun `clear deletes the draft`() = runTest {
        val store = ProposalDraftStore(tempFolder.root, StandardTestDispatcher(testScheduler))
        store.save("event1", draft)
        store.clear("event1")
        assertNull(store.load("event1"))
    }

    @Test fun `savedIds lists unfinished drafts`() = runTest {
        val store = ProposalDraftStore(tempFolder.root, StandardTestDispatcher(testScheduler))
        store.save("event1", draft)
        store.save("event2", draft)
        assertEquals(setOf("event1", "event2"), store.savedIds().toSet())
    }

    @Test fun `a damaged file returns null instead of crashing`() = runTest {
        val store = ProposalDraftStore(tempFolder.root, StandardTestDispatcher(testScheduler))
        File(tempFolder.root, "proposal_bad.json").writeText("{ not json")
        assertNull(store.load("bad"))
    }
}