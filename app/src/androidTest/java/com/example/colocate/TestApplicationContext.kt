package com.example.colocate

import androidx.core.os.bundleOf
import androidx.test.rule.ActivityTestRule
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.StatusModule
import com.example.colocate.persistence.ID_NOT_REGISTERED
import com.example.colocate.registration.TokenRetriever
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNotNull
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule
import java.nio.charset.Charset
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class TestApplicationContext(rule: ActivityTestRule<FlowTestStartActivity>) {

    private val testActivity = rule.activity
    private val app = rule.activity.application as ColocateApplication

    private val notificationService = NotificationService()
    private val testDispatcher = TestCoLocateServiceDispatcher()
    private val testRxBleClient = TestRxBleClient(app)
    private val startTimestampProvider = { Date.from(Instant.parse("2020-04-01T14:33:13Z")) }
    private val endTimestampProvider = { Date.from(Instant.parse("2020-04-01T14:43:13Z")) }

    private val testModule = TestModule(app, testRxBleClient, startTimestampProvider, endTimestampProvider)
    private val mockServer = MockWebServer()

    init {
        mockServer.apply {
            dispatcher = testDispatcher
            start()
        }

        val mockServerUrl = mockServer.url("").toString().removeSuffix("/")

        app.appComponent =
            DaggerTestAppComponent.builder()
                .appModule(AppModule(app))
                .encryptionKeyStorageModule(EncryptionKeyStorageModule(app))
                .statusModule(StatusModule(app))
                .networkModule(NetworkModule(mockServerUrl))
                .testModule(testModule)
                .build()

        app.appComponent.inject(notificationService)
    }

    fun shutdownMockServer() {
        mockServer.shutdown()
    }

    fun simulateActivationCodeReceived() {
        val msg = RemoteMessage(bundleOf("activationCode" to "test activation code #001"))
        notificationService.onMessageReceived(msg)
    }

    fun verifyReceivedRegistrationRequest() {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices/registrations")
        assertThat(lastRequest?.body?.readUtf8()).isEqualTo("""{"pushToken":"test firebase token #010"}""")
    }

    fun verifyReceivedActivationRequest() {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices")
        assertThat(lastRequest?.body?.readUtf8()).isEqualTo("""{"activationCode":"test activation code #001","pushToken":"test firebase token #010"}""")
    }

    fun verifyResidentIdAndSecretKey() {
        val idProvider = testActivity.residentIdProvider
        val keyStorage = testActivity.encryptionKeyStorage

        await until {
            idProvider.getResidentId() != ID_NOT_REGISTERED
        }
        assertThat(idProvider.getResidentId()).isEqualTo(TestCoLocateServiceDispatcher.RESIDENT_ID)

        await untilNotNull {
            keyStorage.provideKey()
        }
        val decodedKey = keyStorage.provideKey()?.toString(Charset.defaultCharset())
        assertThat(decodedKey).isEqualTo(TestCoLocateServiceDispatcher.SECRET_KEY)
    }

    fun simulateDeviceInProximity() {
        testRxBleClient.emitScanResults(
            ScanResultArgs(
                UUID.fromString("04330a56-ad45-4b0f-81ee-dd414910e1f5"),
                "06-00-00-00-00-00",
                listOf(10, 20, 15)
            ),
            ScanResultArgs(UUID.fromString("984c61e2-0d66-44eb-beea-fbd8f2991de3"), "07-00-00-00-00-00", listOf(10))
        )

        val dao = testActivity.appDatabase.contactEventV2Dao()
        await until {
            runBlocking { dao.getAll().size } == 2
        }
    }

    fun verifyReceivedProximityRequest() {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("PATCH")
        assertThat(lastRequest?.path).isEqualTo("/api/residents/${TestCoLocateServiceDispatcher.RESIDENT_ID}")

        val body = lastRequest?.body?.readUtf8() ?: ""
        assertThat(body).startsWith("""{"contactEvents":[""")
        assertThat(body).contains(
            JSONObject(
                mapOf(
                    "sonarId" to "04330a56-ad45-4b0f-81ee-dd414910e1f5",
                    "rssiValues" to listOf(10, 20, 15),
                    "timestamp" to "2020-04-01T14:33:13Z",
                    "duration" to 600
                )
            ).toString()
        )
        assertThat(body).contains(
            JSONObject(
                mapOf(
                    "sonarId" to "984c61e2-0d66-44eb-beea-fbd8f2991de3",
                    "rssiValues" to listOf(10),
                    "timestamp" to "2020-04-01T14:33:13Z",
                    "duration" to 600
                )
            ).toString()
        )
        assertThat(body.countOccurrences("""{"sonarId":""")).isEqualTo(2)
    }
}

class TestTokenRetriever : TokenRetriever {
    override suspend fun retrieveToken() =
        TokenRetriever.Result.Success("test firebase token #010")
}

private fun String.countOccurrences(substring: String): Int =
    if (!contains(substring)) {
        0
    } else {
        1 + replaceFirst(substring, "").countOccurrences(substring)
    }
