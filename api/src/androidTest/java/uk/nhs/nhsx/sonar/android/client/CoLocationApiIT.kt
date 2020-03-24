package uk.nhs.nhsx.sonar.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.awaitility.kotlin.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationData
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import java.security.NoSuchAlgorithmException
import javax.crypto.KeyGenerator


@RunWith(AndroidJUnit4::class)
class CoLocationApiIT {
    lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start(8089)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun shouldSendCoLocationData() {

        server.enqueue(
            MockResponse().setResponseCode(200)
        )

        var called = false
        CoLocationApi(
            generateKey(),
            VolleyHttpClient(
                "http://localhost:8089",
                InstrumentationRegistry.getInstrumentation().targetContext
            )
            )
            .save(CoLocationData("residentId"), {called = true})

        await.until { called }

        val request1 = server.takeRequest()
        assertEquals("/api/residents/residentId", request1.path)
    }

    fun generateKey() : ByteArray {
        try {
            return KeyGenerator.getInstance("HMACSHA256").generateKey().getEncoded()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }
}
