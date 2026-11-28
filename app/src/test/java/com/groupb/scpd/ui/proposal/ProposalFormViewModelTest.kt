package com.groupb.scpd.ui.proposal

import com.groupb.scpd.data.proposal.ProposalDraftStore
import com.groupb.scpd.domain.proposal.EventScale
import com.groupb.scpd.domain.proposal.EventType
import com.groupb.scpd.domain.proposal.FormSection
import com.groupb.scpd.domain.proposal.ProposalDraft
import com.groupb.scpd.domain.proposal.ProposalProblem
import com.groupb.scpd.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProposalFormViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()   // runs viewModelScope at once (Testing lecture)
    @get:Rule val tempFolder = TemporaryFolder()             // a fresh folder for the phone copy

    private val fixedNow = 1_800_000_000_000L
    private val hour = 3_600_000L
    private val repo = FakeProposalRepository()
    private lateinit var store: ProposalDraftStore

    @Before fun setUp() {
        store = ProposalDraftStore(tempFolder.root, mainDispatcherRule.dispatcher)
    }

    private fun newViewModel() = ProposalFormViewModel(
        repository = repo,
        draftStore = store,
        currentUserId = { "manager-uid" },
        now = { fixedNow },
    )

    // The Robotics Club test event, complete.
    private val complete = ProposalDraft(
        title = "Intro to Robotics Workshop",
        eventType = EventType.ON_CAMPUS,
        eventScale = EventScale.MINOR,
        targetAttendees = 30,
        pricePerStudent = 0.0,
        startMillis = fixedNow + 50 * hour,
        endMillis = fixedNow + 52 * hour,
        venue = "BB80",
        backupVenue1 = "TCR1",
        backupVenue2 = "F1A03",
        hasSafas = false,
        safasReason = "Small on-campus workshop for club members only.",
        objectives = "Teach beginners to build and program a simple line-following robot.",
        description = "A hands-on workshop where students assemble a small robot.",
        requestedPoints = 2,
    )

    @Test fun `a new proposal starts empty on the first page`() = runTest {
        val vm = newViewModel()
        vm.start("robotics-club", null)
        val state = vm.state.value
        assertEquals("proposal1", state.proposalId)
        assertEquals(ProposalDraft(), state.draft)
        assertEquals(FormSection.SUMMARY, state.section)
        assertFalse(state.loading)
    }

    @Test fun `every change is autosaved on the phone`() = runTest {
        val vm = newViewModel()
        vm.start("robotics-club", null)
        vm.edit { it.copy(title = "Intro to Robotics Workshop") }
        assertEquals("Intro to Robotics Workshop", store.load("proposal1")?.title)
    }

    @Test fun `reopening the form brings the answers back`() = runTest {
        val first = newViewModel()
        first.start("robotics-club", null)
        first.edit { it.copy(venue = "BB80") }

        val reopened = newViewModel()
        reopened.start("robotics-club", "proposal1")
        assertEquals("BB80", reopened.state.value.draft.venue)
    }

    @Test fun `next and back move between pages`() = runTest {
        val vm = newViewModel()
        vm.start("robotics-club", null)
        vm.next()
        assertEquals(FormSection.SAFAS, vm.state.value.section)
        vm.back()
        assertEquals(FormSection.SUMMARY, vm.state.value.section)
    }

    @Test fun `a missing answer jumps to its page and nothing is sent`() = runTest {
        val vm = newViewModel()
        vm.start("robotics-club", null)
        vm.edit { complete.copy(hasSafas = null) }
        vm.submit()
        assertEquals(ProposalProblem.MISSING_SAFAS, vm.state.value.problem)
        assertEquals(FormSection.SAFAS, vm.state.value.section)
        assertTrue(repo.submitted.isEmpty())
    }

    @Test fun `a complete proposal is submitted and the phone copy is cleared`() = runTest {
        val vm = newViewModel()
        vm.start("robotics-club", null)
        vm.edit { complete }
        vm.submit()
        assertTrue(vm.state.value.submitted)
        assertEquals(listOf("proposal1"), repo.submitted)
        assertNull(store.load("proposal1"))
    }

    @Test fun `submitting offline keeps the answers and says so`() = runTest {
        repo.offline = true
        val vm = newViewModel()
        vm.start("robotics-club", null)
        vm.edit { complete }
        vm.submit()
        assertEquals(ProposalFormViewModel.MSG_OFFLINE, vm.state.value.message)
        assertFalse(vm.state.value.submitted)
        assertEquals(complete, store.load("proposal1"))
    }

    @Test fun `only the first save records the owner`() = runTest {
        val vm = newViewModel()
        vm.start("robotics-club", null)
        vm.saveDraft()
        vm.saveDraft()
        assertEquals(listOf(true, false), repo.firstSaves)
    }
}