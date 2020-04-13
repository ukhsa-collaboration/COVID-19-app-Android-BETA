/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

class Cryptogram(
    val publicKeyBytes: ByteArray,
    val encryptedPayload: ByteArray,
    val tag: ByteArray
) {

    fun asBytes(): ByteArray =
        publicKeyBytes + encryptedPayload + tag
}
