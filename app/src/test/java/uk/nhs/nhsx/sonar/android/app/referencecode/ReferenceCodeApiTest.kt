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
import uk.nhs.nhsx.sonar.android.app.http.PromiseAssert.Companion.assertThat
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.TestQueue
import uk.nhs.nhsx.sonar.android.app.http.generateSignatureKey
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf
import java.nio.charset.Charset
import java.util.Base64

class ReferenceCodeApiTest {

    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val secretKeyStorage = mockk<SecretKeyStorage>()
    private val httpClient = HttpClient(
        queue = requestQueue,
        sonarHeaderValue = "someValue",
        appVersion = "buildInfo"
    ) { Base64.getEncoder().encodeToString(it) }

    private val api = ReferenceCodeApi(baseUrl, secretKeyStorage, httpClient)

    @Test
    fun `get reference code`() {
        every { secretKeyStorage.provideSecretKey() } returns generateSignatureKey()
        val sonarId = "::some sonar id::"

        val promise = api.get(sonarId)

        verifyAll {
            secretKeyStorage.provideSecretKey()
        }
        assertThat(promise).isInProgress()

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("http://api.example.com/api/app-instances/linking-id")
        assertThat(request.method).isEqualTo(Method.PUT)
        assertThat(request.body.toString(Charset.defaultCharset())).isEqualTo("""{"sonarId":"::some sonar id::"}""")
        assertThat(request.headers).containsKey("Sonar-Request-Timestamp")
        assertThat(request.headers).containsKey("Sonar-Message-Signature")

        requestQueue.returnSuccess(
            jsonObjectOf("linkingId" to "some test linking id 200")
        )
        assertThat(promise).succeededWith(ReferenceCode("some test linking id 200"))
    }
}
