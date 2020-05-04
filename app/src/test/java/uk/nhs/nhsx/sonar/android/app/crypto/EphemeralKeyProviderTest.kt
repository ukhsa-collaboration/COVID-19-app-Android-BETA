/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.KeyFactory
import java.security.Security
import java.security.spec.ECPublicKeySpec

class EphemeralKeyProviderTest {

    private val bouncyCastleProvider = BouncyCastleProvider()
    private val provider = EphemeralKeyProvider()

    @Before
    fun setUp() {
        Security.insertProviderAt(bouncyCastleProvider, 1)
    }

    @After
    fun tearDown() {
        Security.removeProvider(bouncyCastleProvider.name)
    }

    @Test
    fun `keys are generated with the correct algorithm`() {
        val keyPair = provider.provideEphemeralKeys()
        assertThat(keyPair.public.algorithm).isEqualTo("EC")
        assertThat(keyPair.private.algorithm).isEqualTo("EC")
    }

    @Test
    fun `key point can recreate public key`() {
        val keyPair = provider.provideEphemeralKeys()
        val encodedKeyPoints = keyPair.public.toPublicKeyPoint()

        val spec = ECNamedCurveTable.getParameterSpec(EC_STANDARD_CURVE_NAME)
        val params = ECNamedCurveSpec(EC_STANDARD_CURVE_NAME, spec.curve, spec.g, spec.n)
        val publicPoint = ECPointUtil.decodePoint(params.curve, encodedKeyPoints)
        val pubKeySpec = ECPublicKeySpec(publicPoint, params)
        val publicKey = KeyFactory.getInstance("EC", PROVIDER_NAME).generatePublic(pubKeySpec)

        assertThat(publicKey).isEqualTo(keyPair.public)
    }

    @Test
    fun `generates new keys each time`() {
        val firstKeyPair = provider.provideEphemeralKeys()
        assertThat(firstKeyPair).isNotEqualTo(provider.provideEphemeralKeys())
    }
}
