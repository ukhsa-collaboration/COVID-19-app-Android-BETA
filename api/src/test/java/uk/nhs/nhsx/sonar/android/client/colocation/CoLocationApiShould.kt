/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
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
        val jsonEvents = JSONArray("""[{"eventId": 1}]""")

        cut.save(CoLocationData("residentId", jsonEvents))

        val requestSlot = slot<HttpRequest>()
        verify { httpClient.patch(capture(requestSlot), any(), any()) }

        val request = requestSlot.captured
        assertThat(request.urlPath).isEqualTo("/api/residents/residentId")
        assertThat(request.json.toString()).isEqualTo("""{"contactEvents":[{"eventId":1}]}""")
    }

    @Test
    fun callTheSuccessCallbackInCaseOfSuccess() {
        val cut = CoLocationApi(encryptionKeyStorage, httpClient)
        val jsonEvents = JSONArray("""[{"eventId": 1}]""")

        var called = false
        cut.save(CoLocationData("residentId", jsonEvents), { called = true }, {})

        val onSuccessCaptor = slot<(JSONObject?) -> Unit>()
        verify { httpClient.patch(any(), capture(onSuccessCaptor), any()) }

        onSuccessCaptor.captured.invoke(null)
        assertThat(called).isTrue()
    }

    @Test
    fun callTheErrorCallbackInCaseOfException() {

        val cut = CoLocationApi(encryptionKeyStorage, httpClient)
        val jsonEvents = JSONArray("""[{"eventId": 1}]""")

        var expectedError = Exception("something")

        cut.save(CoLocationData("residentId", jsonEvents), {}, { error -> expectedError = error })

        val onErrorCaptor = slot<(Exception) -> Unit>()
        verify { httpClient.patch(any(), any(), capture(onErrorCaptor)) }

        onErrorCaptor.captured.invoke(Exception("boom"))
        assertThat(expectedError).hasMessage("boom")
    }
}
