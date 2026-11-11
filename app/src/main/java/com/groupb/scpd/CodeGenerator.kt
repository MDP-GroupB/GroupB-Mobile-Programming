package com.example.scpd.utils

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CodeGenerator {

    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val SLOT_DURATION_SECONDS = 20L
    private const val TOTAL_SLOTS = 120

    /**
     * Generate a hash array of 120 time slots
     * @param secretSeed The secret seed saved locally on the mobile device
     * @param eventId The event ID
     * @param uid The student user ID
     * @param checkinOpensTimestamp The timestamp of sign-in opening
     */
    fun generateSlotHashes(
        secretSeed: String,
        eventId: String,
        uid: String,
        checkinOpensTimestamp: Long
    ): List<String> {
        val slotHashes = mutableListOf<String>()

        for (slot in 0 until TOTAL_SLOTS) {
            // 1. Calculate the dynamic code for each slot: HMAC-SHA256
            val message = "$eventId|$uid|$slot"
            val code = hmacSha256(secretSeed, message)

            // 2. Perform a SHA-256 hash on the code and store the result in the slotHashes array
            val hash = sha256(code)
            slotHashes.add(hash)
        }

        return slotHashes
    }

    /** HMAC-SHA256 */
    fun hmacSha256(key: String, message: String): String {
        val sha256Hmac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM)
        sha256Hmac.init(secretKey)
        val bytes = sha256Hmac.doFinal(message.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Calculate the standard SHA-256 and convert it into a 64-bit lowercase hexadecimal string */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.byteInputStream().readBytes())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}