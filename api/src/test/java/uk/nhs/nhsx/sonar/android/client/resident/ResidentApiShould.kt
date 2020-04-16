/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.resident

import com.android.volley.VolleyError
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.assertBodyHasJson
import uk.nhs.nhsx.sonar.android.client.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.client.http.volley.TestQueue
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage

class ResidentApiShould {

    private val encryptionKeyStorage = mockk<EncryptionKeyStorage>(relaxed = true)
    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val httpClient = VolleyHttpClient(requestQueue)
    private val residentApi = ResidentApi(baseUrl, encryptionKeyStorage, httpClient)

    @Test
    fun testRegister_Request() {
        val promise = residentApi.register("some-token")

        assertThat(promise.isInProgress).isTrue()

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("$baseUrl/api/devices/registrations")
        request.assertBodyHasJson("pushToken" to "some-token")
    }

    @Test
    fun testRegister_OnSuccess() {
        val promise = residentApi.register("some-token")
        requestQueue.returnSuccess(JSONObject())

        assertThat(promise.isSuccess).isTrue()
    }

    @Test
    fun testRegister_OnError() {
        val promise = residentApi.register("some-token")
        requestQueue.returnError(VolleyError("boom"))

        assertThat(promise.isFailed).isTrue()
        assertThat(promise.error).isInstanceOf(VolleyError::class.java)
        assertThat(promise.error).hasMessage("boom")
    }

    @Test
    fun testConfirmDevice_Request() {
        val confirmation = DeviceConfirmation(
            activationCode = "::activation code::",
            pushToken = "::push token::",
            deviceModel = "::device model::",
            deviceOsVersion = "::device os version::",
            postalCode = "::postal code::"
        )

        val promise = residentApi.confirmDevice(confirmation)
        assertThat(promise.isInProgress).isTrue()

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("$baseUrl/api/devices")
        request.assertBodyHasJson(
            "activationCode" to "::activation code::",
            "pushToken" to "::push token::",
            "deviceModel" to "::device model::",
            "deviceOSVersion" to "::device os version::",
            "postalCode" to "::postal code::"
        )
    }

    @Test
    fun testConfirmDevice_OnSuccess() {
        val confirmation = DeviceConfirmation(
            activationCode = "::activation code::",
            pushToken = "::push token::",
            deviceModel = "::device model::",
            deviceOsVersion = "::device os version::",
            postalCode = "::postal code::"
        )
        val jsonResponse = jsonObjectOf(
            "id" to "00000000-0000-0000-0000-000000000001",
            "secretKey" to "some-secret-key-base64-encoded"
        )

        val promise = residentApi.confirmDevice(confirmation)
        requestQueue.returnSuccess(jsonResponse)

        assertThat(promise.isSuccess).isTrue()
        assertThat(promise.value).isEqualTo(Registration("00000000-0000-0000-0000-000000000001"))
        verify { encryptionKeyStorage.putBase64Key("some-secret-key-base64-encoded") }
    }

    @Test
    fun testConfirmDevice_OnError() {
        val confirmation = DeviceConfirmation(
            activationCode = "::activation code::",
            pushToken = "::push token::",
            deviceModel = "::device model::",
            deviceOsVersion = "::device os version::",
            postalCode = "::postal code::"
        )

        val promise = residentApi.confirmDevice(confirmation)
        requestQueue.returnError(VolleyError("boom"))

        assertThat(promise.isFailed).isTrue()
        assertThat(promise.error).isInstanceOf(VolleyError::class.java)
        assertThat(promise.error).hasMessage("boom")
    }
}
