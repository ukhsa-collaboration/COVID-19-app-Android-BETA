/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import com.android.volley.VolleyError
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.http.PromiseAssert.Companion.assertThat
import uk.nhs.nhsx.sonar.android.app.http.RequestAssert.Companion.assertThat
import uk.nhs.nhsx.sonar.android.app.http.TestQueue
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf

class ResidentApiTest {

    private val encryptionKeyStorage = mockk<KeyStorage>(relaxed = true)
    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val httpClient = HttpClient(
        queue = requestQueue,
        sonarHeaderValue = "someValue",
        appVersion = "buildInfo"
    )
    private val residentApi = ResidentApi(baseUrl, encryptionKeyStorage, httpClient)

    @Test
    fun `test register() request`() {
        val promise = residentApi.register("some-token")

        assertThat(promise).isInProgress()

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("$baseUrl/api/devices/registrations")
        assertThat(request).bodyHasJson("pushToken" to "some-token")
    }

    @Test
    fun `test register() on success`() {
        val promise = residentApi.register("some-token")
        requestQueue.returnSuccess(JSONObject())

        assertThat(promise).succeeded()
    }

    @Test
    fun `test register() on error`() {
        val promise = residentApi.register("some-token")
        requestQueue.returnError(VolleyError("boom"))

        assertThat(promise).failedWith<VolleyError>("boom")
    }

    @Test
    fun `test confirmDevice() request`() {
        val confirmation = DeviceConfirmation(
            activationCode = "::activation code::",
            pushToken = "::push token::",
            deviceModel = "::device model::",
            deviceOsVersion = "::device os version::",
            postalCode = "::postal code::"
        )

        val promise = residentApi.confirmDevice(confirmation)
        assertThat(promise).isInProgress()

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("$baseUrl/api/devices")
        assertThat(request).bodyHasJson(
            "activationCode" to "::activation code::",
            "pushToken" to "::push token::",
            "deviceModel" to "::device model::",
            "deviceOSVersion" to "::device os version::",
            "postalCode" to "::postal code::"
        )
    }

    @Test
    fun `test confirmDevice() on success`() {
        val confirmation = DeviceConfirmation(
            activationCode = "::activation code::",
            pushToken = "::push token::",
            deviceModel = "::device model::",
            deviceOsVersion = "::device os version::",
            postalCode = "::postal code::"
        )
        val jsonResponse = jsonObjectOf(
            "id" to "00000000-0000-0000-0000-000000000001",
            "secretKey" to "some-secret-key-base64-encoded",
            "publicKey" to "some-public-key-base64-encoded"
        )

        val promise = residentApi.confirmDevice(confirmation)
        requestQueue.returnSuccess(jsonResponse)

        assertThat(promise).succeededWith(Registration("00000000-0000-0000-0000-000000000001"))
        verify { encryptionKeyStorage.storeSecretKey("some-secret-key-base64-encoded") }
        verify { encryptionKeyStorage.storeServerPublicKey("some-public-key-base64-encoded") }
    }

    @Test
    fun `test confirmDevice() on error`() {
        val confirmation = DeviceConfirmation(
            activationCode = "::activation code::",
            pushToken = "::push token::",
            deviceModel = "::device model::",
            deviceOsVersion = "::device os version::",
            postalCode = "::postal code::"
        )

        val promise = residentApi.confirmDevice(confirmation)
        requestQueue.returnError(VolleyError("boom"))

        assertThat(promise).failedWith<VolleyError>("boom")
    }
}
