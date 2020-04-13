/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.junit.Before
import org.junit.Test
import java.security.KeyFactory
import java.security.Security
import java.security.spec.ECPublicKeySpec

class ECP256KeyProviderTest {
    init {
        val bouncyCastleProvider = org.bouncycastle.jce.provider.BouncyCastleProvider()
        Security.insertProviderAt(bouncyCastleProvider, 1)
    }

    lateinit var provider: EphemeralKeyProvider

    @Before
    fun setUp() {
        provider = ECP256KeyProvider()
    }

    @Test
    fun `keys are generated with the correct algorithm`() {
        assertThat(provider.providePublicKey().algorithm).isEqualTo("EC")
        assertThat(provider.providePrivateKey().algorithm).isEqualTo("EC")
    }

    @Test
    fun `exposes the publicKey as X9dot62 if you squint at it`() {
        // leading 0x04 then two coordinates as 32 bit integers
        assertThat(provider.providePublicKeyPoint()).hasSize(65)
    }

    @Test
    fun `key point can recreate public key`() {
        val encodedKeyPoints = provider.providePublicKeyPoint()

        val spec = ECNamedCurveTable.getParameterSpec(EC_STANDARD_CURVE_NAME)
        val params = ECNamedCurveSpec(EC_STANDARD_CURVE_NAME, spec.curve, spec.g, spec.n)
        val publicPoint = ECPointUtil.decodePoint(params.curve, encodedKeyPoints)
        val pubKeySpec = ECPublicKeySpec(publicPoint, params)
        val publicKey = KeyFactory.getInstance("EC", PROVIDER_NAME).generatePublic(pubKeySpec)

        assertThat(publicKey).isEqualTo(provider.providePublicKey())
    }
}
