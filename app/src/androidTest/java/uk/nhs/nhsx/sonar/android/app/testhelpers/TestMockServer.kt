/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.util.Base64
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import uk.nhs.nhsx.sonar.android.app.http.jsonOf
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class TestMockServer {

    private val testDispatcher = TestSonarServiceDispatcher()

    private val mockServer = MockWebServer().apply {
        dispatcher = testDispatcher
        start(43239)
    }

    fun url(): String =
        mockServer.url("").toString().removeSuffix("/")

    fun shutdown() {
        mockServer.shutdown()
    }

    fun simulateBackendResponse(error: Boolean) {
        testDispatcher.simulateResponse(error)
    }

    fun simulateBackendDelay(delayInMillis: Long) {
        testDispatcher.simulateDelay(delayInMillis)
    }

    fun verifyReceivedRegistrationRequest() {
        // WorkManager is responsible for starting registration process and unfortunately it is not exact
        // Have to wait for longer time (usually less than 10 seconds). Putting 20 secs just to be sure
        var lastRequest = mockServer.takeRequest(20_000, TimeUnit.MILLISECONDS)

        if (lastRequest?.path?.contains("linking-id") == true) {
            lastRequest = mockServer.takeRequest()
        }

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices/registrations")
        assertThat(lastRequest?.body?.readUtf8()).isEqualTo("""{"pushToken":"test firebase token #010"}""")
    }

    fun verifyReceivedActivationRequest() {
        // WorkManager is responsible for starting registration process and unfortunately it is not exact
        // Have to wait for longer time (usually less than 10 seconds). Putting 20 secs just to be sure
        val lastRequest = mockServer.takeRequest(20_000, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices")
        assertThat(lastRequest?.body?.readUtf8())
            .contains("""{"activationCode":"test activation code #001","pushToken":"test firebase token #010",""")
    }

    fun verifyReceivedProximityRequest(proximityEvent: TestProximityEvent) {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.path).isEqualTo("/api/proximity-events/upload")
        assertThat(lastRequest?.method).isEqualTo("PATCH")

        val body = lastRequest?.body?.readUtf8() ?: ""
        assertThat(body).contains(""""symptomsTimestamp":""")
        assertThat(body).contains(""""contactEvents":[""")
        assertThat(body).contains(""""symptoms":[""")
        assertThat(body).contains("TEMPERATURE", "COUGH", "ANOSMIA", "SNEEZE", "NAUSEA")
        val rssiValues = listOf(10, 20, 15).map { it.toByte() }.toByteArray()
        assertThat(body).contains(
            jsonOf(
                "encryptedRemoteContactId" to Base64.encodeToString(
                    proximityEvent.firstDeviceId.cryptogram.asBytes(),
                    Base64.DEFAULT
                ),
                "rssiValues" to Base64.encodeToString(
                    rssiValues,
                    Base64.DEFAULT
                ),
                "rssiIntervals" to listOf(0, 90, 610),
                "timestamp" to "2020-04-01T14:33:13Z",
                "duration" to 700,
                "txPowerInProtocol" to -6,
                "txPowerAdvertised" to -5,
                "hmacSignature" to Base64.encodeToString(proximityEvent.firstDeviceSignature, Base64.DEFAULT),
                "transmissionTime" to proximityEvent.transmissionTime,
                "countryCode" to ByteBuffer.wrap(proximityEvent.countryCode).short
            )
        )
        assertThat(body).contains(
            jsonOf(
                "encryptedRemoteContactId" to Base64.encodeToString(
                    proximityEvent.secondDeviceId.cryptogram.asBytes(),
                    Base64.DEFAULT
                ),
                "rssiValues" to Base64.encodeToString(
                    byteArrayOf(40.toByte()),
                    Base64.DEFAULT
                ),
                "rssiIntervals" to listOf(0),
                "timestamp" to "2020-04-01T14:34:43Z",
                "duration" to 60,
                "txPowerInProtocol" to -8,
                "txPowerAdvertised" to -1,
                "hmacSignature" to Base64.encodeToString(proximityEvent.secondDeviceSignature, Base64.DEFAULT),
                "transmissionTime" to proximityEvent.transmissionTime + 90,
                "countryCode" to ByteBuffer.wrap(proximityEvent.countryCode).short

            )
        )
        assertThat(countOccurrences(body, """{"encryptedRemoteContactId":""")).isEqualTo(2)
    }

    private fun countOccurrences(str: String, substring: String): Int =
        if (!str.contains(substring)) {
            0
        } else {
            1 + countOccurrences(str.replaceFirst(substring, ""), substring)
        }
}
