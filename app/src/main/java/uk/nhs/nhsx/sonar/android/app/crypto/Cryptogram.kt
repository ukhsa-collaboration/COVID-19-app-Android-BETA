/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import android.util.Base64

class Cryptogram(
    val publicKeyBytes: ByteArray,
    val encryptedPayload: ByteArray,
    val tag: ByteArray
) {

    companion object {
        fun fromBytes(bytes: ByteArray): Cryptogram =
            Cryptogram(
                bytes.sliceArray(0 until 64),
                bytes.sliceArray(64 until bytes.size - 16),
                bytes.sliceArray(bytes.size - 16 until bytes.size)
            )
    }

    fun asBytes(): ByteArray =
        publicKeyBytes + encryptedPayload + tag

    fun asString(): String =
        Base64.encodeToString(asBytes(), Base64.DEFAULT)
}
