/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage

class CoLocationApiShould {

    lateinit var encryptionKeyStorage: EncryptionKeyStorage

    @Before
    fun setUp() {
        encryptionKeyStorage = mock(EncryptionKeyStorage::class.java)
        whenever(encryptionKeyStorage.provideKey()).thenReturn(ByteArray(0))
    }

    @Test
    fun callHttpClientPatchWhenSendingCoLocationData() {
        val httpClient = mock(HttpClient::class.java)
        val cut = CoLocationApi(encryptionKeyStorage, httpClient)
        val jsonEvents = JSONArray("""[{"eventId": 1}]""")

        cut.save(CoLocationData("residentId", jsonEvents))

        val requestCaptor = argumentCaptor<HttpRequest>()
        verify(httpClient).patch(requestCaptor.capture(), any(), any())

        val request = requestCaptor.firstValue
        assertThat(request.urlPath).isEqualTo("/api/residents/residentId")
        assertThat(request.json.toString()).isEqualTo("""{"contactEvents":[{"eventId":1}]}""")
    }

    @Test
    fun callTheSuccessCallbackInCaseOfSuccess() {
        val httpClient = mock(HttpClient::class.java)
        val cut = CoLocationApi(encryptionKeyStorage, httpClient)
        val jsonEvents = JSONArray("""[{"eventId": 1}]""")

        var called = false
        cut.save(CoLocationData("residentId", jsonEvents), { called = true }, {})

        val onSuccessCaptor = argumentCaptor<(JSONObject?) -> Unit>()
        verify(httpClient).patch(any(), onSuccessCaptor.capture(), any())

        onSuccessCaptor.firstValue.invoke(null)
        assertThat(called).isTrue()
    }

    @Test
    fun callTheErrorCallbackInCaseOfException() {
        val httpClient = mock(HttpClient::class.java)

        val cut = CoLocationApi(encryptionKeyStorage, httpClient)
        val jsonEvents = JSONArray("""[{"eventId": 1}]""")

        var expectedError = Exception("something")

        cut.save(CoLocationData("residentId", jsonEvents), {}, { error -> expectedError = error })

        val onErrorCaptor = argumentCaptor<(Exception) -> Unit>()
        verify(httpClient).patch(any(), any(), onErrorCaptor.capture())

        onErrorCaptor.firstValue.invoke(Exception("boom"))
        assertThat(expectedError).hasMessage("boom")
    }
}
