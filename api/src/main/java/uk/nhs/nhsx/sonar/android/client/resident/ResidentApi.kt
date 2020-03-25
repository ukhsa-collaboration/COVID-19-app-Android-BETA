/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.resident

import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest

typealias ErrorCallback = (Exception) -> Unit

// Register endpoint: POST /api/devices/registrations -d '{ pushToken: "base64(probably)-blabla" }' --> 204 - No Content
// Confirm registration endpoint:
// POST /api/devices
// -d '{ "activationCode": "uuid-blabla..." }'
// -> 200 { "id": "uuid-blabalabla", "secretKey": "base 64 encoded hmac compatible key" }

class ResidentApi(private val httpClient: HttpClient) {

    fun register(onSuccess: (Registration) -> Unit = {}, onError: ErrorCallback = {}) {
        val request = HttpRequest("/api/residents", JSONObject())

        httpClient.post(
            request,
            { json -> onSuccess(mapResponseToRegistration(json)) },
            { exception -> onError(exception) }
        )
    }

    fun register(token: String, onSuccess: () -> Unit = {}, onError: ErrorCallback = {}) {
        val request = HttpRequest("/api/devices/$token", JSONObject())

        httpClient.post(
            request,
            { onSuccess() },
            { exception -> onError(exception) }
        )
    }

    fun confirmDevice(
        activationCode: String,
        onSuccess: (Registration) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val json = JSONObject()
        json.put("activationCode", activationCode)
        val request = HttpRequest("/api/devices", json)
        httpClient.post(request, {
            json -> onSuccess(mapResponseToRegistration(json))
        }, {})
    }

    private fun mapResponseToRegistration(jsonObject: JSONObject): Registration {
        val residentId = jsonObject.getString("id")
        val hmacKey = jsonObject.getString("secretKey")

        return Registration(residentId, hmacKey)
    }
}
