/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import java.security.PrivateKey
import java.security.PublicKey

interface EphemeralKeyProvider {
    fun providePublicKey(): PublicKey
    fun providePrivateKey(): PrivateKey
    fun providePublicKeyPoint(): ByteArray
}
