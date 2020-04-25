/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import javax.inject.Inject

class EphemeralKeyProvider @Inject constructor() {
    private val generator: KeyPairGenerator = KeyPairGenerator.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME).also {
        val parameterSpec = ECGenParameterSpec(EC_STANDARD_CURVE_NAME)
        it.initialize(parameterSpec, SecureRandom())
    }

    fun provideEphemeralKeys(): KeyPair = generator.generateKeyPair()
}

fun PublicKey.toPublicKeyPoint(): ByteArray {
    // using the Bouncy Castle specific type because it exposes the points
    val key = (this as BCECPublicKey)
    key.q.normalize()
    val x = key.q.affineXCoord.encoded
    val y = key.q.affineYCoord.encoded
    return byteArrayOf(0x04) + x + y
}
