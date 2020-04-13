/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import android.util.Log
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.ErrorCallback
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.http.SimpleCallback
import uk.nhs.nhsx.sonar.android.client.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage

class CoLocationApi(
    private val baseUrl: String,
    private val keyStorage: EncryptionKeyStorage,
    private val httpClient: HttpClient
) {

    fun save(
        coLocationData: CoLocationData,
        onSuccess: SimpleCallback,
        onError: ErrorCallback
    ) {
        val request = HttpRequest(
            "$baseUrl/api/residents/${coLocationData.sonarId}",
            coLocationData.toJson(),
            keyStorage.provideKey()
        )
        Log.i("Sending", "Sending $coLocationData")
        httpClient.patch(request, { onSuccess() }, onError)
    }
}

typealias Seconds = Long

data class CoLocationData(
    val sonarId: String,
    val contactEvents: List<CoLocationEvent>
)

data class CoLocationEvent(
    val sonarId: String,
    val rssiValues: List<Int>,
    val timestamp: String,
    val duration: Seconds
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
