/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import androidx.test.rule.ActivityTestRule
import junit.framework.TestCase
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Minutes
import org.joda.time.Seconds
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramProvider
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramStorage
import uk.nhs.nhsx.sonar.android.app.crypto.Encrypter
import uk.nhs.nhsx.sonar.android.app.crypto.EphemeralKeyProvider
import uk.nhs.nhsx.sonar.android.app.http.AndroidSecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.DelegatingKeyStore
import uk.nhs.nhsx.sonar.android.app.http.PUBLIC_KEY_FILENAME
import uk.nhs.nhsx.sonar.android.app.http.SECRET_KEY_PREFERENCE_FILENAME
import uk.nhs.nhsx.sonar.android.app.http.SharedPreferencesPublicKeyStorage
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import java.security.KeyStore
import kotlin.random.Random

class CryptogramProviderTest {
    @get:Rule
    val activityRule = ActivityTestRule(FlowTestStartActivity::class.java)

    private val context by lazy { activityRule.activity.applicationContext }
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private lateinit var sonarIdProvider: SonarIdProvider
    private lateinit var encrypter: Encrypter
    private lateinit var cryptogramStorage: CryptogramStorage

    private val startTime = DateTime.parse("2020-04-24T14:00:00Z")

    private val currentDateProvider = { startTime }

    @Test
    fun testAll() {
        val tests = listOf(
            ::returnsTheSameCryptogramIfRequestedOnSameDay,
            ::createsNewCryptogramAfterMidnightNextDay,
            ::createsANewCryptogramIfCurrentTimeIsBeforeTheValidityPeriod,
            ::doesNotLoseTheCryptogramWhenReinitialised,
            ::returnsStoredCryptogramIfValid,
            ::updatesStoredCryptogramIfExpired
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
        publicKeyStorage.storeServerPublicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEu1f68MqDXbKeTqZMTHsOGToO4rKnPClXe/kE+oWqlaWZQv4J1E98cUNdpzF9JIFRPMCNdGOvTr4UB+BhQv9GWg==")

        encrypter = Encrypter(DelegatingKeyStore(secretKeyStorage, publicKeyStorage), EphemeralKeyProvider())
        cryptogramStorage = CryptogramStorage(context)
        cryptogramStorage.clear()
    }

    private fun returnsTheSameCryptogramIfRequestedOnSameDay() {
        var call = 0
        val currentDateProvider = {
            call++
            when (call) {
                1 -> { startTime }
                2 -> { startTime.plus(Minutes.minutes(47)) }
                3 -> { startTime.plus(Hours.FOUR) }
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

    private fun createsNewCryptogramAfterMidnightNextDay() {
        var call = 0
        val currentDateProvider = {
            call++
            when (call) {
                1 -> { startTime }
                2 -> { startTime.plus(Hours.hours(10)).plus(Seconds.ONE) }
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

    fun createsANewCryptogramIfCurrentTimeIsBeforeTheValidityPeriod() {
        var call = 0
        val currentDateProvider = {
            call++
            when (call) {
                1 -> { startTime }
                2 -> { startTime.minus(Hours.hours(25)) }
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

    fun doesNotLoseTheCryptogramWhenReinitialised() {
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

    fun returnsStoredCryptogramIfValid() {
        val provider = CryptogramProvider(
            sonarIdProvider,
            encrypter,
            cryptogramStorage,
            currentDateProvider
        )
        val storedCryptogram = Cryptogram.fromBytes(Random.Default.nextBytes(Cryptogram.SIZE))
        cryptogramStorage.set(Pair(startTime.minus(Hours.FIVE).millis, storedCryptogram))

        val cryptogram = provider.provideCryptogram()
        assertThat(cryptogram).isEqualTo(storedCryptogram)
    }

    fun updatesStoredCryptogramIfExpired() {
        val provider = CryptogramProvider(
            sonarIdProvider,
            encrypter,
            cryptogramStorage,
            currentDateProvider
        )
        val storedCryptogram = Cryptogram.fromBytes(Random.Default.nextBytes(Cryptogram.SIZE))
        cryptogramStorage.set(Pair(startTime.minus(Days.ONE).millis, storedCryptogram))

        val cryptogram = provider.provideCryptogram()
        assertThat(cryptogram).isNotEqualTo(storedCryptogram)
        val updatedCryptogram = cryptogramStorage.get().second!!
        assertThat(updatedCryptogram).isNotEqualTo(storedCryptogram)
    }
}
