/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.dius.pact.consumer.dsl.Matchers
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit.PactProviderRule
import au.com.dius.pact.consumer.junit.PactVerification
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import com.android.volley.ExecutorDelivery
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.NoCache
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdSigner
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationData
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationEvent
import uk.nhs.nhsx.sonar.android.app.diagnose.review.toJson
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.http.UTCClock
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import kotlin.random.Random

class StoppedUTCClock(private val alwaysNow: LocalDateTime) : UTCClock {
    override fun now(): LocalDateTime {
        return alwaysNow
    }
}

@ExperimentalCoroutinesApi
class CoLocationDataSubmissionPactTest {
    private val base64encoder = Base64.getEncoder()
    private lateinit var encryptionKeyStorage: KeyStorage
    private lateinit var secretKey: SecretKey
    private lateinit var utcNow: LocalDateTime
    private lateinit var colocationData: CoLocationData

    @get:Rule
    val provider = PactProviderRule("proximity_api", this)

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Pact(consumer = "android_app")
    fun pact(builder: PactDslWithProvider): RequestResponsePact {
        utcNow = LocalDateTime.now(DateTimeZone.UTC)
        val timestamp = utcNow.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")

        secretKey = generatePrivateKey()
        encryptionKeyStorage = mockk(relaxed = true)
        every { encryptionKeyStorage.provideSecretKey() } returns secretKey

        colocationData = CoLocationData(
            UUID.randomUUID().toString(),
            timestamp,
            (0..Random.nextInt(3, 10)).map { generateCoLocationEvent() }
        )

        return builder
            .given(
                "a confirmed registration with key",
                mutableMapOf<String, Any>("key" to encodeBase64(secretKey.encoded))
            )
            .given("the date and time is", mutableMapOf<String, Any>("timestamp" to timestamp))
            // request
            .uponReceiving("a proximity data submission")
            .matchPath(
                "/api/residents/${Matchers.UUID_REGEX}",
                "/api/residents/${colocationData.sonarId}"
            )
            .method("PATCH")
            .matchHeader(
                "Sonar-Request-Timestamp",
                "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z",
                timestamp
            )
            .headers(
                "Sonar-Message-Signature",
                generateSignature(
                    secretKey,
                    timestamp,
                    colocationData.toJson().toString().toByteArray()
                )
            )
            .body(colocationData.toJson())
            // response
            .willRespondWith()
            .status(HttpStatus.SC_NO_CONTENT)
            .toPact()
    }

    @Test
    @PactVerification
    fun `verifies submission of proximity event data`() {
        val httpClient =
            HttpClient(testQueue(), "some-header", StoppedUTCClock(utcNow), ::encodeBase64)
        val coLocationApi = CoLocationApi(
            provider.url,
            encryptionKeyStorage,
            httpClient
        )

        val request = coLocationApi.save(colocationData)
        runBlocking { request.toCoroutine() }

        assertThat(request.isSuccess).isTrue()
    }

    private fun generateCoLocationEvent(): CoLocationEvent {
        val bluetoothIdentifier = generateBluetoothIdentifier()
        val numRssiValues = Random.nextInt(3, 10)
        return CoLocationEvent(
            encryptedRemoteContactId = encodeBase64(bluetoothIdentifier.asBytes()),
            rssiValues = encodeBase64(
                (0..numRssiValues).map { Random.nextInt(-100, 100).toByte() }.toByteArray()
            ),
            rssiIntervals = (0..numRssiValues).map { Random.nextInt(0, 100) },
            timestamp = DateTime.now(DateTimeZone.UTC).toUtcIsoFormat(),
            duration = Random.nextInt(0, 1000),
            txPowerInProtocol = randomTxPower().toByte(),
            txPowerAdvertised = randomTxPower().toByte(),
            countryCode = ByteBuffer.wrap("GB".toByteArray()).short,
            transmissionTime = Random.nextInt(0, 50),
            hmacSignature = encodeBase64(bluetoothIdentifier.hmacSignature)
        )
    }

    private fun encodeBase64(byteArray: ByteArray): String {
        return base64encoder.encodeToString(byteArray)
    }

    private fun generateSignature(
        secretKey: SecretKey,
        timestamp: String,
        body: ByteArray
    ): String {
        val mac = Mac.getInstance("HMACSHA256")
            .apply {
                init(secretKey)
                update(timestamp.toByteArray(Charsets.UTF_8))
            }

        val signature = mac.doFinal(body)

        return encodeBase64(signature)
    }

    private fun generatePrivateKey(): SecretKey {
        return KeyGenerator.getInstance("HMACSHA256").generateKey()
    }

    private fun generateBluetoothIdentifier(): BluetoothIdentifier {
        val cryptogram = Cryptogram.fromBytes(
            Random.Default.nextBytes(Cryptogram.SIZE)
        )

        val txPowerLevel = randomTxPower().toByte()
        val transmissionTime = Random.nextInt(0, 30)
        val countryCode = "GB".toByteArray()

        val signer = BluetoothIdSigner(encryptionKeyStorage)
        val signature = signer.computeHmacSignature(
            countryCode,
            cryptogram.asBytes(),
            txPowerLevel,
            ByteBuffer.wrap(ByteArray(4)).apply {
                putInt(transmissionTime)
            }.array()
        )

        return BluetoothIdentifier(
            countryCode,
            cryptogram,
            txPowerLevel,
            transmissionTime,
            signature
        )
    }

    private fun randomTxPower() = Random.nextInt(-20, -1)

    private fun testQueue(): RequestQueue =
        RequestQueue(
            NoCache(),
            BasicNetwork(OkHttpStack()),
            1,
            ExecutorDelivery(Executors.newSingleThreadExecutor())
        ).apply { start() }
}
