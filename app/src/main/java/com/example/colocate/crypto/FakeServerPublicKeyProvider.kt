/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.crypto

import android.util.Base64
import uk.nhs.nhsx.sonar.android.client.security.ServerPublicKeyProvider
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class FakeServerPublicKeyProvider : ServerPublicKeyProvider {
    override fun providePublicKey(): PublicKey {
        val ecKeyFactory = KeyFactory.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME)
        val examplePEM = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEu1f68MqDXbKeTqZMTHsOGToO4rKnPClXe/kE+oWqlaWZQv4J1E98cUNdpzF9JIFRPMCNdGOvTr4UB+BhQv9GWg=="
        val pubKeyDER: ByteArray = Base64.decode(examplePEM, Base64.DEFAULT)
        val pubKeySpec = X509EncodedKeySpec(pubKeyDER)
        return ecKeyFactory.generatePublic(pubKeySpec)
    }
}
