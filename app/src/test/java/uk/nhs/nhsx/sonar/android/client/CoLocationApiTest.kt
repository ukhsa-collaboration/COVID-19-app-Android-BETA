/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client

import com.android.volley.Request.Method.PATCH
import com.android.volley.VolleyError
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.TestQueue

class CoLocationApiTest {

    private val encryptionKeyStorage = object : KeyStorage by mockk() {
        override fun provideSecretKey() = ByteArray(0)
    }
    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val httpClient = HttpClient(requestQueue, "someValue")
    private val coLocationApi = CoLocationApi(baseUrl, encryptionKeyStorage, httpClient)

    @Test
    fun `test save() request`() {
        val events = listOf(
            CoLocationEvent(
                sonarId = "001",
                rssiValues = listOf(-10, 0),
                rssiOffsets = listOf(0, 6),
                timestamp = "2s ago",
                duration = 10
            ),
            CoLocationEvent(
                sonarId = "002",
                rssiValues = listOf(-10, -10, 10),
                rssiOffsets = listOf(0, 5, 20),
                timestamp = "yesterday",
                duration = 120
            )
        )

        val promise = coLocationApi.save(CoLocationData("::sonar-id::", "::timestamp::", events))

        assertThat(promise.isInProgress).isTrue()

        val request = requestQueue.lastRequest
        assertThat(request.method).isEqualTo(PATCH)
        assertThat(request.url).isEqualTo("$baseUrl/api/residents/::sonar-id::")
        request.assertBodyHasJson(
            "symptomsTimestamp" to "::timestamp::",
            "contactEvents" to listOf(
                mapOf(
                    "sonarId" to "001",
                    "rssiValues" to listOf(-10, 0),
                    "rssiOffsets" to listOf(0, 6),
                    "timestamp" to "2s ago",
                    "duration" to 10
                ),
                mapOf(
                    "sonarId" to "002",
                    "rssiValues" to listOf(-10, -10, 10),
                    "rssiOffsets" to listOf(0, 5, 20),
                    "timestamp" to "yesterday",
                    "duration" to 120
                )
            )
        )
    }

    @Test
    fun `test save() on success`() {
        val promise =
            coLocationApi.save(CoLocationData("::sonar-id::", "::timestamp::", emptyList()))

        requestQueue.returnSuccess(JSONObject())
        assertThat(promise.isSuccess).isTrue()
    }

    @Test
    fun `test save() on error`() {
        val promise =
            coLocationApi.save(CoLocationData("::sonar-id::", "::timestamp::", emptyList()))

        requestQueue.returnError(VolleyError())
        assertThat(promise.isFailed).isTrue()
    }
}
