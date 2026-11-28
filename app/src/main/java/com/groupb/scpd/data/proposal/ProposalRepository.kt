package com.groupb.scpd.data.proposal

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.groupb.scpd.domain.proposal.ProposalDraft
import kotlinx.coroutines.tasks.await
import java.io.IOException

/** Who a proposal belongs to. Saved once, on its first save. */
data class ProposalOwner(val clubId: String, val clubName: String, val managerUid: String)

/** Saves and loads club proposals (C1). An interface, so ViewModel tests can use a fake. */
interface ProposalRepository {
    /** A new proposal id, made on the phone, so a draft can be saved even offline. */
    fun newProposalId(): String

    /** The club's name, stored on every proposal (the Security Rules check it matches). */
    suspend fun clubName(clubId: String): String

    /** Saves the form as a draft. Works offline: Firestore keeps it on the phone and uploads it later. */
    fun saveDraft(id: String, draft: ProposalDraft, owner: ProposalOwner, firstSave: Boolean)

    /** Sends the proposal for approval. Needs internet: throws IOException when offline. */
    suspend fun submit(id: String, draft: ProposalDraft, owner: ProposalOwner, firstSave: Boolean)

    /** Loads a saved proposal back into the form, or null if it was never saved. */
    suspend fun load(id: String): ProposalDraft?
}

class FirestoreProposalRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ProposalRepository {

    private val events get() = db.collection("events")

    override fun newProposalId(): String = events.document().id   // no internet needed

    override suspend fun clubName(clubId: String): String = firestoreCall {
        db.collection("clubs").document(clubId).get().await().getString("name")
            ?: throw IllegalStateException("Club $clubId has no name")
    }

    /** The fields to write. Creation fields are only sent the first time, because the rules lock them. */
    private fun fieldsToWrite(
        draft: ProposalDraft, owner: ProposalOwner, firstSave: Boolean, status: String,
    ): MutableMap<String, Any?> {
        val data = ProposalMapper.formFields(draft).toMutableMap()
        data["status"] = status
        if (firstSave) {
            data += ProposalMapper.creationFields(owner.clubId, owner.clubName, owner.managerUid)
            data["createdAt"] = FieldValue.serverTimestamp()
        }
        return data
    }

    override fun saveDraft(id: String, draft: ProposalDraft, owner: ProposalOwner, firstSave: Boolean) {
        // Not awaited on purpose: Firestore saves it on the phone straight away
        // and uploads it when there is a connection.
        events.document(id).set(fieldsToWrite(draft, owner, firstSave, "draft"), SetOptions.merge())
    }

    override suspend fun submit(
        id: String, draft: ProposalDraft, owner: ProposalOwner, firstSave: Boolean,
    ) = firestoreCall {
        // Check we are online first. A server read fails at once without internet,
        // so the proposal is never queued and sent later without the manager knowing.
        db.collection("clubs").document(owner.clubId).get(Source.SERVER).await()
        val data = fieldsToWrite(draft, owner, firstSave, "pending")
        data["submittedAt"] = FieldValue.serverTimestamp()
        events.document(id).set(data, SetOptions.merge()).await()
        Unit
    }

    override suspend fun load(id: String): ProposalDraft? = firestoreCall {
        val snapshot = events.document(id).get().await()   // uses the phone's copy when offline
        if (!snapshot.exists()) return@firestoreCall null
        // Firestore gives back Timestamp; the mapper expects Date.
        val data = snapshot.data.orEmpty().mapValues { (_, value) ->
            if (value is Timestamp) value.toDate() else value
        }
        ProposalMapper.toDraft(data)
    }

    /** Turns Firebase errors into the ones the ViewModel understands. */
    private suspend fun <T> firestoreCall(block: suspend () -> T): T = try {
        block()
    } catch (e: FirebaseNetworkException) {
        throw IOException("No connection", e)
    } catch (e: FirebaseFirestoreException) {
        when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> throw IOException("No connection", e)
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> throw SecurityException("Not allowed", e)
            else -> throw e
        }
    }
}