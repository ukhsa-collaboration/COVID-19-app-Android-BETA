/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContextWrapper
import android.content.Intent
import android.util.Base64
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.work.WorkManager
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import net.danlew.android.joda.JodaTimeAndroid
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNotNull
import org.joda.time.DateTime
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.SonarApplication
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import uk.nhs.nhsx.sonar.android.app.crypto.encodeAsSecondsSinceEpoch
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationService
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeWaitTime
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.PUBLIC_KEY
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.RESIDENT_ID
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.SECRET_KEY
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.encodedSecretKey
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper
import uk.nhs.nhsx.sonar.android.app.util.TestNotificationManagerHelper
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import kotlin.random.Random

class TestApplicationContext {

    val app: SonarApplication = ApplicationProvider.getApplicationContext()
    val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val notificationService = NotificationService()
    private val testRxBleClient = TestRxBleClient(app)
    private var eventNumber = 0
    private val startTime = DateTime.parse("2020-04-01T14:33:13Z")
    private val currentTimestampProvider = {
        eventNumber++
        Timber.d("Sending event nr $eventNumber")
        when (eventNumber) {
            1 -> {
                startTime
            }
            2, 3 -> DateTime.parse("2020-04-01T14:34:43Z") // +90 seconds
            4 -> DateTime.parse("2020-04-01T14:44:53Z") // +610 seconds
            else -> throw IllegalStateException()
        }
    }
    private val countryCode = "GB".toByteArray()
    private val transmissionTime = ByteBuffer.wrap(startTime.encodeAsSecondsSinceEpoch()).int
    private val firstDeviceSignature = Random.nextBytes(16)
    private val secondDeviceSignature = Random.nextBytes(16)
    private val firstDeviceId = BluetoothIdentifier(
        countryCode,
        Cryptogram.fromBytes(Random.nextBytes(Cryptogram.SIZE)),
        -6,
        transmissionTime,
        firstDeviceSignature
    )
    private val secondDeviceId = BluetoothIdentifier(
        countryCode,
        Cryptogram.fromBytes(Random.nextBytes(Cryptogram.SIZE)),
        -8,
        transmissionTime + 90,
        secondDeviceSignature
    )

    private val testBluetoothModule = TestBluetoothModule(
        app,
        testRxBleClient,
        currentTimestampProvider,
        scanIntervalLength = 2
    )

    private val testLocationHelper = TestLocationHelper(AndroidLocationHelper(app))
    private val testNotificationManagerHelper = TestNotificationManagerHelper(true)

    private val server = TestMockServer()

    val component: TestAppComponent

    init {
        JodaTimeAndroid.init(app)

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.aliases().asSequence().forEach { keyStore.deleteEntry(it) }

        server.start()

        component = DaggerTestAppComponent.builder()
            .appModule(
                AppModule(
                    app,
                    testLocationHelper,
                    testNotificationManagerHelper,
                    ActivationCodeWaitTime(10, TimeUnit.SECONDS)
                )
            )
            .persistenceModule(PersistenceModule(app))
            .bluetoothModule(testBluetoothModule)
            .cryptoModule(CryptoModule(app, keyStore))
            .networkModule(NetworkModule(server.url(), "someValue", "buildInfo"))
            .testNotificationsModule(TestNotificationsModule())
            .build()

        app.appComponent = component

        notificationService.let {
            val contextField = ContextWrapper::class.java.getDeclaredField("mBase")
            contextField.isAccessible = true
            contextField.set(it, app)

            app.appComponent.inject(it)
        }

        reset()
    }

    fun teardown() {
        server.shutdown()
    }

    fun setFullValidUser(state: UserState = DefaultState) {
        component.getUserStateStorage().set(state)
        component.getSonarIdProvider().set(RESIDENT_ID)
        component.getKeyStorage().storeSecretKey(encodedSecretKey)
        component.getKeyStorage().storeServerPublicKey(PUBLIC_KEY)
    }

    fun addRecoveryMessage() {
        component.getUserInbox().addRecovery()
    }

    fun addTestInfo(info: TestInfo) {
        component.getUserInbox().addTestInfo(info)
    }

    fun setFinishedOnboarding() {
        val storage = component.getOnboardingStatusProvider()
        storage.set(true)
    }

    fun setValidPostcode() {
        val storage = component.getPostCodeProvider()
        storage.set("E1")
    }

    private fun bluetoothAdapter(): BluetoothAdapter {
        val context = app.applicationContext
        val manager = context.getSystemService(BluetoothManager::class.java) as BluetoothManager
        return manager.adapter
    }

    fun ensureBluetoothDisabled() {
        bluetoothAdapter().let {
            it.disable()
            await until { !it.isEnabled }
        }
    }

    fun isNotificationDisplayed(
        @StringRes notificationTitleRes: Int,
        isDisplayed: Boolean
    ) {
        val notificationTitle = app.getString(notificationTitleRes)

        device.openNotification()

        assertThat(device.hasObject(By.text(notificationTitle))).isEqualTo(isDisplayed)

        device.pressBack()
    }

    fun clickOnNotification(
        @StringRes notificationTitleRes: Int,
        @StringRes notificationTextRes: Int,
        notificationDisplayTimeout: Long = 500
    ) {
        val notificationTitle = app.getString(notificationTitleRes)
        val notificationText = app.getString(notificationTextRes)

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

    fun clickOnNotificationAction(
        @StringRes notificationTitleRes: Int,
        @StringRes notificationTextRes: Int,
        @StringRes notificationActionRes: Int,
        notificationDisplayTimeout: Long = 500
    ) {
        val notificationTitle = app.getString(notificationTitleRes)
        val notificationText = app.getString(notificationTextRes)
        val notificationAction = app.getString(notificationActionRes)

        device.openNotification()

        device.wait(Until.hasObject(By.text(notificationTitle)), notificationDisplayTimeout)

        // Only title is shown, click on it to toggle notification,
        // on some devices/android version it might trigger the notification action instead
        if (!device.hasObject(By.text(notificationAction)) &&
            !device.hasObject(By.text(notificationAction.toUpperCase()))
        ) {
            device.findObject(By.text(notificationTitle)).swipe(Direction.DOWN, 1F)
        }

        assertThat(device.hasObject(By.text(notificationText))).isTrue()

        val action = device.findObject(By.text(notificationAction.toUpperCase()))
            ?: device.findObject(By.text(notificationAction))

        action.click()
        device.pressBack()
    }

    fun verifyBluetoothIsEnabled() {
        bluetoothAdapter().let {
            await until { it.isEnabled }
        }
    }

    fun verifyRegistrationFlow() {
        server.verifyReceivedRegistrationRequest()
        simulateActivationCodeReceived()
        server.verifyReceivedActivationRequest()
        verifySonarIdAndSecretKeyAndPublicKey()
    }

    fun verifyRegistrationRetry() {
        server.verifyReceivedRegistrationRequest()
        simulateActivationCodeReceived()
    }

    fun verifyReceivedProximityRequest() {
        server.verifyReceivedProximityRequest(
            firstDeviceId = firstDeviceId,
            firstDeviceSignature = firstDeviceSignature,
            secondDeviceId = secondDeviceId,
            secondDeviceSignature = secondDeviceSignature,
            transmissionTime = transmissionTime,
            countryCode = countryCode
        )
    }

    private fun verifySonarIdAndSecretKeyAndPublicKey() {
        val idProvider = component.getSonarIdProvider()
        val keyStorage = component.getKeyStorage()

        await until {
            idProvider.get().isNotEmpty()
        }
        assertThat(idProvider.get()).isEqualTo(RESIDENT_ID)

        await untilNotNull {
            keyStorage.provideSecretKey()
        }
        val messageToSign = "some message".toByteArray()
        val actualSignature = Mac.getInstance("HMACSHA256").apply {
            init(keyStorage.provideSecretKey())
        }.doFinal(messageToSign)
        val expectedSignature = Mac.getInstance("HMACSHA256").apply {
            init(SECRET_KEY)
        }.doFinal(messageToSign)

        assertThat(actualSignature).isEqualTo(expectedSignature)

        await untilNotNull {
            keyStorage.providePublicKey()
        }
        val publicKey = keyStorage.providePublicKey()?.encoded
        val decodedPublicKey = Base64.decode(PUBLIC_KEY, Base64.DEFAULT)
        assertThat(publicKey).isEqualTo(decodedPublicKey)
    }

    fun simulateExposureNotificationReceived() {
        val msg = RemoteMessage(
            bundleOf(
                "type" to "Status Update",
                "status" to "Potential"
            )
        )
        notificationService.onMessageReceived(msg)
    }

    fun simulateTestResultNotificationReceived(testInfo: TestInfo) {
        val msg = RemoteMessage(
            bundleOf(
                "type" to "Test Result",
                "result" to "${testInfo.result}",
                "testTimestamp" to testInfo.date.toUtcIsoFormat()
            )
        )
        notificationService.onMessageReceived(msg)
    }

    fun simulateBackendResponse(error: Boolean) {
        server.simulateBackendResponse(error)
    }

    fun simulateDeviceInProximity() {
        val dao = component.getAppDatabase().contactEventDao()

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                encryptedId = firstDeviceId.asBytes(),
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(10),
                txPower = -5
            ),
            ScanResultArgs(
                encryptedId = secondDeviceId.asBytes(),
                macAddress = "07-00-00-00-00-00",
                rssiList = listOf(40),
                txPower = -1
            )
        )

        await until {
            runBlocking { dao.getAll().size } == 2
        }

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                encryptedId = firstDeviceId.asBytes(),
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(20),
                txPower = -5
            )
        )

        await until {
            runBlocking { dao.get(firstDeviceId.cryptogram.asBytes())!!.rssiValues.size } == 2
        }

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                encryptedId = firstDeviceId.asBytes(),
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(15),
                txPower = -5
            )
        )

        await until {
            runBlocking { dao.get(firstDeviceId.cryptogram.asBytes())!!.rssiValues.size } == 3
        }
    }

    private fun simulateActivationCodeReceived() {
        val msg = RemoteMessage(bundleOf("activationCode" to "test activation code #001"))
        notificationService.onMessageReceived(msg)
    }

    fun simulateBackendDelay(delayInMillis: Long) {
        server.simulateBackendDelay(delayInMillis)
    }

    fun simulateUnsupportedDevice() {
        testBluetoothModule.simulateUnsupportedDevice = true
    }

    fun simulateTablet() {
        testBluetoothModule.simulateTablet = true
    }

    fun disableLocationAccess() {
        testLocationHelper.locationEnabled = false
        app.sendBroadcast(Intent(testLocationHelper.providerChangedIntentAction))
    }

    fun enableLocationAccess() {
        testLocationHelper.locationEnabled = true
        app.sendBroadcast(Intent(testLocationHelper.providerChangedIntentAction))
    }

    fun revokeLocationPermission() {
        testLocationHelper.locationPermissionsGranted = false
    }

    fun grantLocationPermission() {
        testLocationHelper.locationPermissionsGranted = true
    }

    fun revokeNotificationsPermission() {
        testNotificationManagerHelper.notificationEnabled = false
    }

    fun grantNotificationsPermission() {
        testNotificationManagerHelper.notificationEnabled = true
    }

    fun waitUntilCannotFindText(@StringRes stringId: Int, timeoutInMs: Long = 500) {
        device.wait(Until.gone(By.text(app.getString(stringId))), timeoutInMs)
    }

    private fun closeNotificationPanel() {
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        app.baseContext.sendBroadcast(it)
    }

    private fun ensureBluetoothEnabled() {
        bluetoothAdapter().let {
            it.enable()
            await until { it.isEnabled }
        }
    }

    private fun reset() {
        component.apply {
            getAppDatabase().clearAllTables()
            getOnboardingStatusProvider().set(false)
            getUserStateStorage().clear()
            getSonarIdProvider().clear()
            getActivationCodeProvider().clear()
        }

        testBluetoothModule.reset()
        testLocationHelper.reset()

        WorkManager.getInstance(app).cancelAllWork()

        closeNotificationPanel()
        ensureBluetoothEnabled()
    }
}

fun stringFromResId(@StringRes stringRes: Int): String {
    val resources = ApplicationProvider.getApplicationContext<SonarApplication>().resources
    return resources.getString(stringRes)
}
