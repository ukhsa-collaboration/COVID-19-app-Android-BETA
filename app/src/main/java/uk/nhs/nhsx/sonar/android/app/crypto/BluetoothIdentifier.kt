/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import android.util.Base64
import java.lang.IllegalArgumentException

class BluetoothIdentifier(
    val countryCode: ByteArray,
    val cryptogram: Cryptogram
) {
    companion object {
        fun fromBytes(bytes: ByteArray): BluetoothIdentifier {
            if (bytes.size != SIZE) {
                throw IllegalArgumentException("Identifier must be exactly $SIZE bytes, was given ${bytes.size}")
            }
            return BluetoothIdentifier(
                bytes.sliceArray(0 until 2),
                Cryptogram.fromBytes(bytes.sliceArray(2 until bytes.size))
            )
        }

        const val SIZE = 2 + Cryptogram.SIZE
    }

    fun asBytes(): ByteArray =
        countryCode + cryptogram.asBytes()

    fun asString(): String =
        Base64.encodeToString(asBytes(), Base64.DEFAULT)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothIdentifier

        if (!countryCode.contentEquals(other.countryCode)) return false
        if (cryptogram != other.cryptogram) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.contentHashCode()
        result = 31 * result + cryptogram.hashCode()
        return result
    }
}
