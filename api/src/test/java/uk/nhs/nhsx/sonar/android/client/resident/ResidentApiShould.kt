/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.resident

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage
import java.util.Base64

class ResidentApiShould {

    private val encryptionKeyStorage: EncryptionKeyStorage = mock()

    private val httpClient = mock(HttpClient::class.java)

    private val cut =
        ResidentApi(
            encryptionKeyStorage,
            httpClient
        )

    @Test
    fun postJsonToHttpClientAndMapJsonResponseToRegistration() {

        cut.register()

        val requestCaptor = argumentCaptor<HttpRequest>()

        verify(httpClient).post(requestCaptor.capture(), any(), any())
        assertThat(requestCaptor.firstValue.urlPath).isEqualTo("/api/residents")
        assertThat(requestCaptor.firstValue.json.toString()).isEqualTo("{}")
    }

    @Test
    fun postATokenWhenRegistering() {

        cut.register("some-token")

        val requestCaptor = argumentCaptor<HttpRequest>()

        verify(httpClient).post(requestCaptor.capture(), any(), any())
        assertThat(requestCaptor.firstValue.urlPath).isEqualTo("/api/devices/registrations")
        assertThat(requestCaptor.firstValue.json.get("pushToken")).isEqualTo("some-token")
    }

    @Test
    fun callTheSuccessCallbackInCaseOfSuccess() {

        var expectedRegistration =
            Registration(
                "dummy"
            )

        cut.register({ registration -> expectedRegistration = registration })

        val onSuccessCaptor = argumentCaptor<(JSONObject) -> Unit>()

        verify(httpClient).post(any(), onSuccessCaptor.capture(), any())

        onSuccessCaptor.firstValue.invoke(createJsonRegistration())
        assertThat(expectedRegistration.id).isEqualTo("00000000-0000-0000-0000-000000000001")
    }

    @Test
    fun callTheErrorCallbackInCaseOfException() {

        var expectedError = Exception("something")

        cut.register({}, { error -> expectedError = error })

        val onErrorCaptor = argumentCaptor<(Exception) -> Unit>()
        verify(httpClient).post(any(), any(), onErrorCaptor.capture())

        onErrorCaptor.firstValue.invoke(Exception("boom"))
        assertThat(expectedError).hasMessage("boom")
    }

    @Test
    fun callThePostEndpointWithActivationCodeWhenConfirmingADevice() {

        cut.confirmDevice("some-activation-code", {}, {})

        val requestCaptor = argumentCaptor<HttpRequest>()
        verify(httpClient).post(requestCaptor.capture(), any(), any())

        assertThat(requestCaptor.firstValue.urlPath).isEqualTo("/api/devices")
        assertThat(requestCaptor.firstValue.json["activationCode"]).isEqualTo("some-activation-code")
    }

    @Test
    fun returnARegistrationWhenConfirmingADevice() {

        var actualRegistration: Registration? = null

        cut.confirmDevice("some-activation-code", { registration ->
            actualRegistration = registration
        }, {})

        val successCaptor = argumentCaptor<(JSONObject) -> Unit>()
        verify(httpClient).post(any(), successCaptor.capture(), any())

        successCaptor.firstValue.invoke(createJsonRegistration())

        assertThat(actualRegistration!!.id).isEqualTo("00000000-0000-0000-0000-000000000001")
    }

    @Test
    fun callTheErrorCallbackInCaseOfExceptionWhenConfirmingADevice() {

        var expectedError = Exception("something")

        cut.confirmDevice("some-activation-code", {}, { error -> expectedError = error })

        val onErrorCaptor = argumentCaptor<(Exception) -> Unit>()
        verify(httpClient).post(any(), any(), onErrorCaptor.capture())

        onErrorCaptor.firstValue.invoke(Exception("boom"))
        assertThat(expectedError).hasMessage("boom")
    }

    @Test
    fun callPersistSecretKeyWhenConfirmingADevice() {

        cut.confirmDevice("some-activation-code", {}, {})

        val successCaptor = argumentCaptor<(JSONObject) -> Unit>()
        verify(httpClient).post(any(), successCaptor.capture(), any())
        successCaptor.firstValue.invoke(createJsonRegistration())

        verify(encryptionKeyStorage).putBase64Key(base64EncodedSecretKey())
    }

    private fun createJsonRegistration(): JSONObject {
        val jsonRegistration = JSONObject()
            .put("id", "00000000-0000-0000-0000-000000000001")
            .put("secretKey", base64EncodedSecretKey())
        return jsonRegistration
    }

    private fun base64EncodedSecretKey() =
        Base64.getEncoder().encodeToString("some secret key".toByteArray())
}
