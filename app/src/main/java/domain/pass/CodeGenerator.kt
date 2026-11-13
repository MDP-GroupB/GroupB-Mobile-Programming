package com.groupb.scpd.domain.pass

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CodeGenerator {

    private const val TOTAL_SLOTS = 120

    /**Generate dynamic verification codes for individual time slots*/
    fun generateSlotCode(
        secretSeed: String,
        eventId: String,
        uid: String,
        slot: Int
    ): String {
        val message = "$eventId|$uid|$slot"
        return hmacSha256(secretSeed, message)
    }

    /**Generate a hash array consisting of 120 time slots*/
    fun generateSlotHashes(
        secretSeed: String,
        eventId: String,
        uid: String
    ): List<String> {
        val hashes = ArrayList<String>(TOTAL_SLOTS)
        for (slot in 0 until TOTAL_SLOTS) {
            val dynamicCode = generateSlotCode(secretSeed, eventId, uid, slot)
            val storedHash = sha256(dynamicCode)
            hashes.add(storedHash)
        }
        return hashes
    }

    /**QR code scanning and decoding*/
    fun buildQrPayload(
        secretSeed: String,
        eventId: String,
        uid: String,
        slot: Int
    ): String {
        val code = generateSlotCode(secretSeed, eventId, uid, slot)
        return "$eventId|$uid|$slot|$code"
    }

    private fun hmacSha256(key: String, data: String): String {
        val algorithm = "HmacSHA256"
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secretKeySpec)
        val rawHmac = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return rawHmac.joinToString("") { "%02x".format(it) }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}