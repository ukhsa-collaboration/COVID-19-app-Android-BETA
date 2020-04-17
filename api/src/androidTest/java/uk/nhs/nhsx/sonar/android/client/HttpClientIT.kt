/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client

import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpMethod
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest

class HttpClientIT {

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
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val client = HttpClient(ctx)

        server.enqueue(MockResponse().setResponseCode(200))

        val request = HttpRequest(HttpMethod.POST, "http://localhost:8089", JSONObject())
        val promise = client.send(request)

        await until { promise.isSuccess }
        assertThat(promise.value.toString()).isEqualTo("{}")
    }
}
