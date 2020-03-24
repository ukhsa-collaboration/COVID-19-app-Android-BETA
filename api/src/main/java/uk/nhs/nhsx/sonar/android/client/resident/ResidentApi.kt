/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.resident

import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import java.lang.Exception

typealias SuccessCallback = (Registration) -> Unit
typealias ErrorCallback = (Exception) -> Unit

class ResidentApi(private val httpClient: HttpClient) {

    fun register(onSuccess: SuccessCallback = {}, onError: ErrorCallback = {}) {
        httpClient.post(HttpRequest("/api/residents", JSONObject()), {
            json: JSONObject -> onSuccess(mapResponseToRegistration(json))
        }, {
            error: Exception -> onError(error)
        })
    }

    fun register(token: String, onSuccess: SuccessCallback = {}, onError: ErrorCallback = {}) {

    }

    private fun mapResponseToRegistration(jsonObject: JSONObject): Registration {
        val residentId = jsonObject.getString("id")
        val hmacKey = jsonObject.getString("hmacKey")

        return Registration(
            residentId,
            hmacKey
        )
    }
}
