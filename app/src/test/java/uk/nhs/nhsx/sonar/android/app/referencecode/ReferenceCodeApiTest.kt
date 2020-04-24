package uk.nhs.nhsx.sonar.android.app.referencecode

import com.android.volley.Request.Method
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.TestQueue
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider

class ReferenceCodeApiTest {

    private val sonarIdProvider = mockk<SonarIdProvider>()

    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val httpClient = HttpClient(requestQueue, "someValue")

    private val api = ReferenceCodeApi(baseUrl, sonarIdProvider, httpClient)

    @Test
    fun test() {
        every { sonarIdProvider.getSonarId() } returns "some-sonar-id-101"

        val promise = api.generate()

        assertThat(promise.isInProgress).isTrue()

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("http://api.example.com/api/residents/some-sonar-id-101/linking-id")
        assertThat(request.method).isEqualTo(Method.PUT)

        requestQueue.returnSuccess(
            jsonObjectOf(
                "linkingId" to "some test linking id 200"
            )
        )
        assertThat(promise.value).isEqualTo(ReferenceCode("some test linking id 200"))
    }
}
