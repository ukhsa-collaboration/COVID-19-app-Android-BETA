/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import androidx.test.rule.ActivityTestRule
import junit.framework.TestCase
import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.KDF2BytesGenerator
import org.bouncycastle.crypto.params.KDFParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Minutes
import org.joda.time.Seconds
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.crypto.AES
import uk.nhs.nhsx.sonar.android.app.crypto.AES_GCM_NoPadding
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramProvider
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramStorage
import uk.nhs.nhsx.sonar.android.app.crypto.ECDH
import uk.nhs.nhsx.sonar.android.app.crypto.EC_STANDARD_CURVE_NAME
import uk.nhs.nhsx.sonar.android.app.crypto.ELLIPTIC_CURVE
import uk.nhs.nhsx.sonar.android.app.crypto.Encrypter
import uk.nhs.nhsx.sonar.android.app.crypto.EphemeralKeyProvider
import uk.nhs.nhsx.sonar.android.app.crypto.GCM_TAG_LENGTH
import uk.nhs.nhsx.sonar.android.app.crypto.PROVIDER_NAME
import uk.nhs.nhsx.sonar.android.app.http.AndroidSecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.DelegatingKeyStore
import uk.nhs.nhsx.sonar.android.app.http.PUBLIC_KEY_FILENAME
import uk.nhs.nhsx.sonar.android.app.http.SECRET_KEY_PREFERENCE_FILENAME
import uk.nhs.nhsx.sonar.android.app.http.SharedPreferencesPublicKeyStorage
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class CryptogramProviderTest {
    @get:Rule
    val activityRule = ActivityTestRule(FlowTestStartActivity::class.java)

    private val context by lazy { activityRule.activity.applicationContext }
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private lateinit var sonarIdProvider: SonarIdProvider
    private lateinit var encrypter: Encrypter
    private lateinit var cryptogramStorage: CryptogramStorage

    private val startTime = DateTime.parse("2020-04-24T14:00:01Z")

    private val currentDateProvider = { startTime }

    private val exampleServerPublicKey =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEu1f68MqDXbKeTqZMTHsOGToO4rKnPClXe/kE+oWqlaWZQv4J1E98cUNdpzF9JIFRPMCNdGOvTr4UB+BhQv9GWg=="

    @Test
    fun testAll() {
        val tests = listOf(
            ::returnsTheSameCryptogramIfWithinFifteenMinutes,
            ::createsNewCryptogramAfterFifteenMinutes,
            ::createsANewCryptogramIfCurrentTimeIsBeforeTheValidityPeriod,
            ::doesNotLoseTheCryptogramWhenReinitialised,
            ::returnsStoredCryptogramIfValid,
            ::updatesStoredCryptogramIfExpired,
            ::storesCorrectEncryptedValidityInterval
        )

        tests.forEach {
            setUp()
            it()
        }
    }

    fun setUp() {
        keyStore.aliases().asSequence().forEach { keyStore.deleteEntry(it) }
        listOf(PUBLIC_KEY_FILENAME, SECRET_KEY_PREFERENCE_FILENAME).forEach {
            val clear = context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear()
            if (!clear.commit()) TestCase.fail("Unable to clear shared preference: $it")
        }

        sonarIdProvider = SonarIdProvider(context)
        sonarIdProvider.set("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")

        val secretKeyStorage = AndroidSecretKeyStorage(keyStore, context)
        secretKeyStorage.storeSecretKey("ddYTB7doP61hMnChfcWBVvvZsP5l5ypar5eeFQrBfY8=")
        val publicKeyStorage = SharedPreferencesPublicKeyStorage(context)
        publicKeyStorage.storeServerPublicKey(exampleServerPublicKey)

        encrypter = Encrypter(
            DelegatingKeyStore(secretKeyStorage, publicKeyStorage),
            EphemeralKeyProvider()
        )
        cryptogramStorage = CryptogramStorage(context)
        cryptogramStorage.clear()
    }

    private fun returnsTheSameCryptogramIfWithinFifteenMinutes() {
        var call = 0
        val currentDateProvider = {
            call++
            when (call) {
                1 -> {
                    startTime
                }
                2 -> {
                    startTime.plus(Minutes.minutes(14))
                }
                3 -> {
                    startTime.plus(Seconds.seconds(60))
                }
                else -> throw IllegalStateException()
            }
        }
        val provider = CryptogramProvider(
            sonarIdProvider,
            encrypter,
            cryptogramStorage,
            currentDateProvider
        )
        val cryptogram = provider.provideCryptogram()
        val cryptogramFortyMinutesLater = provider.provideCryptogram()
        assertThat(cryptogramFortyMinutesLater).isEqualTo(cryptogram)
        val cryptogramFourHoursLater = provider.provideCryptogram()
        assertThat(cryptogramFourHoursLater).isEqualTo(cryptogram)
    }

    private fun createsNewCryptogramAfterFifteenMinutes() {
        var call = 0
        val currentDateProvider = {
            call++
            when (call) {
                1 -> {
                    startTime
                }
                2 -> {
                    startTime.plus(Minutes.minutes(15)).plus(Seconds.ONE)
                }
                else -> throw IllegalStateException()
            }
        }
        val provider = CryptogramProvider(
            sonarIdProvider,
            encrypter,
            cryptogramStorage,
            currentDateProvider
        )
        val cryptogram = provider.provideCryptogram()
        val cryptogramTomorrow = provider.provideCryptogram()
        assertThat(cryptogramTomorrow).isNotEqualTo(cryptogram)
    }

    private fun createsANewCryptogramIfCurrentTimeIsBeforeTheValidityPeriod() {
        var call = 0
        val currentDateProvider = {
            call++
            when (call) {
                1 -> {
                    startTime
                }
                2 -> {
                    startTime.minus(Minutes.minutes(1))
                }
                else -> throw IllegalStateException()
            }
        }
        val provider = CryptogramProvider(
            sonarIdProvider,
            encrypter,
            cryptogramStorage,
            currentDateProvider
        )
        val cryptogram = provider.provideCryptogram()
        val cryptogramTwentyFiveHoursEarlier = provider.provideCryptogram()
        assertThat(cryptogramTwentyFiveHoursEarlier).isNotEqualTo(cryptogram)
    }

    private fun doesNotLoseTheCryptogramWhenReinitialised() {
        val provider = CryptogramProvider(
            sonarIdProvider,
            encrypter,
            cryptogramStorage,
            currentDateProvider
        )
        val cryptogram = provider.provideCryptogram()
        assertThat(cryptogram).isEqualTo(
            CryptogramProvider(
                sonarIdProvider,
                encrypter,
                cryptogramStorage,
                currentDateProvider
            ).provideCryptogram()
        )
    }

    private fun returnsStoredCryptogramIfValid() {
        val provider = CryptogramProvider(
            sonarIdProvider,
            encrypter,
            cryptogramStorage,
            currentDateProvider
        )
        val storedCryptogram = Cryptogram.fromBytes(Random.Default.nextBytes(Cryptogram.SIZE))
        cryptogramStorage.set(Pair(startTime.minus(Minutes.minutes(4)).millis, storedCryptogram))

        val cryptogram = provider.provideCryptogram()
        assertThat(cryptogram).isEqualTo(storedCryptogram)
    }

    private fun updatesStoredCryptogramIfExpired() {
        val provider = CryptogramProvider(
            sonarIdProvider,
            encrypter,
            cryptogramStorage,
            currentDateProvider
        )
        val storedCryptogram = Cryptogram.fromBytes(Random.Default.nextBytes(Cryptogram.SIZE))
        cryptogramStorage.set(Pair(startTime.plus(Minutes.minutes(20)).millis, storedCryptogram))

        val cryptogram = provider.provideCryptogram()
        assertThat(cryptogram).isNotEqualTo(storedCryptogram)
        val updatedCryptogram = cryptogramStorage.get().second!!
        assertThat(updatedCryptogram).isNotEqualTo(storedCryptogram)
    }

    // This could potentially be a unit test, that just checks that the encrypter gets the correct encoded timestamps.
    // However, it felt prudent to make sure it all works together.
    private fun storesCorrectEncryptedValidityInterval() {
        val provider = CryptogramProvider(
            sonarIdProvider,
            encrypter,
            cryptogramStorage,
            currentDateProvider
        )
        val cryptogram = provider.provideCryptogram()

        val plainText = decrypt(cryptogram)
        val start = decodeSecondsSinceEpochToDateTime(plainText, (0 until 4))
        val end = decodeSecondsSinceEpochToDateTime(plainText, (4 until 8))
        assertThat(start).isEqualTo(DateTime.parse("2020-04-24T14:00:01Z"))
        assertThat(end).isEqualTo(DateTime.parse("2020-04-24T14:15:01Z"))
    }

    private fun decodeSecondsSinceEpochToDateTime(plainText: ByteArray, range: IntRange): DateTime =
        ByteBuffer.wrap(plainText.sliceArray(range)).int.let { secondsSinceEpoch ->
            DateTime(secondsSinceEpoch.toLong() * 1_000, DateTimeZone.UTC)
        }

    private fun decrypt(
        cryptogram: Cryptogram
    ): ByteArray {
        val exampleServerPrivateKey = getExampleServerPrivateKey()
        val (keyBytes, iv) = deriveKey(
            generateSharedSecret(
                exampleServerPrivateKey,
                localPublicKeyFromCryptogram(cryptogram)
            ),
            cryptogram.publicKeyBytes
        )
        val key = SecretKeySpec(keyBytes, AES)

        val c: Cipher = Cipher.getInstance(AES_GCM_NoPadding, PROVIDER_NAME)
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return c.doFinal(cryptogram.encryptedPayload + cryptogram.tag)
    }

    private fun getExampleServerPrivateKey(): PrivateKey =
        KeyFactory.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME)
            .generatePrivate(
                PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(
                        "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgLCloV6m2p78+7EYHJyyniGdpLxiPhWk+q2AozMlWT4ahRANCAAS7V/rwyoNdsp5OpkxMew4ZOg7isqc8KVd7+QT6haqVpZlC/gnUT3xxQ12nMX0kgVE8wI10Y69OvhQH4GFC/0Za"
                    )
                )
            )!!

    private fun deriveKey(
        sharedSecret: SecretKey,
        publicKeyBytes: ByteArray
    ): Pair<ByteArray, ByteArray> {
        val kdfGenerator = KDF2BytesGenerator(SHA256Digest())
        kdfGenerator.init(KDFParameters(sharedSecret.encoded, byteArrayOf(0x04) + publicKeyBytes))
        val kdfOutput = ByteArray(32)
        kdfGenerator.generateBytes(kdfOutput, 0, 32)

        return Pair(
            kdfOutput.sliceArray((0 until 16)),
            kdfOutput.sliceArray((16 until 32))
        )
    }

    private fun generateSharedSecret(
        serverPrivateKey: PrivateKey,
        localPublicKey: PublicKey
    ): SecretKeySpec = KeyAgreement.getInstance(ECDH, PROVIDER_NAME).also {
        it.init(serverPrivateKey)
        it.doPhase(localPublicKey, true)
    }.let {
        SecretKeySpec(it.generateSecret(), AES)
    }

    private fun localPublicKeyFromCryptogram(cryptogram: Cryptogram): PublicKey {
        // leading 0x04 to specify that the points are compressed
        val encodedKeyPoints = byteArrayOf(0x04) + cryptogram.publicKeyBytes
        val spec = ECNamedCurveTable.getParameterSpec(EC_STANDARD_CURVE_NAME)
        val params = ECNamedCurveSpec(EC_STANDARD_CURVE_NAME, spec.curve, spec.g, spec.n)
        val publicPoint = ECPointUtil.decodePoint(params.curve, encodedKeyPoints)
        val pubKeySpec = ECPublicKeySpec(publicPoint, params)
        return KeyFactory.getInstance("EC", PROVIDER_NAME).generatePublic(pubKeySpec)
    }
}
