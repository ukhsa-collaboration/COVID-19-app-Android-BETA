/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import java.lang.IllegalArgumentException

class Cryptogram(
    val publicKeyBytes: ByteArray,
    val encryptedPayload: ByteArray,
    val tag: ByteArray
) {

    init {
        if (publicKeyBytes.size != 64) {
            throw IllegalArgumentException("Public key needs to be exactly 64 bytes (X6.92 encoded uncompressed points, was given ${publicKeyBytes.size} bytes")
        }

        if (encryptedPayload.size != 26) {
            throw IllegalArgumentException("Payload needs to be exactly 26 bytes, was given ${encryptedPayload.size} bytes")
        }

        if (tag.size != 16) {
            throw IllegalArgumentException("Tag needs to be exactly 16 bytes, was given ${tag.size} bytes")
        }
    }

    companion object {
        fun fromBytes(bytes: ByteArray): Cryptogram {
            if (bytes.size != SIZE) {
                throw IllegalArgumentException("Cryptogram needs to be exactly $SIZE bytes, was given ${bytes.size}")
            }
            val publicKey = bytes.sliceArray(0 until 64)
            val encryptedPayload = bytes.sliceArray(64 until bytes.size - 16)
            val tag = bytes.sliceArray(bytes.size - 16 until bytes.size)
            return Cryptogram(
                publicKey,
                encryptedPayload,
                tag
            )
        }

        const val SIZE = 106
    }

    fun asBytes(): ByteArray =
        publicKeyBytes + encryptedPayload + tag

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cryptogram

        if (!publicKeyBytes.contentEquals(other.publicKeyBytes)) return false
        if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false
        if (!tag.contentEquals(other.tag)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKeyBytes.contentHashCode()
        result = 31 * result + encryptedPayload.contentHashCode()
        result = 31 * result + tag.contentHashCode()
        return result
    }
}
