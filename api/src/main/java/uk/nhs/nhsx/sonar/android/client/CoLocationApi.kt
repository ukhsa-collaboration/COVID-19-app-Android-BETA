/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client

import android.util.Log
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpMethod.PATCH
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.http.Promise
import uk.nhs.nhsx.sonar.android.client.http.jsonObjectOf

class CoLocationApi(
    private val baseUrl: String,
    private val keyStorage: EncryptionKeyStorage,
    private val httpClient: HttpClient
) {

    fun save(coLocationData: CoLocationData): Promise<Unit> {
        val request = HttpRequest(
            method = PATCH,
            url = "$baseUrl/api/residents/${coLocationData.sonarId}",
            jsonBody = coLocationData.toJson(),
            key = keyStorage.provideKey()!!
        )
        Log.i("Sending", "Sending $coLocationData")

        return httpClient.send(request).mapToUnit()
    }
}

data class CoLocationData(
    val sonarId: String,
    val contactEvents: List<CoLocationEvent>
)

data class CoLocationEvent(
    val sonarId: String,
    val rssiValues: List<Int>,
    val timestamp: String,
    val duration: Int
)

private fun CoLocationData.toJson(): JSONObject =
    jsonObjectOf(
        "contactEvents" to contactEvents.map {
            mapOf(
                "sonarId" to it.sonarId,
                "rssiValues" to it.rssiValues,
                "timestamp" to it.timestamp,
                "duration" to it.duration
            )
        }
    )
