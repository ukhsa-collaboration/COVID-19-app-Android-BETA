package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.content.ContextWrapper
import androidx.core.os.bundleOf
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNotNull
import org.joda.time.DateTime
import org.joda.time.Seconds
import uk.nhs.nhsx.sonar.android.app.ColocateApplication
import uk.nhs.nhsx.sonar.android.app.FlowTestStartActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationService
import uk.nhs.nhsx.sonar.android.app.registration.ID_NOT_REGISTERED
import uk.nhs.nhsx.sonar.android.app.registration.TokenRetriever
import uk.nhs.nhsx.sonar.android.client.http.jsonOf
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.TimeUnit

class TestApplicationContext(rule: ActivityTestRule<FlowTestStartActivity>) {

    private val testActivity = rule.activity
    private val app = rule.activity.application as ColocateApplication
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val notificationService = NotificationService()
    private val testDispatcher = TestCoLocateServiceDispatcher()
    private val testRxBleClient = TestRxBleClient(app)
    private val startTimestampProvider = { DateTime.parse("2020-04-01T14:33:13Z") }
    private val endTimestampProvider = { DateTime.parse("2020-04-01T14:43:13Z") }

    private val testBluetoothModule = TestBluetoothModule(
        app,
        testRxBleClient,
        startTimestampProvider,
        endTimestampProvider
    )
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
                .persistenceModule(PersistenceModule(app))
                .bluetoothModule(testBluetoothModule)
                .cryptoModule(CryptoModule())
                .networkModule(NetworkModule(mockServerUrl))
                .testModule(TestModule())
                .build()

        notificationService.let {
            val contextField = ContextWrapper::class.java.getDeclaredField("mBase")
            contextField.isAccessible = true
            contextField.set(it, app)

            app.appComponent.inject(it)
        }
    }

    fun shutdownMockServer() {
        mockServer.shutdown()
    }

    fun simulateActivationCodeReceived() {
        val msg = RemoteMessage(bundleOf("activationCode" to "test activation code #001"))
        notificationService.onMessageReceived(msg)
    }

    fun simulateStatusUpdateReceived() {
        val msg = RemoteMessage(bundleOf("status" to "POTENTIAL"))
        notificationService.onMessageReceived(msg)
    }

    fun clickOnStatusNotification() {
        val notificationText = testActivity.getString(R.string.notification_text)
        val notificationTitle = testActivity.getString(R.string.notification_title)

        device.openNotification()

        device.wait(Until.hasObject(By.text(notificationTitle)), 500)

        // Only title is shown, click on it to toggle notification,
        // on some devices/android version it might trigger the notification action instead
        if (!device.hasObject(By.text(notificationText))) {
            device.findObject(By.text(notificationTitle)).click()
        }

        // If notification text is visible, click it.
        // It might have shown up because we toggled by clicking on the title
        // It might have always been visible if there was enough room on the screen
        if (device.hasObject(By.text(notificationText))) {
            device.findObject(By.text(notificationText)).click()
        }

        // Ensure notifications are hidden before moving on.
        device.wait(Until.gone(By.text(notificationText)), 500)
        device.wait(Until.gone(By.text(notificationTitle)), 500)
    }

    fun simulateBackendResponse(error: Boolean) {
        testDispatcher.simulateResponse(error)
    }

    fun verifyReceivedRegistrationRequest() {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices/registrations")
        assertThat(lastRequest?.body?.readUtf8()).isEqualTo("""{"pushToken":"test firebase token #010"}""")
    }

    fun verifyRegistrationFlow() {
        verifyReceivedRegistrationRequest()
        verifyReceivedActivationRequest()
        verifySonarIdAndSecretKey()
    }

    fun verifyReceivedActivationRequest() {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices")
        assertThat(lastRequest?.body?.readUtf8())
            .contains("""{"activationCode":"test activation code #001","pushToken":"test firebase token #010",""")
    }

    fun verifySonarIdAndSecretKey() {
        val idProvider = testActivity.sonarIdProvider
        val keyStorage = testActivity.encryptionKeyStorage

        await until {
            idProvider.getSonarId() != ID_NOT_REGISTERED
        }
        assertThat(idProvider.getSonarId()).isEqualTo(TestCoLocateServiceDispatcher.RESIDENT_ID)

        await untilNotNull {
            keyStorage.provideKey()
        }
        val decodedKey = keyStorage.provideKey()?.toString(Charset.defaultCharset())
        assertThat(decodedKey).isEqualTo(TestCoLocateServiceDispatcher.SECRET_KEY)
    }

    fun simulateDeviceInProximity() {
        val timestamp = DateTime.parse("2020-04-01T14:33:13Z")
        testRxBleClient.emitScanResults(
            ScanResultArgs(
                UUID.fromString("04330a56-ad45-4b0f-81ee-dd414910e1f5"),
                "06-00-00-00-00-00",
                listOf(10),
                timestamp
            ),
            ScanResultArgs(
                UUID.fromString("04330a56-ad45-4b0f-81ee-dd414910e1f5"),
                "06-00-00-00-00-00",
                listOf(20),
                timestamp.plus(Seconds.seconds(10))
            ),
            ScanResultArgs(
                UUID.fromString("04330a56-ad45-4b0f-81ee-dd414910e1f5"),
                "06-00-00-00-00-00",
                listOf(15),
                timestamp.plus(Seconds.seconds(100))

            ),
            ScanResultArgs(
                UUID.fromString("984c61e2-0d66-44eb-beea-fbd8f2991de3"),
                "07-00-00-00-00-00",
                listOf(10),
                timestamp
            )
        )

        val dao = testActivity.appDatabase.contactEventV2Dao()
        await until {
            runBlocking { dao.getAll().size } == 3
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
            jsonOf(
                "sonarId" to "04330a56-ad45-4b0f-81ee-dd414910e1f5",
                "rssiValues" to listOf(10, 20),
                "timestamp" to "2020-04-01T14:33:13Z",
                "duration" to 10
            )
        )
        assertThat(body).contains(
            jsonOf(
                "sonarId" to "04330a56-ad45-4b0f-81ee-dd414910e1f5",
                "rssiValues" to listOf(15),
                "timestamp" to "2020-04-01T14:34:53Z",
                "duration" to 0
            )
        )
        assertThat(body).contains(
            jsonOf(
                "sonarId" to "984c61e2-0d66-44eb-beea-fbd8f2991de3",
                "rssiValues" to listOf(10),
                "timestamp" to "2020-04-01T14:33:13Z",
                "duration" to 0
            )
        )
        assertThat(body.countOccurrences("""{"sonarId":""")).isEqualTo(3)
    }

    fun simulateBackendDelay(delayInMillis: Long) {
        testDispatcher.simulateDelay(delayInMillis)
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
