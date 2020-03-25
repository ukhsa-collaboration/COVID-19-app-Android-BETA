/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.resident

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest

class ResidentApiShould {

    @Test
    fun postJsonToHttpClientAndMapJsonResponseToRegistration() {
        val httpClient = mock(HttpClient::class.java)
        val cut =
            ResidentApi(
                httpClient
            )

        cut.register()

        val requestCaptor = argumentCaptor<HttpRequest>()

        verify(httpClient).post(requestCaptor.capture(), any(), any())
        assertThat(requestCaptor.firstValue.urlPath).isEqualTo("/api/residents")
        assertThat(requestCaptor.firstValue.json.toString()).isEqualTo("{}")
    }

    @Test
    fun postATokenWhenRegistering() {
        val httpClient = mock(HttpClient::class.java)
        val cut =
            ResidentApi(
                httpClient
            )

        cut.register("some-token")

        val requestCaptor = argumentCaptor<HttpRequest>()

        verify(httpClient).post(requestCaptor.capture(), any(), any())
        assertThat(requestCaptor.firstValue.urlPath).isEqualTo("/api/devices/some-token")
    }

    @Test
    fun callTheSuccessCallbackInCaseOfSuccess() {
        val httpClient = mock(HttpClient::class.java)
        val cut =
            ResidentApi(
                httpClient
            )
        var expectedRegistration =
            Registration(
                "dummy",
                "dummy"
            )

        cut.register({ registration -> expectedRegistration = registration })

        val onSuccessCaptor = argumentCaptor<(JSONObject) -> Unit>()

        verify(httpClient).post(any(), onSuccessCaptor.capture(), any())

        onSuccessCaptor.firstValue.invoke(createJsonRegistration())
        assertThat(expectedRegistration.id).isEqualTo("00000000-0000-0000-0000-000000000001")
        assertThat(expectedRegistration.secretKey).isEqualTo("some secret key")
    }

    @Test
    fun callTheErrorCallbackInCaseOfException() {
        val httpClient = mock(HttpClient::class.java)
        val cut =
            ResidentApi(
                httpClient
            )
        var expectedError = Exception("something")

        cut.register({}, { error -> expectedError = error })

        val onErrorCaptor = argumentCaptor<(Exception) -> Unit>()
        verify(httpClient).post(any(), any(), onErrorCaptor.capture())

        onErrorCaptor.firstValue.invoke(Exception("boom"))
        assertThat(expectedError).hasMessage("boom")
    }

    @Test
    fun callThePostEndpointWithActivationCodeWhenConfirmingADevice() {
        val httpClient = mock(HttpClient::class.java)
        val cut =
            ResidentApi(
                httpClient
            )

        cut.confirmDevice("some-activation-code", {}, {})

        val requestCaptor = argumentCaptor<HttpRequest>()
        verify(httpClient).post(requestCaptor.capture(), any(), any())

        assertThat(requestCaptor.firstValue.urlPath).isEqualTo("/api/devices")
        assertThat(requestCaptor.firstValue.json["activationCode"]).isEqualTo("some-activation-code")
    }

    @Test
    fun returnARegistrationWhenConfirmingADevice() {
        val httpClient = mock(HttpClient::class.java)
        val cut =
            ResidentApi(
                httpClient
            )
        var actualRegistration: Registration? = null

        cut.confirmDevice("some-activation-code", { registration ->
            actualRegistration = registration
        }, {})

        val successCaptor = argumentCaptor<(JSONObject) -> Unit>()
        verify(httpClient).post(any(), successCaptor.capture(), any())

        successCaptor.firstValue.invoke(createJsonRegistration())

        assertThat(actualRegistration!!.id).isEqualTo("00000000-0000-0000-0000-000000000001")
        assertThat(actualRegistration!!.secretKey).isEqualTo("some secret key")
    }

    private fun createJsonRegistration(): JSONObject {
        val jsonRegistration = JSONObject()
            .put("id", "00000000-0000-0000-0000-000000000001")
            .put("secretKey", "some secret key")
        return jsonRegistration
    }
}
