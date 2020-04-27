/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import com.android.volley.Request
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.HttpMethod.POST
import java.util.Base64

class HttpClientTest {

    private val queue = TestQueue()
    private val httpClient = HttpClient(queue, "someValue") { Base64.getEncoder().encodeToString(it) }

    @Test
    fun `test send() POST request, without encryption key`() {
        val inputRequest = HttpRequest(
            POST,
            "http://localhost:123/api",
            jsonObjectOf("foo" to "bar")
        )
        val promise = httpClient.send(inputRequest)

        assertThat(promise.isInProgress).isTrue()
        assertThat(queue.requests).hasSize(1)

        val request = queue.requests.first()
        assertThat(request.method).isEqualTo(Request.Method.POST)
        assertThat(request.url).isEqualTo("http://localhost:123/api")
        assertThat(request.body).isEqualTo(jsonOf("foo" to "bar").toByteArray())
        assertThat(request.bodyContentType).contains("application/json")

        val headers = request.headers
        assertThat(headers).containsEntry("Accept", "application/json")
        assertThat(headers).containsEntry("X-Sonar-Foundation", "someValue")
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

        assertThat(promise.isInProgress).isTrue()
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

        assertThat(promise.isSuccess).isTrue()
        assertThat(promise.value).isInstanceOf(JSONObject::class.java)
        assertThat(promise.value.toString()).isEqualTo(jsonOf("hello" to "world"))
        assertThat(promise.error).isNull()
    }

    @Test
    fun `test send() on failure`() {
        val inputRequest = HttpRequest(HttpMethod.GET, "http://localhost:123/api")
        val promise = httpClient.send(inputRequest)

        queue.returnError(VolleyError("Oopsies"))

        assertThat(promise.isFailed).isTrue()
        assertThat(promise.value).isNull()
        assertThat(promise.error).isInstanceOf(VolleyError::class.java)
        assertThat(promise.error).hasMessage("Oopsies")
    }
}
