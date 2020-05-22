/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import com.android.volley.Request.Method.PATCH
import com.android.volley.VolleyError
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationData
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationEvent
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.http.PromiseAssert.Companion.assertThat
import uk.nhs.nhsx.sonar.android.app.http.RequestAssert.Companion.assertThat
import uk.nhs.nhsx.sonar.android.app.http.TestQueue
import uk.nhs.nhsx.sonar.android.app.http.generateSignatureKey
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import java.util.Base64

class CoLocationApiTest {

    private val encryptionKeyStorage = object : KeyStorage by mockk() {
        override fun provideSecretKey() = generateSignatureKey()
    }
    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val httpClient = HttpClient(
        requestQueue,
        "someValue",
        "buildInfo"
    )
    private val coLocationApi = CoLocationApi(baseUrl, encryptionKeyStorage, httpClient)

    @Test
    fun `test save() request`() {
        val events = listOf(
            CoLocationEvent(
                encryptedRemoteContactId = "001",
                rssiValues = Base64.getEncoder().encodeToString(
                    byteArrayOf(0, (-10).toByte())
                ),
                rssiIntervals = listOf(0, 6),
                timestamp = "2s ago",
                duration = 10,
                txPowerInProtocol = (-9).toByte(),
                txPowerAdvertised = (-5).toByte(),
                hmacSignature = Base64.getEncoder().encodeToString(ByteArray(16) { 1 }),
                countryCode = 1.toShort(),
                transmissionTime = 1
            ),
            CoLocationEvent(
                encryptedRemoteContactId = "002",
                rssiValues = Base64.getEncoder().encodeToString(
                    byteArrayOf(
                        (-10).toByte(),
                        (-10).toByte(),
                        (-10).toByte()
                    )
                ),
                rssiIntervals = listOf(0, 5, 20),
                timestamp = "yesterday",
                duration = 120,
                txPowerInProtocol = (-4).toByte(),
                txPowerAdvertised = (-8).toByte(),
                hmacSignature = Base64.getEncoder().encodeToString(ByteArray(16) { 2 }),
                countryCode = 2.toShort(),
                transmissionTime = 2
            )
        )

        val symptoms = listOf(Symptom.NAUSEA, Symptom.ANOSMIA)
        val promise =
            coLocationApi.save(CoLocationData("::sonar-id::", "::timestamp::", symptoms, events))

        assertThat(promise).isInProgress()

        val request = requestQueue.lastRequest
        assertThat(request.method).isEqualTo(PATCH)
        assertThat(request.url).isEqualTo("$baseUrl/api/proximity-events/upload")
        assertThat(request).bodyHasJson(
            "sonarId" to "::sonar-id::",
            "symptomsTimestamp" to "::timestamp::",
            "symptoms" to symptoms.map { it.value },
            "contactEvents" to listOf(
                mapOf(
                    "encryptedRemoteContactId" to "001",
                    "rssiValues" to Base64.getEncoder().encodeToString(
                        byteArrayOf(0, (-10).toByte())
                    ),
                    "rssiIntervals" to listOf(0, 6),
                    "timestamp" to "2s ago",
                    "duration" to 10,
                    "txPowerInProtocol" to -9,
                    "txPowerAdvertised" to -5,
                    "countryCode" to 1,
                    "hmacSignature" to Base64.getEncoder().encodeToString(ByteArray(16) { 1 }),
                    "transmissionTime" to 1
                ),
                mapOf(
                    "encryptedRemoteContactId" to "002",
                    "rssiValues" to Base64.getEncoder().encodeToString(
                        byteArrayOf(
                            (-10).toByte(),
                            (-10).toByte(),
                            (-10).toByte()
                        )
                    ),
                    "rssiIntervals" to listOf(0, 5, 20),
                    "timestamp" to "yesterday",
                    "duration" to 120,
                    "txPowerInProtocol" to -4,
                    "txPowerAdvertised" to -8,
                    "countryCode" to 2,
                    "hmacSignature" to Base64.getEncoder().encodeToString(ByteArray(16) { 2 }),
                    "transmissionTime" to 2
                )
            )
        )
    }

    @Test
    fun `test save() on success`() {
        val promise =
            coLocationApi.save(
                CoLocationData(
                    "::sonar-id::",
                    "::timestamp::",
                    emptyList(),
                    emptyList()
                )
            )

        requestQueue.returnSuccess(JSONObject())
        assertThat(promise).succeeded()
    }

    @Test
    fun `test save() on error`() {
        val promise =
            coLocationApi.save(
                CoLocationData(
                    "::sonar-id::",
                    "::timestamp::",
                    emptyList(),
                    emptyList()
                )
            )

        requestQueue.returnError(VolleyError())
        assertThat(promise).failed()
    }
}
