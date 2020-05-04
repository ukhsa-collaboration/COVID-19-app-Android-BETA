package uk.nhs.nhsx.sonar.android.app.registration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit.PactProviderRule
import au.com.dius.pact.consumer.junit.PactVerification
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import com.android.volley.ExecutorDelivery
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.NoCache
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class RegistrationPactTest {
    @Rule
    @JvmField
    val provider = PactProviderRule("registration_api", this)

    @Rule
    @JvmField
    var rule = InstantTaskExecutorRule()

    @Pact(consumer = "android_app")
    fun pact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("no existing registration")
            // request
            .uponReceiving("a registration request")
            .path("/api/devices/registrations")
            .method("POST")
            .body(PactDslJsonBody().stringValue("pushToken", "a-valid-token-with-min-length-15"))
            // response
            .willRespondWith()
            .status(HttpStatus.SC_NO_CONTENT)
            .toPact()
    }

    @Test
    @PactVerification
    fun `verifies pact with resident api provider`() {
        val encryptionKeyStorage = mockk<KeyStorage>(relaxed = true)
        val httpClient = HttpClient(testQueue(), "some-header")
        val residentApi = ResidentApi(
            provider.url,
            encryptionKeyStorage,
            httpClient
        )

        val request = residentApi.register("a-valid-token-with-min-length-15")
        runBlocking { request.toCoroutine() }
        assertThat(request.isSuccess).isTrue()
    }

    private fun testQueue(): RequestQueue {
        return RequestQueue(
            NoCache(),
            BasicNetwork(HurlStack()),
            1,
            ExecutorDelivery(Executors.newSingleThreadExecutor())
        ).apply { start() }
    }
}
