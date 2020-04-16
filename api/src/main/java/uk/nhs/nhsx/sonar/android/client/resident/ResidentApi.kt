/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.resident

import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpMethod.POST
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.http.Promise
import uk.nhs.nhsx.sonar.android.client.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage
import javax.inject.Inject

// Register endpoint: POST /api/devices/registrations -d '{ pushToken: "base64(probably)-blabla" }' --> 204 - No Content
// Confirm registration endpoint:
// POST /api/devices
// -d '{ "activationCode": "uuid-blabla..." }'
// -> 200 { "id": "uuid-blabalabla", "secretKey": "base 64 encoded hmac compatible key" }

class ResidentApi @Inject constructor(
    private val baseUrl: String,
    private val encryptionKeyStorage: EncryptionKeyStorage,
    private val httpClient: HttpClient
) {

    fun register(token: String): Promise<Unit> {
        val requestJson = jsonObjectOf("pushToken" to token)
        val request = HttpRequest(POST, "$baseUrl/api/devices/registrations", requestJson)

        return httpClient.send(request).mapToUnit()
    }

    fun confirmDevice(deviceConfirmation: DeviceConfirmation): Promise<Registration> {
        val requestJson = jsonObjectOf(
            "activationCode" to deviceConfirmation.activationCode,
            "pushToken" to deviceConfirmation.pushToken,
            "deviceModel" to deviceConfirmation.deviceModel,
            "deviceOSVersion" to deviceConfirmation.deviceOsVersion,
            "postalCode" to deviceConfirmation.postalCode
        )
        val request = HttpRequest(POST, "$baseUrl/api/devices", requestJson)

        return httpClient
            .send(request)
            .map { json: JSONObject ->
                val key = json.getString("secretKey")
                val registrationId = json.getString("id")

                encryptionKeyStorage.putBase64Key(key)
                Registration(registrationId)
            }
    }
}

data class Registration(val id: String)

data class DeviceConfirmation(
    val activationCode: String,
    val pushToken: String,
    val deviceModel: String,
    val deviceOsVersion: String,
    val postalCode: String
)
