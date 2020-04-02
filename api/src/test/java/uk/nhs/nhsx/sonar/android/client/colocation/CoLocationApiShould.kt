/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage

class CoLocationApiShould {

    private val encryptionKeyStorage = object : EncryptionKeyStorage by mockk() {
        override fun provideKey() = ByteArray(0)
    }
    private val httpClient = mockk<HttpClient>(relaxed = true)

    @Test
    fun callHttpClientPatchWhenSendingCoLocationData() {
        val cut = CoLocationApi(encryptionKeyStorage, httpClient)
        val events = listOf(
            CoLocationEvent("001", listOf(-10, 0), "2s ago", 10),
            CoLocationEvent("002", listOf(-10, -10, 10), "yesterday", 120)
        )

        cut.save(CoLocationData("residentId", events))

        val requestSlot = slot<HttpRequest>()
        verify { httpClient.patch(capture(requestSlot), any(), any()) }

        val request = requestSlot.captured
        assertThat(request.urlPath).isEqualTo("/api/residents/residentId")
        assertThat(request.json.toString()).isEqualTo(
            JSONObject(
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
            ).toString()
        )
    }

    @Test
    fun callTheSuccessCallbackInCaseOfSuccess() {
        val cut = CoLocationApi(encryptionKeyStorage, httpClient)

        var called = false
        cut.save(CoLocationData("residentId", emptyList()), { called = true }, {})

        val onSuccessCaptor = slot<(JSONObject?) -> Unit>()
        verify { httpClient.patch(any(), capture(onSuccessCaptor), any()) }

        onSuccessCaptor.captured.invoke(null)
        assertThat(called).isTrue()
    }

    @Test
    fun callTheErrorCallbackInCaseOfException() {
        val cut = CoLocationApi(encryptionKeyStorage, httpClient)

        var expectedError = Exception("something")

        cut.save(CoLocationData("residentId", emptyList()), {}, { error -> expectedError = error })

        val onErrorCaptor = slot<(Exception) -> Unit>()
        verify { httpClient.patch(any(), any(), capture(onErrorCaptor)) }

        onErrorCaptor.captured.invoke(Exception("boom"))
        assertThat(expectedError).hasMessage("boom")
    }
}
