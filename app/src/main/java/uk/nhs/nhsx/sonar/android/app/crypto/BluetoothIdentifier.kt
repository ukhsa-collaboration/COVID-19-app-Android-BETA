/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import timber.log.Timber
import java.lang.IllegalArgumentException

class BluetoothIdentifier(
    val countryCode: ByteArray,
    val cryptogram: Cryptogram,
    val txPower: Byte
) {

    companion object {
        fun fromBytes(bytes: ByteArray): BluetoothIdentifier {
            Timber.d("Bytes is ${bytes.size} - ${bytes.map{it.toInt()}.joinToString(",")}")
            if (bytes.size != SIZE) {
                throw IllegalArgumentException("Identifier must be exactly $SIZE bytes, was given ${bytes.size}")
            }
            return BluetoothIdentifier(
                bytes.sliceArray(0 until 2),
                Cryptogram.fromBytes(bytes.sliceArray(2 until bytes.size - 1)),
                bytes.last()
            )
        }

        private const val countryCodeSize = 2
        private const val txPowerSize = 1
        const val SIZE = countryCodeSize + Cryptogram.SIZE + txPowerSize
    }

    fun asBytes(): ByteArray =
        (countryCode + cryptogram.asBytes() + txPower).clone()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothIdentifier

        if (!countryCode.contentEquals(other.countryCode)) return false
        if (cryptogram != other.cryptogram) return false
        if (txPower != other.txPower) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.contentHashCode()
        result = 31 * result + cryptogram.hashCode()
        result = 31 * result + txPower
        return result
    }
}
