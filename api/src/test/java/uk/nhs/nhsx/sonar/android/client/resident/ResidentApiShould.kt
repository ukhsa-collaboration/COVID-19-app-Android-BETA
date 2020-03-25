/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.resident

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage
import java.util.Base64

class ResidentApiShould {

    private val encryptionKeyStorage = mockk<EncryptionKeyStorage>(relaxed = true)
    private val httpClient = mockk<HttpClient>(relaxed = true)
    private val residentApi = ResidentApi(encryptionKeyStorage, httpClient)

    @Test
    fun postJsonToHttpClientAndMapJsonResponseToRegistration() {
        residentApi.register()

        val requestSlot = slot<HttpRequest>()
        verify { httpClient.post(capture(requestSlot), any(), any()) }

        assertThat(requestSlot.captured.urlPath).isEqualTo("/api/residents")
        assertThat(requestSlot.captured.json.toString()).isEqualTo("{}")
    }

    @Test
    fun postATokenWhenRegistering() {
        residentApi.register("some-token")

        val requestSlot = slot<HttpRequest>()
        verify { httpClient.post(capture(requestSlot), any(), any()) }

        assertThat(requestSlot.captured.urlPath).isEqualTo("/api/devices/registrations")
        assertThat(requestSlot.captured.json.get("pushToken")).isEqualTo("some-token")
    }

    @Test
    fun callTheSuccessCallbackInCaseOfSuccess() {
        var expectedRegistration: Registration? = null

        residentApi.register({ registration -> expectedRegistration = registration })

        val onSuccessSlot = slot<(JSONObject) -> Unit>()
        verify { httpClient.post(any(), capture(onSuccessSlot), any()) }

        onSuccessSlot.captured.invoke(jsonRegistration)
        assertThat(expectedRegistration?.id).isEqualTo("00000000-0000-0000-0000-000000000001")
    }

    @Test
    fun callTheErrorCallbackInCaseOfException() {
        var expectedError: Exception? = null

        residentApi.register({}, { error -> expectedError = error })

        val onErrorSlot = slot<(Exception) -> Unit>()
        verify { httpClient.post(any(), any(), capture(onErrorSlot)) }

        onErrorSlot.captured.invoke(Exception("boom"))
        assertThat(expectedError).hasMessage("boom")
    }

    @Test
    fun callThePostEndpointWithActivationCodeWhenConfirmingADevice() {
        residentApi.confirmDevice("some-activation-code", {}, {})

        val requestSlot = slot<HttpRequest>()
        verify { httpClient.post(capture(requestSlot), any(), any()) }

        assertThat(requestSlot.captured.urlPath).isEqualTo("/api/devices")
        assertThat(requestSlot.captured.json["activationCode"]).isEqualTo("some-activation-code")
    }

    @Test
    fun returnARegistrationWhenConfirmingADevice() {
        var actualRegistration: Registration? = null

        residentApi.confirmDevice("some-activation-code", { registration ->
            actualRegistration = registration
        }, {})

        val successSlot = slot<(JSONObject) -> Unit>()
        verify { httpClient.post(any(), capture(successSlot), any()) }

        successSlot.captured.invoke(jsonRegistration)
        assertThat(actualRegistration?.id).isEqualTo("00000000-0000-0000-0000-000000000001")
    }

    @Test
    fun callTheErrorCallbackInCaseOfExceptionWhenConfirmingADevice() {
        var expectedError: Exception? = null

        residentApi.confirmDevice("some-activation-code", {}, { error -> expectedError = error })

        val onErrorSlot = slot<(Exception) -> Unit>()
        verify { httpClient.post(any(), any(), capture(onErrorSlot)) }

        onErrorSlot.captured.invoke(Exception("boom"))
        assertThat(expectedError).hasMessage("boom")
    }

    @Test
    fun callPersistSecretKeyWhenConfirmingADevice() {
        residentApi.confirmDevice("some-activation-code", {}, {})

        val successCaptor = slot<(JSONObject) -> Unit>()
        verify { httpClient.post(any(), capture(successCaptor), any()) }

        successCaptor.captured.invoke(jsonRegistration)
        verify { encryptionKeyStorage.putBase64Key(base64EncodedSecretKey) }
    }

    private val jsonRegistration: JSONObject =
        JSONObject().apply {
            put("id", "00000000-0000-0000-0000-000000000001")
            put("secretKey", "some secret key")
        }

    private val base64EncodedSecretKey: String =
        Base64.getEncoder().encodeToString("some secret key".toByteArray())
}
