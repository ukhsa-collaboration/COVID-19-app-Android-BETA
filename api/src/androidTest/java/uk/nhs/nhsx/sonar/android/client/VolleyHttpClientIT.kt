package uk.nhs.nhsx.sonar.android.client

import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient

class VolleyHttpClientIT {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start(8089)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun shouldParseCorrectlyJsonResponseWithNoContent() {
        var responseJson: JSONObject? = null
        val client = VolleyHttpClient(
            "http://localhost:8089",
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        val request = HttpRequest("/", JSONObject())

        server.enqueue(
            MockResponse().setResponseCode(200)
        )

        client.post(request, { responseJson = it }, {})

        await untilNotNull { responseJson }
        assertThat(responseJson.toString()).isEqualTo("{}")
    }
}
