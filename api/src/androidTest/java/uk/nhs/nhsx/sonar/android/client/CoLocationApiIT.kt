/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.awaitility.kotlin.await
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationData
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.crypto.KeyGenerator
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class CoLocationApiIT {
    lateinit var server: MockWebServer
    lateinit var encryptionKeyStorage: EncryptionKeyStorage

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start(8089)
        encryptionKeyStorage = mock()
        whenever(encryptionKeyStorage.provideKey()).thenReturn(generateKey())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun shouldSendCoLocationData() {
        val successResponse = MockResponse().setResponseCode(200)

        server.enqueue(successResponse)

        var isSuccess = false
        var isError = false

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val httpClient = VolleyHttpClient("http://localhost:8089", context)
        val coLocationApi = CoLocationApi(encryptionKeyStorage, httpClient)

        coLocationApi.save(
            CoLocationData("residentId", JSONArray()),
            { isSuccess = true },
            { isError = true }
        )

        val request = server.takeRequest(300, MILLISECONDS)
        assertEquals("/api/residents/residentId", request?.path)

        await.until { isSuccess }
        assertFalse(isError)
    }

    private fun generateKey(): ByteArray =
        KeyGenerator.getInstance("HMACSHA256").generateKey().encoded
}
