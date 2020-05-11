/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import java.nio.ByteBuffer
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class BluetoothIdProviderTest {

    private val secretKeyStorage = mockk<SecretKeyStorage>()
    private val currentDateProvider = mockk<() -> DateTime>()

    private val cryptogramProvider = mockk<CryptogramProvider>()
    private val signer = BluetoothIdSigner(secretKeyStorage)
    private val idProvider: BluetoothIdProvider = BluetoothIdProvider(
        cryptogramProvider,
        signer,
        currentDateProvider
    )

    private val startTime = DateTime.parse("2020-04-24T14:00:00Z")
    private val countryCode = COUNTRY_CODE
    private val cryptogram = Cryptogram.fromBytes(Random.Default.nextBytes(Cryptogram.SIZE))

    private val secretKey =
        SecretKeySpec(Base64.getDecoder().decode("ddYTB7doP61hMnChfcWBVvvZsP5l5ypar5eeFQrBfY8="), "HMACSHA256")

    @Before
    fun setUp() {
        every { secretKeyStorage.provideSecretKey() } returns secretKey
        every { currentDateProvider.invoke() } returns startTime
        every { cryptogramProvider.provideCryptogram() } returns cryptogram
        every { cryptogramProvider.canProvideCryptogram() } returns true
    }

    @Test
    fun `contains correct country code`() {
        val payload = idProvider.provideBluetoothPayload()
        assertThat(payload.countryCode).isEqualTo(countryCode)
    }

    @Test
    fun `contains the txPower`() {
        val payload = idProvider.provideBluetoothPayload()
        // https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/bluetooth/le/BluetoothLeAdvertiser.java#144
        assertThat(payload.txPower).isEqualTo(-7)
    }

    @Test
    fun `contains the transmission time`() {
        val payload = idProvider.provideBluetoothPayload()
        assertThat(payload.transmissionTime).isEqualTo(1587736800)
    }

    @Test
    fun `contains first half of hmac`() {
        val payload = idProvider.provideBluetoothPayload()
        val transmissionTimeBytes = ByteBuffer.wrap(ByteArray(4)).apply {
            putInt(payload.transmissionTime)
        }.array()
        val expectedSignature = signer.computeHmacSignature(
            countryCode,
            payload.cryptogram.asBytes(),
            payload.txPower,
            transmissionTimeBytes
        )
        assertThat(payload.hmacSignature).isEqualTo(expectedSignature)
    }

    @Test
    fun `can provide identifier if sonarId and encrypter has public key`() {
        assertThat(idProvider.canProvideIdentifier()).isTrue()
    }

    @Test
    fun `can not provide identifier if we cannot get a cryptogram`() {
        every { cryptogramProvider.canProvideCryptogram() } returns false
        assertThat(idProvider.canProvideIdentifier()).isFalse()
    }

    @Test
    fun `can not provide identifier if secret key is not available`() {
        every { secretKeyStorage.provideSecretKey() } returns null
        assertThat(idProvider.canProvideIdentifier()).isFalse()
    }
}
