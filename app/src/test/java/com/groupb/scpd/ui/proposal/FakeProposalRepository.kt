package com.groupb.scpd.ui.proposal

import com.groupb.scpd.data.proposal.ProposalOwner
import com.groupb.scpd.data.proposal.ProposalRepository
import com.groupb.scpd.domain.proposal.ProposalDraft
import java.io.IOException

/** In-memory ProposalRepository for tests: no Firebase, no internet. */
class FakeProposalRepository : ProposalRepository {
    var offline = false                                   // set true to pretend there is no internet
    val saved = mutableMapOf<String, ProposalDraft>()     // what was saved, by id
    val firstSaves = mutableListOf<Boolean>()             // the firstSave flag of each save
    val submitted = mutableListOf<String>()               // ids that were submitted
    private var nextId = 0

    override fun newProposalId() = "proposal${++nextId}"

    override suspend fun clubName(clubId: String): String {
        if (offline) throw IOException("No connection")
        return "Robotics Club"
    }

    override fun saveDraft(id: String, draft: ProposalDraft, owner: ProposalOwner, firstSave: Boolean) {
        saved[id] = draft
        firstSaves += firstSave
    }

    override suspend fun submit(id: String, draft: ProposalDraft, owner: ProposalOwner, firstSave: Boolean) {
        if (offline) throw IOException("No connection")
        saved[id] = draft
        firstSaves += firstSave
        submitted += id
    }

    override suspend fun load(id: String): ProposalDraft? = saved[id]
}