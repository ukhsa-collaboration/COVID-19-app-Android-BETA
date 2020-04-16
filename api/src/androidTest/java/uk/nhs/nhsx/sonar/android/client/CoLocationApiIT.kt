/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
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

@RunWith(AndroidJUnit4::class)
class CoLocationApiIT {
    lateinit var server: MockWebServer
    lateinit var encryptionKeyStorage: EncryptionKeyStorage

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start(8089)

        encryptionKeyStorage = object : EncryptionKeyStorage {
            override fun provideKey() = generateKey()
            override fun putBase64Key(encodedKey: String) = Unit
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun shouldSendCoLocationData() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val httpClient = VolleyHttpClient(ctx)
        val coLocationApi = CoLocationApi("http://localhost:8089", encryptionKeyStorage, httpClient)

        server.enqueue(MockResponse().setResponseCode(200))

        val promise = coLocationApi.save(CoLocationData("::sonar-id::", emptyList()))

        val request = server.takeRequest(300, MILLISECONDS)
        assertEquals("/api/residents/::sonar-id::", request?.path)

        await until { promise.isSuccess }
    }

    private fun generateKey(): ByteArray =
        KeyGenerator.getInstance("HMACSHA256").generateKey().encoded
}
