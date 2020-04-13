/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec

class ECP256KeyProvider : EphemeralKeyProvider {
    private var keyPair: KeyPair

    init {
        val ecSpec = ECGenParameterSpec(EC_STANDARD_CURVE_NAME)
        val g: KeyPairGenerator = KeyPairGenerator.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME)
        g.initialize(ecSpec, SecureRandom())
        keyPair = g.generateKeyPair()
    }

    override fun providePublicKey(): PublicKey = keyPair.public
    override fun providePrivateKey(): PrivateKey = keyPair.private

    // the
    override fun providePublicKeyPoint(): ByteArray {
        // using the Bouncy Castle specific type because it exposes the points
        val key = (keyPair.public as BCECPublicKey)
        key.q.normalize()
        val x = key.q.affineXCoord.encoded
        val y = key.q.affineYCoord.encoded
        return byteArrayOf(0x04) + x + y
    }
}
