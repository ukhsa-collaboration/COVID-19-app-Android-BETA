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
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import java.security.PublicKey
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.crypto.KeyGenerator

@RunWith(AndroidJUnit4::class)
class CoLocationApiInstrumentationTest {
    lateinit var server: MockWebServer
    lateinit var keyStorage: KeyStorage

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start(8089)

        keyStorage = object : KeyStorage {
            override fun provideSecretKey() = generateKey()
            override fun storeSecretKey(encodedKey: String) = Unit
            override fun providePublicKey(): PublicKey? = null
            override fun storeServerPublicKey(encodedKey: String) = Unit
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testSave() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val httpClient = HttpClient(ctx)
        val coLocationApi = CoLocationApi("http://localhost:8089", keyStorage, httpClient)

        server.enqueue(MockResponse().setResponseCode(200))

        val promise = coLocationApi.save(
            CoLocationData(
                sonarId = "::sonar-id::",
                symptomsTimestamp = "::timestamp::",
                contactEvents = emptyList()
            )
        )

        val request = server.takeRequest(300, MILLISECONDS)
        assertEquals("/api/residents/::sonar-id::", request?.path)

        await until { promise.isSuccess }
    }

    private fun generateKey(): ByteArray =
        KeyGenerator.getInstance("HMACSHA256").generateKey().encoded
}
