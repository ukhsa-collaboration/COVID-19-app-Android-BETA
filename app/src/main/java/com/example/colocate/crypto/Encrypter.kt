/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.crypto

interface Encrypter {
    fun encrypt(plainText: ByteArray): Cryptogram
}
