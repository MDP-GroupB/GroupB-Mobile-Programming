package com.groupb.scpd.ui.proposal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.groupb.scpd.ScpdApp
import com.groupb.scpd.data.proposal.ProposalDraftStore
import com.groupb.scpd.data.proposal.ProposalOwner
import com.groupb.scpd.data.proposal.ProposalRepository
import com.groupb.scpd.domain.proposal.FormSection
import com.groupb.scpd.domain.proposal.ProposalDraft
import com.groupb.scpd.domain.proposal.ProposalProblem
import com.groupb.scpd.domain.proposal.ProposalRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** Everything the proposal form shows. */
data class ProposalFormUiState(
    val proposalId: String = "",
    val draft: ProposalDraft = ProposalDraft(),
    val section: FormSection = FormSection.SUMMARY,   // the page being shown
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val problem: ProposalProblem? = null,              // shown on its page after Submit
    val message: String? = null,                       // e.g. "Draft saved"
    val submitted: Boolean = false,
)

/** The proposal form (C1): one page per EPF section, autosaved on the phone, then saved or submitted. */
class ProposalFormViewModel(
    private val repository: ProposalRepository,
    private val draftStore: ProposalDraftStore,
    private val currentUserId: () -> String?,
    private val now: () -> Long = { System.currentTimeMillis() },   // replaceable, so tests use a fixed time
) : ViewModel() {

    private val _state = MutableStateFlow(ProposalFormUiState())
    val state: StateFlow<ProposalFormUiState> = _state.asStateFlow()

    private var started = false
    private var clubId = ""
    private var firstSave = true   // the first save also records owner and creation time

    /** Opens a new proposal (proposalId = null) or continues an existing one. */
    fun start(clubId: String, proposalId: String?) {
        if (started) return   // already open, e.g. after the screen rotates
        started = true
        this.clubId = clubId
        viewModelScope.launch {
            val id = proposalId ?: repository.newProposalId()
            val savedOnline = if (proposalId == null) null else runCatching { repository.load(id) }.getOrNull()
            firstSave = savedOnline == null
            val savedOnPhone = draftStore.load(id)   // newest answers, typed on this phone
            _state.update {
                it.copy(proposalId = id, draft = savedOnPhone ?: savedOnline ?: ProposalDraft(), loading = false)
            }
        }
    }

    /** Called for every answer the manager types or picks. Also autosaves on the phone (Storage lecture). */
    fun edit(change: (ProposalDraft) -> ProposalDraft) {
        _state.update { it.copy(draft = change(it.draft), problem = null, message = null) }
        val current = _state.value
        if (current.proposalId.isEmpty()) return
        viewModelScope.launch { draftStore.save(current.proposalId, current.draft) }
    }

    fun goTo(section: FormSection) = _state.update { it.copy(section = section) }

    fun next() {
        FormSection.entries.getOrNull(_state.value.section.ordinal + 1)?.let(::goTo)
    }

    fun back() {
        FormSection.entries.getOrNull(_state.value.section.ordinal - 1)?.let(::goTo)
    }

    /** Saves to Firestore as a draft, so the manager can finish it later, even on another phone. */
    fun saveDraft() {
        val current = _state.value
        if (current.loading || current.submitting) return
        viewModelScope.launch {
            try {
                repository.saveDraft(current.proposalId, current.draft, owner(), firstSave)
                firstSave = false
                _state.update { it.copy(message = MSG_DRAFT_SAVED) }
            } catch (e: IOException) {
                _state.update { it.copy(message = MSG_KEPT_ON_PHONE) }
            } catch (e: Exception) {
                _state.update { it.copy(message = MSG_FAILED) }
            }
        }
    }

    /** Checks the answers, then sends the proposal for approval. */
    fun submit() {
        val current = _state.value
        if (current.loading || current.submitting) return   // stops double taps
        val problem = ProposalRules.problemBeforeSubmit(current.draft, now())
        if (problem != null) {
            // Jump to the page with the problem and show why.
            _state.update { it.copy(problem = problem, section = problem.section, message = null) }
            return
        }
        _state.update { it.copy(submitting = true, problem = null, message = null) }
        viewModelScope.launch {
            try {
                repository.submit(current.proposalId, current.draft, owner(), firstSave)
                firstSave = false
                draftStore.clear(current.proposalId)   // sent, so the phone copy is no longer needed
                _state.update { it.copy(submitting = false, submitted = true, message = MSG_SUBMITTED) }
            } catch (e: IOException) {
                _state.update { it.copy(submitting = false, message = MSG_OFFLINE) }
            } catch (e: SecurityException) {
                _state.update { it.copy(submitting = false, message = MSG_NOT_ALLOWED) }
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, message = MSG_FAILED) }
            }
        }
    }

    /** Who the proposal belongs to: this manager and their club. */
    private suspend fun owner(): ProposalOwner {
        val uid = currentUserId() ?: throw SecurityException("Not signed in")
        return ProposalOwner(clubId, repository.clubName(clubId), uid)
    }

    companion object {
        const val MSG_DRAFT_SAVED = "Draft saved. You can come back and finish it later."
        const val MSG_KEPT_ON_PHONE = "No internet. Your answers are kept on this phone; save the draft again when you're online."
        const val MSG_SUBMITTED = "Submitted. Waiting for the s-CPD office to approve."
        const val MSG_OFFLINE = "No internet connection. Your answers are kept; submit again when you're online."
        const val MSG_NOT_ALLOWED = "Only this club's event manager can submit proposals."
        const val MSG_FAILED = "Something went wrong. Please try again."

        /** Builds the ViewModel with the repository and draft store from the AppContainer. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScpdApp
                ProposalFormViewModel(
                    repository = app.container.proposalRepository,
                    draftStore = app.container.proposalDraftStore,
                    currentUserId = app.container.currentUserId,
                )
            }
        }
    }
}