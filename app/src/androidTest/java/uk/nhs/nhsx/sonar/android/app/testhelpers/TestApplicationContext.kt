/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.content.ContextWrapper
import android.content.Intent
import android.util.Base64
import androidx.annotation.StringRes
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
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.http.jsonOf
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationService
import uk.nhs.nhsx.sonar.android.app.notifications.ReminderTimeProvider
import uk.nhs.nhsx.sonar.android.app.registration.TokenRetriever
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TestApplicationContext(rule: ActivityTestRule<FlowTestStartActivity>) {

    private val testActivity = rule.activity
    val app = rule.activity.application as ColocateApplication
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val notificationService = NotificationService()
    private val testRxBleClient = TestRxBleClient(app)
    private var eventNumber = 0
    private val currentTimestampProvider = {
        eventNumber++
        Timber.d("Sending event nr $eventNumber")
        when (eventNumber) {
            1 -> DateTime.parse("2020-04-01T14:33:13Z")
            2, 3 -> DateTime.parse("2020-04-01T14:34:43Z") // +90 seconds
            4 -> DateTime.parse("2020-04-01T14:44:53Z") // +610 seconds
            else -> throw IllegalStateException()
        }
    }

    private val testBluetoothModule = TestBluetoothModule(
        app,
        testRxBleClient,
        currentTimestampProvider,
        scanIntervalLength = 2
    )

    private var testDispatcher = TestCoLocateServiceDispatcher()
    private var mockServer = MockWebServer()

    val component: TestAppComponent

    init {
        JodaTimeAndroid.init(app)

        resetTestMockServer()
        val mockServerUrl = mockServer.url("").toString().removeSuffix("/")

        component = DaggerTestAppComponent.builder()
            .appModule(AppModule(app))
            .persistenceModule(PersistenceModule(app))
            .bluetoothModule(testBluetoothModule)
            .cryptoModule(CryptoModule())
            .networkModule(NetworkModule(mockServerUrl, "someValue"))
            .testNotificationsModule(TestNotificationsModule())
            .build()

        app.appComponent = component

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

    private fun simulateActivationCodeReceived() {
        val msg = RemoteMessage(bundleOf("activationCode" to "test activation code #001"))
        notificationService.onMessageReceived(msg)
    }

    fun simulateStatusUpdateReceived() {
        val msg = RemoteMessage(bundleOf("status" to "POTENTIAL"))
        notificationService.onMessageReceived(msg)
    }

    fun clickOnNotification(
        @StringRes notificationTitleRes: Int,
        @StringRes notificationTextRes: Int,
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

    fun verifyRegistrationFlow() {
        verifyReceivedRegistrationRequest()
        simulateActivationCodeReceived()
        verifyReceivedActivationRequest()
        verifySonarIdAndSecretKeyAndPublicKey()
    }

    fun verifyRegistrationRetry() {
        verifyReceivedRegistrationRequest()
        simulateActivationCodeReceived()
    }

    private fun verifyReceivedRegistrationRequest() {
        // WorkManager is responsible for starting registration process and unfortunately it is not exact
        // Have to wait for longer time (usually less than 10 seconds). Putting 20 secs just to be sure
        var lastRequest = mockServer.takeRequest(20_000, TimeUnit.MILLISECONDS)

        if (lastRequest?.path?.contains("linking-id") == true) {
            lastRequest = mockServer.takeRequest()
        }

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices/registrations")
        assertThat(lastRequest?.body?.readUtf8()).isEqualTo("""{"pushToken":"test firebase token #010"}""")
    }

    private fun verifyReceivedActivationRequest() {
        // WorkManager is responsible for starting registration process and unfortunately it is not exact
        // Have to wait for longer time (usually less than 10 seconds). Putting 20 secs just to be sure
        val lastRequest = mockServer.takeRequest(20_000, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices")
        assertThat(lastRequest?.body?.readUtf8())
            .contains("""{"activationCode":"test activation code #001","pushToken":"test firebase token #010",""")
    }

    private fun verifySonarIdAndSecretKeyAndPublicKey() {
        val idProvider = component.getSonarIdProvider()
        val keyStorage = component.getKeyStorage()

        await until {
            idProvider.getSonarId().isNotEmpty()
        }
        assertThat(idProvider.getSonarId()).isEqualTo(TestCoLocateServiceDispatcher.RESIDENT_ID)

        await untilNotNull {
            keyStorage.provideSecretKey()
        }
        assertThat(keyStorage.provideSecretKey()).isEqualTo(TestCoLocateServiceDispatcher.SECRET_KEY)

        await untilNotNull {
            keyStorage.providePublicKey()
        }
        val publicKey = keyStorage.providePublicKey()?.encoded
        val decodedPublicKey =
            Base64.decode(TestCoLocateServiceDispatcher.PUBLIC_KEY, Base64.DEFAULT)
        assertThat(publicKey).isEqualTo(decodedPublicKey)
    }

    fun simulateDeviceInProximity() {
        val firstDeviceId = ByteArray(BluetoothIdentifier.SIZE) { 1 }
        val secondDeviceId = ByteArray(BluetoothIdentifier.SIZE) { 2 }
        val dao = component.getAppDatabase().contactEventDao()

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                encryptedId = firstDeviceId,
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(10)
            ),
            ScanResultArgs(
                encryptedId = secondDeviceId,
                macAddress = "07-00-00-00-00-00",
                rssiList = listOf(40)
            )
        )

        await until {
            runBlocking { dao.getAll().size } == 2
        }

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                encryptedId = firstDeviceId,
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(20)
            )
        )

        await until {
            runBlocking { dao.get(firstDeviceId)!!.rssiValues.size } == 2
        }

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                encryptedId = firstDeviceId,
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(15)
            )
        )

        await until {
            runBlocking { dao.get(firstDeviceId)!!.rssiValues.size } == 3
        }
    }

    fun verifyReceivedProximityRequest() {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.path).isEqualTo("/api/residents/${TestCoLocateServiceDispatcher.RESIDENT_ID}")
        assertThat(lastRequest?.method).isEqualTo("PATCH")

        val body = lastRequest?.body?.readUtf8() ?: ""
        assertThat(body).contains(""""symptomsTimestamp":""")
        assertThat(body).contains(""""contactEvents":[""")
        assertThat(body).contains(
            jsonOf(
                "encryptedRemoteContactId" to "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\nAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQ==\n",
                "rssiValues" to listOf(10, 20, 15),
                "rssiOffsets" to listOf(0, 90, 610),
                "timestamp" to "2020-04-01T14:33:13Z",
                "duration" to 700,
                "txPower" to 1
            )
        )
        assertThat(body).contains(
            jsonOf(
                "encryptedRemoteContactId" to "AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC\nAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAg==\n",
                "rssiValues" to listOf(40),
                "rssiOffsets" to listOf(0),
                "timestamp" to "2020-04-01T14:34:43Z",
                "duration" to 60,
                "txPower" to 2
            )
        )
        assertThat(body.countOccurrences("""{"encryptedRemoteContactId":""")).isEqualTo(2)
    }

    fun simulateBackendDelay(delayInMillis: Long) {
        testDispatcher.simulateDelay(delayInMillis)
    }

    fun closeNotificationPanel() {
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        app.baseContext.sendBroadcast(it)
    }

    fun simulateUnsupportedDevice() {
        testBluetoothModule.simulateUnsupportedDevice = true
    }

    fun resetTestMockServer() {
        testDispatcher = TestCoLocateServiceDispatcher()
        mockServer.shutdown()
        mockServer = MockWebServer()
        mockServer.dispatcher = testDispatcher
        mockServer.start(43239)
    }

    fun reset() {
        component.apply {
            getAppDatabase().clearAllTables()
            getOnboardingStatusProvider().setOnboardingFinished(false)
            getStateStorage().clear()
            getSonarIdProvider().clear()
            getActivationCodeProvider().clear()
        }
        testBluetoothModule.simulateUnsupportedDevice = false
        resetTestMockServer()
    }
}

class TestTokenRetriever : TokenRetriever {
    override suspend fun retrieveToken() = "test firebase token #010"
}

class TestReminderTimeProvider : ReminderTimeProvider {
    override fun provideNextReminderTime(): Long {
        return System.currentTimeMillis() + 50
    }

    override fun setLastReminderNotificationTime(time: Calendar) {
    }
}

private fun String.countOccurrences(substring: String): Int =
    if (!contains(substring)) {
        0
    } else {
        1 + replaceFirst(substring, "").countOccurrences(substring)
    }
