/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client

import org.json.JSONObject
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpMethod.PATCH
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.http.Promise
import uk.nhs.nhsx.sonar.android.client.http.jsonObjectOf

class CoLocationApi(
    private val baseUrl: String,
    private val keyStorage: KeyStorage,
    private val httpClient: HttpClient
) {

    fun save(coLocationData: CoLocationData): Promise<Unit> {
        val request = HttpRequest(
            method = PATCH,
            url = "$baseUrl/api/residents/${coLocationData.sonarId}",
            jsonBody = coLocationData.toJson(),
            key = keyStorage.provideSecretKey()!!
        )
        Timber.i("Sending $coLocationData")

        return httpClient.send(request).mapToUnit()
    }
}

data class CoLocationData(
    val sonarId: String,
    val symptomsTimestamp: String,
    val contactEvents: List<CoLocationEvent>
)

data class CoLocationEvent(
    val sonarId: String? = null,
    val encryptedRemoteContactId: String? = null,
    val rssiValues: List<Int>,
    val rssiOffsets: List<Int>,
    val timestamp: String,
    val duration: Int
)

private fun CoLocationData.toJson(): JSONObject =
    jsonObjectOf(
        "symptomsTimestamp" to symptomsTimestamp,
        "contactEvents" to contactEvents.map {
            if (it.sonarId != null) mapOf(
                "sonarId" to it.sonarId,
                "rssiValues" to it.rssiValues,
                "rssiOffsets" to it.rssiOffsets,
                "timestamp" to it.timestamp,
                "duration" to it.duration
            )
            else mapOf(
                "encryptedRemoteContactId" to it.encryptedRemoteContactId,
                "rssiValues" to it.rssiValues,
                "rssiOffsets" to it.rssiOffsets,
                "timestamp" to it.timestamp,
                "duration" to it.duration
            )
        }
    )
