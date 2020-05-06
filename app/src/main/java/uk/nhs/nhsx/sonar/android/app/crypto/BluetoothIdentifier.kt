/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

class BluetoothIdentifier(
    val countryCode: ByteArray,
    val cryptogram: Cryptogram,
    val txPower: Byte,
    val transmissionTime: Int,
    val hmacSignature: ByteArray
) {
    init {
        if (countryCode.size != 2) {
            throw IllegalArgumentException("Country code must be two bytes of ISO 3166-1. Was ${countryCode.size} ")
        }

        if (hmacSignature.size != 16) {
            throw IllegalArgumentException("Signature must be 16 bytes. Was ${hmacSignature.size} ")
        }
    }

    companion object {
        fun fromBytes(bytes: ByteArray): BluetoothIdentifier {
            if (bytes.size != SIZE) {
                throw IllegalArgumentException("Identifier must be exactly $SIZE bytes, was given ${bytes.size}")
            }

            val transmissionTimeOffset = countryCodeSize + Cryptogram.SIZE + txPowerSize
            val signatureOffset = transmissionTimeOffset + transmissionTimeSize
            return BluetoothIdentifier(
                bytes.sliceArray(0 until countryCodeSize),
                Cryptogram.fromBytes(bytes.sliceArray(countryCodeSize until countryCodeSize + Cryptogram.SIZE)),
                bytes[countryCodeSize + Cryptogram.SIZE],
                ByteBuffer.wrap(bytes.sliceArray(transmissionTimeOffset until signatureOffset)).int,
                bytes.sliceArray(signatureOffset until SIZE)
            )
        }

        private const val countryCodeSize = 2
        private const val txPowerSize = 1
        private const val transmissionTimeSize = 4
        private const val signatureSize = 16
        const val SIZE =
            countryCodeSize + Cryptogram.SIZE + txPowerSize + transmissionTimeSize + signatureSize
    }

    fun asBytes(): ByteArray =
        (countryCode + cryptogram.asBytes() + txPower + transmissionTimeBytes() + hmacSignature)

    private fun transmissionTimeBytes() = ByteBuffer.wrap(ByteArray(transmissionTimeSize)).apply {
        putInt(transmissionTime)
    }.array()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothIdentifier

        if (!countryCode.contentEquals(other.countryCode)) return false
        if (cryptogram != other.cryptogram) return false
        if (txPower != other.txPower) return false
        if (transmissionTime != other.transmissionTime) return false
        if (!hmacSignature.contentEquals(other.hmacSignature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.contentHashCode()
        result = 31 * result + cryptogram.hashCode()
        result = 31 * result + txPower
        result = 31 * result + transmissionTime
        result = 31 * result + hmacSignature.contentHashCode()
        return result
    }
}
