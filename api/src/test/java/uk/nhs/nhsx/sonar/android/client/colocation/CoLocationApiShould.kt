/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import com.android.volley.Request.Method.PATCH
import com.android.volley.VolleyError
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.assertBodyHasJson
import uk.nhs.nhsx.sonar.android.client.http.volley.TestQueue
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage

class CoLocationApiShould {

    private val encryptionKeyStorage = object : EncryptionKeyStorage by mockk() {
        override fun provideKey() = ByteArray(0)
    }
    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val httpClient = VolleyHttpClient(requestQueue)
    private val coLocationApi = CoLocationApi(baseUrl, encryptionKeyStorage, httpClient)

    @Test
    fun testSave_Request() {
        val events = listOf(
            CoLocationEvent("001", listOf(-10, 0), "2s ago", 10),
            CoLocationEvent("002", listOf(-10, -10, 10), "yesterday", 120)
        )

        coLocationApi.save(CoLocationData("::sonar-id::", events), {}, {})

        val request = requestQueue.lastRequest
        assertThat(request.method).isEqualTo(PATCH)
        assertThat(request.url).isEqualTo("$baseUrl/api/residents/::sonar-id::")
        request.assertBodyHasJson(
            "contactEvents" to listOf(
                mapOf(
                    "sonarId" to "001",
                    "rssiValues" to listOf(-10, 0),
                    "timestamp" to "2s ago",
                    "duration" to 10
                ),
                mapOf(
                    "sonarId" to "002",
                    "rssiValues" to listOf(-10, -10, 10),
                    "timestamp" to "yesterday",
                    "duration" to 120
                )
            )
        )
    }

    @Test
    fun testSave_OnSuccess() {
        var success = false
        var error = false

        coLocationApi.save(CoLocationData("::sonar-id::", emptyList()), { success = true }, { error = true })

        requestQueue.returnSuccess(JSONObject())
        assertThat(success).isTrue()
        assertThat(error).isFalse()
    }

    @Test
    fun testSave_OnError() {
        var success = false
        var error = false

        coLocationApi.save(CoLocationData("::sonar-id::", emptyList()), { success = true }, { error = true })

        requestQueue.returnError(VolleyError())
        assertThat(success).isFalse()
        assertThat(error).isTrue()
    }
}
