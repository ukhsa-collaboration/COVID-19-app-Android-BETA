/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import com.android.volley.Request.Method
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.TestQueue
import uk.nhs.nhsx.sonar.android.app.http.generateSignatureKey
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import java.nio.charset.Charset
import java.util.Base64

class ReferenceCodeApiTest {

    private val sonarIdProvider = mockk<SonarIdProvider>()

    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val secretKeyStorage = mockk<SecretKeyStorage>()
    private val httpClient = HttpClient(requestQueue, "someValue") { Base64.getEncoder().encodeToString(it) }

    private val api = ReferenceCodeApi(baseUrl, sonarIdProvider, secretKeyStorage, httpClient)

    @Test
    fun test() {
        every { secretKeyStorage.provideSecretKey() } returns generateSignatureKey()
        every { sonarIdProvider.get() } returns "some-sonar-id-101"

        val promise = api.generate()

        verifyAll {
            secretKeyStorage.provideSecretKey()
            sonarIdProvider.get()
        }
        assertThat(promise.isInProgress).isTrue()

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("http://api.example.com/api/residents/some-sonar-id-101/linking-id")
        assertThat(request.method).isEqualTo(Method.PUT)
        assertThat(request.body.toString(Charset.defaultCharset())).isEqualTo("{}")
        assertThat(request.headers).containsKey("Sonar-Request-Timestamp")
        assertThat(request.headers).containsKey("Sonar-Message-Signature")

        requestQueue.returnSuccess(
            jsonObjectOf("linkingId" to "some test linking id 200")
        )
        assertThat(promise.value).isEqualTo(ReferenceCode("some test linking id 200"))
    }
}
