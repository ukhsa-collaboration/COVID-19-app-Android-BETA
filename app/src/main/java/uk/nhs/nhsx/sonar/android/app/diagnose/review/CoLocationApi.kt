/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose.review

import org.json.JSONObject
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.HttpMethod.PATCH
import uk.nhs.nhsx.sonar.android.app.http.HttpRequest
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.http.Promise
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf

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
    val encryptedRemoteContactId: String,
    val rssiValues: List<Int>,
    val rssiOffsets: List<Int>,
    val timestamp: String,
    val duration: Int
)

private fun CoLocationData.toJson(): JSONObject =
    jsonObjectOf(
        "symptomsTimestamp" to symptomsTimestamp,
        "contactEvents" to contactEvents.map {
            mapOf(
                "encryptedRemoteContactId" to it.encryptedRemoteContactId,
                "rssiValues" to it.rssiValues,
                "rssiOffsets" to it.rssiOffsets,
                "timestamp" to it.timestamp,
                "duration" to it.duration
            )
        }
    )
