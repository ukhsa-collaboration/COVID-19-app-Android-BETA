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
import uk.nhs.nhsx.sonar.android.client.http.volley.TestQueue
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage
import uk.nhs.nhsx.sonar.android.client.test.assertBodyHasJson

class CoLocationApiShould {

    private val encryptionKeyStorage = object : EncryptionKeyStorage by mockk() {
        override fun provideKey() = ByteArray(0)
    }
    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val httpClient = VolleyHttpClient(baseUrl, requestQueue)

    @Test
    fun testSave_Request() {
        val cut = CoLocationApi(encryptionKeyStorage, httpClient)
        val events = listOf(
            CoLocationEvent("001", listOf(-10, 0), "2s ago", 10),
            CoLocationEvent("002", listOf(-10, -10, 10), "yesterday", 120)
        )

        cut.save(CoLocationData("residentId", events), {}, {})

        val request = requestQueue.lastRequest
        assertThat(request.method).isEqualTo(PATCH)
        assertThat(request.url).isEqualTo("$baseUrl/api/residents/residentId")
        request.assertBodyHasJson(
            mapOf(
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
        )
    }

    @Test
    fun testSave_OnSuccess() {
        val cut = CoLocationApi(encryptionKeyStorage, httpClient)
        var success = false
        var error = false

        cut.save(CoLocationData("residentId", emptyList()), { success = true }, { error = true })

        requestQueue.returnSuccess(JSONObject())
        assertThat(success).isTrue()
        assertThat(error).isFalse()
    }

    @Test
    fun testSave_OnError() {
        val cut = CoLocationApi(encryptionKeyStorage, httpClient)
        var success = false
        var error = false

        cut.save(CoLocationData("residentId", emptyList()), { success = true }, { error = true })

        requestQueue.returnError(VolleyError())
        assertThat(success).isFalse()
        assertThat(error).isTrue()
    }
}
