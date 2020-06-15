/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.util.Base64
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import timber.log.Timber
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class TestSonarServiceDispatcher : Dispatcher() {

    private var delay = 0L
    private var shouldSimulateError = false

    companion object {
        val SECRET_KEY: SecretKey = KeyGenerator.getInstance("HMACSHA256").generateKey()
        const val PUBLIC_KEY: String = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEu1f68MqDXbKeTqZMTHsOGToO4rKnPClXe/kE+oWqlaWZQv4J1E98cUNdpzF9JIFRPMCNdGOvTr4UB+BhQv9GWg=="
        const val RESIDENT_ID: String = "1ae5d70d-0c40-4af2-bac0-c2d18d25091f"
        const val REFERENCE_CODE: String = "REF CODE #202"

        val encodedSecretKey = Base64
            .encode(SECRET_KEY.encoded, Base64.DEFAULT)
            .toString(Charset.defaultCharset())
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
        val response =
            if (shouldSimulateError) {
                MockResponse().setResponseCode(500)
            } else {
                when (request.path) {
                    "/api/devices/registrations" -> MockResponse()
                    "/api/devices" -> MockResponse().apply {
                        setBody("""{"id":"$RESIDENT_ID","secretKey":"$encodedSecretKey","publicKey":"$PUBLIC_KEY"}""")
                    }
                    "/api/proximity-events/upload" -> MockResponse()
                    "/api/app-instances/linking-id" -> MockResponse().apply {
                        setBody("""{"linkingId":"$REFERENCE_CODE"}""")
                    }
                    else -> MockResponse().apply {
                        setBody("Unexpected request reached TestSonarServiceDispatcher class")
                        setResponseCode(500)
                    }
                }
            }

        response.setHeadersDelay(delay, TimeUnit.MILLISECONDS)

        Timber.d("Mock Request: $request")
        Timber.d("Mock Response: $response")

        return response
    }

    fun simulateResponse(error: Boolean) {
        shouldSimulateError = error
    }

    fun simulateDelay(delayInMillis: Long) {
        delay = delayInMillis
    }
}
