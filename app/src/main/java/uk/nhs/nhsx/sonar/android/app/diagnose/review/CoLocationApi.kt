/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose.review

import org.json.JSONObject
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.HttpMethod.PATCH
import uk.nhs.nhsx.sonar.android.app.http.HttpRequest
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.app.status.Symptom

class CoLocationApi(
    private val baseUrl: String,
    private val keyStorage: SecretKeyStorage,
    private val httpClient: HttpClient
) {

    fun save(coLocationData: CoLocationData): Promise<Unit> {
        val request = HttpRequest(
            method = PATCH,
            url = "$baseUrl/api/proximity-events/upload",
            jsonBody = coLocationData.toJson(),
            secretKey = keyStorage.provideSecretKey()!!
        )
        Timber.d("Sending ${coLocationData.toJson()}")

        return httpClient.send(request).mapToUnit()
    }
}

data class CoLocationData(
    val sonarId: String,
    val symptomsTimestamp: String,
    val symptoms: List<Symptom>,
    val contactEvents: List<CoLocationEvent>
)

data class CoLocationEvent(
    val encryptedRemoteContactId: String,
    val rssiValues: String,
    val rssiIntervals: List<Int>,
    val timestamp: String,
    val duration: Int,
    val txPowerInProtocol: Byte,
    val txPowerAdvertised: Byte,
    val countryCode: Short,
    val transmissionTime: Int,
    val hmacSignature: String
)

fun CoLocationData.toJson(): JSONObject =
    jsonObjectOf(
        "sonarId" to sonarId,
        "symptomsTimestamp" to symptomsTimestamp,
        "symptoms" to convertSymptoms(symptoms),
        "contactEvents" to contactEvents.map {
            mapOf(
                "encryptedRemoteContactId" to it.encryptedRemoteContactId,
                "rssiValues" to it.rssiValues,
                "rssiIntervals" to it.rssiIntervals,
                "timestamp" to it.timestamp,
                "duration" to it.duration,
                "txPowerInProtocol" to it.txPowerInProtocol,
                "txPowerAdvertised" to it.txPowerAdvertised,
                "hmacSignature" to it.hmacSignature,
                "transmissionTime" to it.transmissionTime,
                "countryCode" to it.countryCode
            )
        }
    )

fun convertSymptoms(symptoms: List<Symptom>) = symptoms.map { it.value }
