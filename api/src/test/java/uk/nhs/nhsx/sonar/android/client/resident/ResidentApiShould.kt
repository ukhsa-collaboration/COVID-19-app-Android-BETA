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
import uk.nhs.nhsx.sonar.android.client.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.client.http.volley.TestQueue
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage
import uk.nhs.nhsx.sonar.android.client.test.assertBodyHasJson

class ResidentApiShould {

    private val encryptionKeyStorage = mockk<EncryptionKeyStorage>(relaxed = true)
    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val httpClient = VolleyHttpClient(baseUrl, requestQueue)
    private val residentApi = ResidentApi(encryptionKeyStorage, httpClient)

    @Test
    fun testRegister_Request() {
        residentApi.register("some-token", {}, {})

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("$baseUrl/api/devices/registrations")
        request.assertBodyHasJson(mapOf("pushToken" to "some-token"))
    }

    @Test
    fun testRegister_OnSuccess() {
        var success = false
        var error: Exception? = null

        residentApi.register("some-token", { success = true }, { error = it })
        requestQueue.returnSuccess(JSONObject())

        assertThat(success).isTrue()
        assertThat(error).isNull()
    }

    @Test
    fun testRegister_OnError() {
        var success = false
        var error: Exception? = null

        residentApi.register("some-token", { success = true }, { error = it })
        requestQueue.returnError(VolleyError("boom"))

        assertThat(success).isFalse()
        assertThat(error).isInstanceOf(VolleyError::class.java)
        assertThat(error).hasMessage("boom")
    }

    @Test
    fun testConfirmDevice_Request() {
        residentApi.confirmDevice("some-activation-code", "firebase-token-001", {}, {})

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("$baseUrl/api/devices")
        request.assertBodyHasJson(
            mapOf(
                "activationCode" to "some-activation-code",
                "pushToken" to "firebase-token-001"
            )
        )
    }

    @Test
    fun testConfirmDevice_OnSuccess() {
        var registration: Registration? = null
        var error: Exception? = null

        val jsonResponse = jsonObjectOf(
            "id" to "00000000-0000-0000-0000-000000000001",
            "secretKey" to "some-secret-key-base64-encoded"
        )
        residentApi.confirmDevice("some-activation-code", "firebase-token", { registration = it }, { error = it })
        requestQueue.returnSuccess(jsonResponse)

        assertThat(registration).isEqualTo(Registration("00000000-0000-0000-0000-000000000001"))
        assertThat(error).isNull()
        verify { encryptionKeyStorage.putBase64Key("some-secret-key-base64-encoded") }
    }

    @Test
    fun testConfirmDevice_OnError() {
        var registration: Registration? = null
        var error: Exception? = null

        residentApi.confirmDevice("some-activation-code", "firebase-token", { registration = it }, { error = it })
        requestQueue.returnError(VolleyError("boom"))

        assertThat(registration).isNull()
        assertThat(error).isInstanceOf(VolleyError::class.java)
        assertThat(error).hasMessage("boom")
    }
}
