/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import java.lang.Exception


class CoLocationApiShould {

    @Test
    fun callHttpClientPatchWhenSendingCoLocationData() {
        val httpClient = mock(HttpClient::class.java)
        val cut = CoLocationApi(ByteArray(0), httpClient)

        cut.save(CoLocationData("residentId"))

        val requestCaptor = argumentCaptor<HttpRequest>()
        verify(httpClient).patch(requestCaptor.capture(), any(), any())
        assertThat(requestCaptor.firstValue.urlPath).isEqualTo("/api/residents/residentId")
    }

    @Test
    fun callTheSuccessCallbackInCaseOfSuccess() {
        val httpClient = mock(HttpClient::class.java)
        val cut = CoLocationApi(ByteArray(0), httpClient)

        var called = false
        cut.save(CoLocationData("residentId"), {called = true}, {})

        val onSuccessCaptor = argumentCaptor<(JSONObject?) -> Unit>()
        verify(httpClient).patch(any(), onSuccessCaptor.capture(), any())

        onSuccessCaptor.firstValue.invoke(null)
        Assertions.assertThat(called).isTrue()
    }

    @Test
    fun callTheErrorCallbackInCaseOfException() {
        val httpClient = mock(HttpClient::class.java)
        val cut = CoLocationApi(ByteArray(0), httpClient)

        var expectedError = Exception("something")

        cut.save(CoLocationData("residentId"), {}, { error ->  expectedError = error })

        val onErrorCaptor = argumentCaptor<(Exception) -> Unit>()
        verify(httpClient).patch(any(), any(), onErrorCaptor.capture())

        onErrorCaptor.firstValue.invoke(Exception("boom"))
        Assertions.assertThat(expectedError).hasMessage("boom")
    }

}
