/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.HttpMethod.POST
import uk.nhs.nhsx.sonar.android.app.http.PromiseAssert.Companion.assertThat
import java.util.Base64

class HttpClientTest {

    private val queue = TestQueue()
    private val httpClient = HttpClient(
        queue,
        sonarHeaderValue = "someValue",
        appVersion = "buildInfo"
    ) { Base64.getEncoder().encodeToString(it) }

    @Test
    fun `test send() POST request, without encryption key`() {
        val inputRequest = HttpRequest(
            POST,
            "http://localhost:123/api",
            jsonObjectOf("foo" to "bar")
        )
        val promise = httpClient.send(inputRequest)

        assertThat(promise).isInProgress()
        assertThat(queue.requests).hasSize(1)

        val request = queue.requests.first()
        assertThat(request.method).isEqualTo(Request.Method.POST)
        assertThat(request.url).isEqualTo("http://localhost:123/api")
        assertThat(request.body).isEqualTo(jsonOf("foo" to "bar").toByteArray())
        assertThat(request.bodyContentType).contains("application/json")

        val headers = request.headers
        assertThat(headers).containsEntry("Accept", "application/json")
        assertThat(headers).containsEntry("X-Sonar-Foundation", "someValue")
        assertThat(headers).containsEntry("X-Sonar-App-Version", "buildInfo")
    }

    @Test
    fun `test send() PATCH request, with encryption key`() {
        val inputRequest =
            HttpRequest(
                method = HttpMethod.PATCH,
                url = "http://localhost:123/api",
                jsonBody = JSONObject(),
                secretKey = generateSignatureKey()
            )
        val promise = httpClient.send(inputRequest)

        assertThat(promise).isInProgress()
        assertThat(queue.requests).hasSize(1)

        val request = queue.requests.first()
        assertThat(request.method).isEqualTo(Request.Method.PATCH)
        assertThat(request.url).isEqualTo("http://localhost:123/api")
        assertThat(request.body).isEqualTo("{}".toByteArray())
        assertThat(request.bodyContentType).contains("application/json")

        val headers = request.headers
        assertThat(headers).containsEntry("Accept", "application/json")
        assertThat(headers).containsEntry("X-Sonar-Foundation", "someValue")
    }

    @Test
    fun `test send() on success`() {
        val inputRequest = HttpRequest(HttpMethod.GET, "http://localhost:123/api")
        val promise = httpClient.send(inputRequest)

        queue.returnSuccess(jsonObjectOf("hello" to "world"))

        assertThat(promise).succeeded()
        assertThat(promise.value).isInstanceOf(JSONObject::class.java)
        assertThat(promise.value.toString()).isEqualTo(jsonOf("hello" to "world"))
    }

    @Test
    fun `test send() on HTTP error`() {
        val inputRequest = HttpRequest(HttpMethod.GET, "http://localhost:123/api")
        val promise = httpClient.send(inputRequest)
        val volleyError = VolleyError(buildNetworkResponse(503))

        queue.returnError(volleyError)

        assertThat(promise).failedWith<VolleyError>(code = 503)
    }

    @Test
    fun `test send() on networking failure`() {
        val inputRequest = HttpRequest(HttpMethod.GET, "http://localhost:123/api")
        val promise = httpClient.send(inputRequest)
        val errorWithoutNetworkResponse = VolleyError("Oopsies")

        queue.returnError(errorWithoutNetworkResponse)

        assertThat(promise).failedWith<VolleyError>("Oopsies")
    }
}

private fun buildNetworkResponse(statusCode: Int) =
    NetworkResponse(statusCode, ByteArray(0), true, 0L, listOf())
