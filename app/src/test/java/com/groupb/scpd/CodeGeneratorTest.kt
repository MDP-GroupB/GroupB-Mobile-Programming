package com.example.scpd

import com.example.scpd.utils.CodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CodeGeneratorTest {

    @Test
    fun testGenerateSlotHashesSize() {
        val secretSeed = "my_secure_device_seed_123"
        val eventId = "event_001"
        val uid = "user_abc_123"
        val checkinOpens = System.currentTimeMillis()

        val hashes = CodeGenerator.generateSlotHashes(secretSeed, eventId, uid, checkinOpens)

        // Verify whether exactly 120 slots have been generated for the hash
        assertEquals(120, hashes.size)

        // Verify whether each hash is of the standard length of SHA-256
        for (hash in hashes) {
            assertNotNull(hash)
            assertEquals(64, hash.length)
        }
    }
}