/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.crypto

import java.security.PrivateKey
import java.security.PublicKey

interface EphemeralKeyProvider {
    fun providePublicKey(): PublicKey
    fun providePrivateKey(): PrivateKey
    fun providePublicKeyPoint(): ByteArray
}
