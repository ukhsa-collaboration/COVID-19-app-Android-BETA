/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

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
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import uk.nhs.nhsx.sonar.android.client.resident.Registration
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi

@RunWith(AndroidJUnit4::class)
class ResidentApiIT {
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
    fun shouldRegisterResident() {
        val citizenID = """00000000-0000-0000-0000-000000000001"""
        val secretKey = """alsdkfj lk jasdf"""
        val responseJson = """
            {
                "id": "$citizenID",
                "secretKey": "$secretKey"                
            }
        """

        server.enqueue(
            MockResponse().addHeader("Content-Type", "application/json")
                .setBody(responseJson).setResponseCode(200)
        )

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        var reg: Registration? = null

        ResidentApi(
            VolleyHttpClient(
                "http://localhost:8089",
                appContext
            )
        )
            .register({ registration ->
                reg = registration
            })

        await.until { reg !== null }

        assertEquals(citizenID, reg?.id)
        assertEquals(secretKey, reg?.secretKey)

        val request1 = server.takeRequest()
        assertEquals("/api/residents", request1.path)
    }
}
