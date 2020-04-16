package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.content.ContextWrapper
import android.content.Intent
import androidx.annotation.StringRes
import android.util.Base64
import androidx.core.os.bundleOf
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import net.danlew.android.joda.JodaTimeAndroid
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNotNull
import org.joda.time.DateTime
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ColocateApplication
import uk.nhs.nhsx.sonar.android.app.FlowTestStartActivity
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationService
import uk.nhs.nhsx.sonar.android.app.notifications.ReminderTimeProvider
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
    private var eventNumber = 0
    private val currentTimestampProvider = {
        eventNumber++
        Timber.d("Sending event nr $eventNumber")
        when (eventNumber) {
            1, 2 -> DateTime.parse("2020-04-01T14:33:13Z")
            3, 4 -> DateTime.parse("2020-04-01T14:34:43Z") // +90 seconds
            5, 6 -> DateTime.parse("2020-04-01T14:44:53Z") // +610 seconds
            else -> throw IllegalStateException()
        }
    }

    private val testBluetoothModule = TestBluetoothModule(
        app,
        testRxBleClient,
        startTimestampProvider,
        endTimestampProvider,
        currentTimestampProvider
    )
    private val mockServer = MockWebServer()

    init {
        mockServer.apply {
            dispatcher = testDispatcher
            start()
        }
        JodaTimeAndroid.init(app)

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

    fun clickOnNotification(
        @StringRes notificationTitleRes: Int, @StringRes notificationTextRes: Int,
        notificationDisplayTimeout: Long = 500
    ) {
        val notificationTitle = testActivity.getString(notificationTitleRes)
        val notificationText = testActivity.getString(notificationTextRes)

        device.openNotification()

        device.wait(Until.hasObject(By.text(notificationTitle)), notificationDisplayTimeout)

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
        verifySonarIdAndSecretKeyAndPublicKey()
    }

    fun verifyReceivedActivationRequest() {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices")
        assertThat(lastRequest?.body?.readUtf8())
            .contains("""{"activationCode":"test activation code #001","pushToken":"test firebase token #010",""")
    }

    fun verifySonarIdAndSecretKeyAndPublicKey() {
        val idProvider = testActivity.sonarIdProvider
        val keyStorage = testActivity.keyStorage

        await until {
            idProvider.getSonarId() != ID_NOT_REGISTERED
        }
        assertThat(idProvider.getSonarId()).isEqualTo(TestCoLocateServiceDispatcher.RESIDENT_ID)

        await untilNotNull {
            keyStorage.provideSecretKey()
        }
        val decodedKey = keyStorage.provideSecretKey()?.toString(Charset.defaultCharset())
        assertThat(decodedKey).isEqualTo(TestCoLocateServiceDispatcher.SECRET_KEY)

        await untilNotNull {
            keyStorage.providePublicKey()
        }
        val publicKey = keyStorage.providePublicKey()?.encoded
        assertThat(publicKey).isEqualTo(
            Base64.decode(
                TestCoLocateServiceDispatcher.PUBLIC_KEY,
                Base64.DEFAULT
            )
        )
    }

    fun simulateDeviceInProximity() {
        testRxBleClient.emitScanResults(
            ScanResultArgs(
                sonarId = UUID.fromString("04330a56-ad45-4b0f-81ee-dd414910e1f5"),
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(10)
            ),
            ScanResultArgs(
                sonarId = UUID.fromString("984c61e2-0d66-44eb-beea-fbd8f2991de3"),
                macAddress = "07-00-00-00-00-00",
                rssiList = listOf(40)
            )
        )

        val dao = testActivity.appDatabase.contactEventDao()
        await until {
            runBlocking { dao.getAll().size } == 2
        }

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                sonarId = UUID.fromString("04330a56-ad45-4b0f-81ee-dd414910e1f5"),
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(20)
            ),
            ScanResultArgs(
                sonarId = UUID.fromString("98155054-72cc-4437-b8fc-82ea33ef683c"),
                macAddress = "09-00-00-00-00-00",
                rssiList = listOf(80)
            )
        )
        await until {
            runBlocking { dao.getAll().size } == 3
        }

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                sonarId = UUID.fromString("04330a56-ad45-4b0f-81ee-dd414910e1f5"),
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(15)
            )
        )
        await until {
            runBlocking { dao.getAll().size } > 3
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
                "duration" to 90
            )
        )
        assertThat(body).contains(
            jsonOf(
                "sonarId" to "04330a56-ad45-4b0f-81ee-dd414910e1f5",
                "rssiValues" to listOf(15),
                "timestamp" to "2020-04-01T14:44:53Z",
                "duration" to 60
            )
        )
        assertThat(body).contains(
            jsonOf(
                "sonarId" to "98155054-72cc-4437-b8fc-82ea33ef683c",
                "rssiValues" to listOf(80),
                "timestamp" to "2020-04-01T14:34:43Z",
                "duration" to 60
            )
        )
        assertThat(body.countOccurrences("""{"sonarId":""")).isEqualTo(4)
    }

    fun simulateBackendDelay(delayInMillis: Long) {
        testDispatcher.simulateDelay(delayInMillis)
    }

    fun closeNotificationPanel() {
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        app.baseContext.sendBroadcast(it)
    }
}

class TestTokenRetriever : TokenRetriever {
    override suspend fun retrieveToken() =
        TokenRetriever.Result.Success("test firebase token #010")
}

class TestReminderTimeProvider : ReminderTimeProvider {
    override fun provideTime(): Long {
        return System.currentTimeMillis() + 50
    }
}

private fun String.countOccurrences(substring: String): Int =
    if (!contains(substring)) {
        0
    } else {
        1 + replaceFirst(substring, "").countOccurrences(substring)
    }
