/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Minutes
import org.joda.time.Seconds
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.Security
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

class BluetoothIdProviderTest {
    private val sonarIdProvider = mockk<SonarIdProvider>()

    private val keyStorage = mockk<KeyStorage>()
    private val currentDateProvider = mockk<() -> DateTime>()
    private val ephemeralKeyProvider = EphemeralKeyProvider()

    private val encrypter = Encrypter(keyStorage, ephemeralKeyProvider)
    private val signer = BluetoothIdSigner(keyStorage)
    private val idProvider: BluetoothIdProvider = BluetoothIdProvider(
        sonarIdProvider,
        encrypter,
        signer,
        currentDateProvider
    )
    companion object {

        @BeforeClass
        @JvmStatic
        fun setUpBouncyCastle() {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }
    private val startTime = DateTime.parse("2020-04-24T14:00:00Z")
    private val publicKeyBytes = Base64.getDecoder()
        .decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEu1f68MqDXbKeTqZMTHsOGToO4rKnPClXe/kE+oWqlaWZQv4J1E98cUNdpzF9JIFRPMCNdGOvTr4UB+BhQv9GWg==")
    private val countryCode = "GB".toByteArray()

    private val secretKey = SecretKeySpec(Base64.getDecoder().decode("ddYTB7doP61hMnChfcWBVvvZsP5l5ypar5eeFQrBfY8="), "HMACSHA256")

    @Before
    fun setUp() {
        every { sonarIdProvider.hasProperSonarId() } returns true
        every { sonarIdProvider.getSonarId() } returns "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        val ecKeyFactory = KeyFactory.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME)
        every { keyStorage.providePublicKey() } returns ecKeyFactory.generatePublic(
            X509EncodedKeySpec(publicKeyBytes)
        )
        every { keyStorage.provideSecretKey() } returns secretKey
        every { currentDateProvider.invoke() } returns startTime
    }

    @Test
    fun `returns the same cryptogram if requested on same day`() {
        val cryptogram = idProvider.provideBluetoothPayload().cryptogram
        val newTime = startTime.plus(Minutes.minutes(47))
        every { currentDateProvider.invoke() } returns newTime
        assertThat(cryptogram).isEqualTo(idProvider.provideBluetoothPayload().cryptogram)
        every { currentDateProvider.invoke() } returns newTime.plus(Hours.FOUR)
        assertThat(cryptogram).isEqualTo(idProvider.provideBluetoothPayload().cryptogram)
    }

    @Test
    fun `creates new cryptogram if current one was generated yesterday`() {
        val cryptogram = idProvider.provideBluetoothPayload().cryptogram
        every { currentDateProvider.invoke() } returns startTime.plus(Hours.hours(10))
            .plus(Seconds.ONE)
        assertThat(cryptogram).isNotEqualTo(idProvider.provideBluetoothPayload().cryptogram)
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
    fun `can not provide identifier if sonarId is not set`() {
        every { sonarIdProvider.hasProperSonarId() } returns false
        assertThat(idProvider.canProvideIdentifier()).isFalse()
    }

    @Test
    fun `can not provide identifier if encrypter is not able to encrypt`() {
        every { keyStorage.providePublicKey() } returns null
        assertThat(idProvider.canProvideIdentifier()).isFalse()
    }

    @Test
    fun `can not provide identifier if secret key is not available`() {
        every { keyStorage.provideSecretKey() } returns null
        assertThat(idProvider.canProvideIdentifier()).isFalse()
    }
}
