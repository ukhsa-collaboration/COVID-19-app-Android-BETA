/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.crypto

class Cryptogram(
    val publicKeyBytes: ByteArray,
    val encryptedPayload: ByteArray,
    val tag: ByteArray
) {

    fun asBytes(): ByteArray =
        publicKeyBytes + encryptedPayload + tag
}
