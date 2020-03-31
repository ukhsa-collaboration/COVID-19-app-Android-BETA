package com.example.colocate

import android.util.Base64
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.nio.charset.Charset

class TestCoLocateServiceDispatcher : Dispatcher() {

    companion object {
        const val SECRET_KEY: String = "secret key from TestCoLocateServiceDispatcher"
        const val RESIDENT_ID: String = "resident #001"

        val encodedKey = Base64
            .encode(SECRET_KEY.toByteArray(), Base64.DEFAULT)
            .toString(Charset.defaultCharset())
    }

    override fun dispatch(request: RecordedRequest): MockResponse =
        when (request.path) {
            "/api/devices/registrations" -> MockResponse()
            "/api/devices" -> MockResponse().apply {
                setBody("""{"id":"$RESIDENT_ID","secretKey":"$encodedKey"}""")
            }
            else -> MockResponse().apply {
                setBody("Unexpected request reached TestCoLocateServiceDispatcher class")
                setResponseCode(500)
            }
        }
}
