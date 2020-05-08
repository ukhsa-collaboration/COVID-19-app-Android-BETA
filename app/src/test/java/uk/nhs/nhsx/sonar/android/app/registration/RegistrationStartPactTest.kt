/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

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
class RegistrationStartPactTest {

    @get:Rule
    val provider = PactProviderRule("Registration API", this)

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Pact(consumer = "Android App")
    fun pact(builder: PactDslWithProvider): RequestResponsePact =
        builder
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

    @Test
    @PactVerification
    fun `verifies contract for starting a registration`() {
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

    private fun testQueue(): RequestQueue =
        RequestQueue(
            NoCache(),
            BasicNetwork(HurlStack()),
            1,
            ExecutorDelivery(Executors.newSingleThreadExecutor())
        ).apply { start() }
}
