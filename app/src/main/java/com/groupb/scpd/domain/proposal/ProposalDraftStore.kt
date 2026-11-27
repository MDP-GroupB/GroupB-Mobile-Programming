package com.groupb.scpd.data.proposal

import com.groupb.scpd.domain.proposal.ProposalDraft
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Autosaves an unfinished proposal form on the phone (Storage lecture: DraftStore),
 * so nothing is lost if the user goes offline or the app is closed while typing.
 * One small JSON file per proposal, in the app's private files folder.
 */
class ProposalDraftStore(
    private val folder: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,   // injected, so tests control threading
) {
    // ignoreUnknownKeys: a draft saved by an older version of the app still loads.
    private val json = Json { ignoreUnknownKeys = true }

    private fun fileFor(id: String) = File(folder, "proposal_$id.json")

    /** Saves the form. Writes a temporary file first, then swaps it in, so a crash never leaves half a file. */
    suspend fun save(id: String, draft: ProposalDraft) = withContext(io) {
        folder.mkdirs()
        val temp = File(folder, "proposal_$id.json.tmp")
        temp.writeText(json.encodeToString(ProposalDraft.serializer(), draft))
        Files.move(
            temp.toPath(), fileFor(id).toPath(),
            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE,
        )
    }

    /** Reads the saved form, or null if there is none (or the file is damaged). */
    suspend fun load(id: String): ProposalDraft? = withContext(io) {
        val file = fileFor(id)
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString(ProposalDraft.serializer(), file.readText()) }.getOrNull()
    }

    /** Deletes the saved form, e.g. after it is submitted. */
    suspend fun clear(id: String) = withContext(io) {
        fileFor(id).delete()
        Unit
    }

    /** The ids of every unfinished form on this phone, so the app can offer "Continue". */
    suspend fun savedIds(): List<String> = withContext(io) {
        folder.listFiles().orEmpty()
            .map { it.name }
            .filter { it.startsWith("proposal_") && it.endsWith(".json") }
            .map { it.removePrefix("proposal_").removeSuffix(".json") }
    }
}