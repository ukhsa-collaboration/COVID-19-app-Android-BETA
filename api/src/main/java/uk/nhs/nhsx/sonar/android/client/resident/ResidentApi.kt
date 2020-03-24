/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.resident

import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest

typealias ErrorCallback = (Exception) -> Unit

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

    private fun mapResponseToRegistration(jsonObject: JSONObject): Registration {
        val residentId = jsonObject.getString("id")
        val hmacKey = jsonObject.getString("hmacKey")

        return Registration(residentId, hmacKey)
    }
}
