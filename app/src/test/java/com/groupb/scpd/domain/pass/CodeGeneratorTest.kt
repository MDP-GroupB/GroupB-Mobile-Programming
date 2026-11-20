package com.groupb.scpd.domain.pass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CodeGeneratorTest {

    @Test
    fun testGenerateSlotHashesSize() {
        val secretSeed = "my_secure_device_seed_123"
        val eventId = "event_001"
        val uid = "user_abc_123"

        val hashes = CodeGenerator.generateSlotHashes(secretSeed, eventId, uid)

        // Verify whether exactly 120 time slots have been generated and hashed
        assertEquals(120, hashes.size)

        // Verify whether each hash is a 64-bit hexadecimal string
        for (hash in hashes) {
            assertNotNull(hash)
            assertEquals(64, hash.length)
        }
    }

    @Test
    fun testGenerateSlotCodeAndPayload() {
        val secretSeed = "my_secure_device_seed_123"
        val eventId = "event_001"
        val uid = "user_abc_123"
        val slot = 5

        val code = CodeGenerator.generateSlotCode(secretSeed, eventId, uid, slot)
        assertEquals(64, code.length)

        val payload = CodeGenerator.buildQrPayload(secretSeed, eventId, uid, slot)
        assertEquals("$eventId|$uid|$slot|$code", payload)
    }
}