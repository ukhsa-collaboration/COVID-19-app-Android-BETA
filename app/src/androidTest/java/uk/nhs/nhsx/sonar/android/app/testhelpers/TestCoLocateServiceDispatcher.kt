package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.util.Base64
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class TestCoLocateServiceDispatcher : Dispatcher() {

    private var delay = 0L
    private var shouldSimulateError = false

    companion object {
        const val SECRET_KEY: String = "secret key from TestCoLocateServiceDispatcher"
        const val RESIDENT_ID: String = "1ae5d70d-0c40-4af2-bac0-c2d18d25091f"

        val encodedKey = Base64
            .encode(SECRET_KEY.toByteArray(), Base64.DEFAULT)
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
                        setBody("""{"id":"$RESIDENT_ID","secretKey":"$encodedKey"}""")
                    }
                    "/api/residents/$RESIDENT_ID" -> MockResponse()
                    else -> MockResponse().apply {
                        setBody("Unexpected request reached TestCoLocateServiceDispatcher class")
                        setResponseCode(500)
                    }
                }
            }

        response.setHeadersDelay(delay, TimeUnit.MILLISECONDS)

        return response
    }

    fun simulateResponse(error: Boolean) {
        shouldSimulateError = error
    }

    fun simulateDelay(delayInMillis: Long) {
        delay = delayInMillis
    }
}
