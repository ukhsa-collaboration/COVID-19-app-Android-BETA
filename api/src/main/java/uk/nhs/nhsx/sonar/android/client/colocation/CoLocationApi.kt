/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import android.util.Log
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage
import javax.inject.Inject

class CoLocationApi @Inject constructor(
    private val keyStorage: EncryptionKeyStorage,
    private val httpClient: HttpClient
) {

    fun save(
        coLocationData: CoLocationData,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val request = HttpRequest(
            "/api/residents/${coLocationData.residentId}",
            coLocationData.toJson(),
            keyStorage.provideKey()
        )
        Log.i("Sending", "Sending $coLocationData")
        httpClient.patch(request, { onSuccess() }, { exception -> onError(exception) })
    }
}

typealias Seconds = Long

data class CoLocationData(
    val residentId: String,
    val contactEvents: List<CoLocationEvent>
)

data class CoLocationEvent(
    val sonarId: String,
    val rssiValues: List<Int>,
    val timestamp: String,
    val duration: Seconds
)

private fun CoLocationData.toJson(): JSONObject =
    JSONObject(mapOf(
        "contactEvents" to contactEvents.map {
            mapOf(
                "sonarId" to it.sonarId,
                "rssiValues" to it.rssiValues,
                "timestamp" to it.timestamp,
                "duration" to it.duration
            )
        }
    ))
