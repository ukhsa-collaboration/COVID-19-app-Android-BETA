/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

interface Encrypter {
    fun encrypt(plainText: ByteArray): Cryptogram
}
