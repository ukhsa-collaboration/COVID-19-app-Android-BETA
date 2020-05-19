package uk.nhs.nhsx.sonar.android.app.registration

import android.security.keystore.KeyProperties
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit.PactProviderRule
import au.com.dius.pact.consumer.junit.PactVerification
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.model.annotations.PactFolder
import com.android.volley.ExecutorDelivery
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.NoCache
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.KeyGenerator

@ExperimentalCoroutinesApi
@PactFolder("pacts")
class RegistrationConfirmDevicePactTest {

    @get:Rule
    val provider = PactProviderRule("Registration API", this)

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var serverPublicKey: String
    private lateinit var clientSymmetricKey: String
    private lateinit var registrationId: UUID

    @Pact(consumer = "Android App")
    fun pact(builder: PactDslWithProvider): RequestResponsePact {
        clientSymmetricKey = generatePrivateKey()
        serverPublicKey = generateServerPublicKey()
        registrationId = UUID.randomUUID()

        return builder
            .given("a successful registration start request")
            // request
            .uponReceiving("a device confirmation request")
            .path("/api/devices")
            .method("POST")
            .body(PactDslJsonBody()
                .uuid("activationCode")
                .stringMatcher("pushToken", ".{15,240}", "a-valid-token-with-min-length-15")
                .stringMatcher("deviceModel", ".{1,30}", "model12")
                .stringMatcher("deviceOSVersion", ".{1,30}", "os/2")
                .stringMatcher("postalCode", "^[A-Z]{1,2}[0-9R][0-9A-Z]?", "EC1V")
            )
            // response
            .willRespondWith()
            .body(PactDslJsonBody()
                .stringMatcher("secretKey", ".+", clientSymmetricKey)
                .stringMatcher("publicKey", ".+", serverPublicKey)
                .uuid("id", registrationId.toString())
            )
            .status(HttpStatus.SC_OK)
            .toPact()
    }

    @Test
    @PactVerification
    fun `verifies the contract for device confirmation`() {
        val encryptionKeyStorage = mockk<KeyStorage>(relaxed = true)
        val httpClient = HttpClient(testQueue(), "some-header")
        val residentApi = ResidentApi(
            provider.url,
            encryptionKeyStorage,
            httpClient
        )

        val deviceConfirmation = DeviceConfirmation(
            UUID.randomUUID().toString(),
            "a-valid-push-token",
            "model12",
            "os/2",
            "SW11"
        )

        val request = residentApi.confirmDevice(deviceConfirmation)
        runBlocking { request.toCoroutine() }

        assertThat(request.isSuccess).isTrue()
        assertThat(request.value!!.id).isEqualTo(registrationId.toString())

        verify { encryptionKeyStorage.storeServerPublicKey(serverPublicKey) }
        verify { encryptionKeyStorage.storeSecretKey(clientSymmetricKey) }
    }

    private fun generatePrivateKey(): String {
        return encodeBase64(KeyGenerator.getInstance("HMACSHA256").generateKey().encoded!!)
    }

    private fun generateServerPublicKey(): String {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        generator.initialize(128, SecureRandom())
        return encodeBase64(generator.genKeyPair().public.encoded)
    }

    private fun encodeBase64(byteArray: ByteArray) =
        Base64.getEncoder().encodeToString(byteArray)

    private fun testQueue(): RequestQueue {
        return RequestQueue(
            NoCache(),
            BasicNetwork(HurlStack()),
            1,
            ExecutorDelivery(Executors.newSingleThreadExecutor())
        ).apply { start() }
    }
}
