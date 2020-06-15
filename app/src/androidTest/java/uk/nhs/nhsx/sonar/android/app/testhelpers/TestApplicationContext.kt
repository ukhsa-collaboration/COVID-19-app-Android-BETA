/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.util.Base64
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.work.WorkManager
import net.danlew.android.joda.JodaTimeAndroid
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNotNull
import uk.nhs.nhsx.sonar.android.app.SonarApplication
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeWaitTime
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.PUBLIC_KEY
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.RESIDENT_ID
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.SECRET_KEY
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.encodedSecretKey
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper
import uk.nhs.nhsx.sonar.android.app.util.TestNotificationManagerHelper
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Mac

class TestApplicationContext {

    val app: SonarApplication = ApplicationProvider.getApplicationContext()

    val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val testLocationHelper = TestLocationHelper(AndroidLocationHelper(app))

    private val testNotificationManagerHelper = TestNotificationManagerHelper(true)

    private val proximityEvents = TestProximityEvents(app)

    private val notificationEmulator = TestNotificationEmulator(app)

    private val server = TestMockServer()

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        .apply { load(null) }
        .apply { aliases().asSequence().forEach { deleteEntry(it) } }

    private val component: TestAppComponent = DaggerTestAppComponent.builder()
        .appModule(
            AppModule(
                app,
                testLocationHelper,
                testNotificationManagerHelper,
                ActivationCodeWaitTime(10, TimeUnit.SECONDS)
            )
        )
        .persistenceModule(PersistenceModule(app))
        .bluetoothModule(proximityEvents.testBluetoothModule)
        .cryptoModule(CryptoModule(app, keyStore))
        .networkModule(NetworkModule(server.url(), "someValue", "buildInfo"))
        .testNotificationsModule(TestNotificationsModule())
        .build()

    init {
        JodaTimeAndroid.init(app)

        app.appComponent = component
        app.appComponent.inject(notificationEmulator.notificationService)

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
        server.verifyReceivedProximityRequest(proximityEvents.testProximityEvent)
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
        notificationEmulator.simulateExposureNotificationReceived()
    }

    fun simulateTestResultNotificationReceived(testInfo: TestInfo) {
        notificationEmulator.simulateTestResultNotificationReceived(testInfo)
    }

    private fun simulateActivationCodeReceived() {
        notificationEmulator.simulateActivationCodeReceived()
    }

    fun simulateBackendResponse(error: Boolean) {
        server.simulateBackendResponse(error)
    }

    fun simulateUnsupportedDevice() {
        proximityEvents.simulateUnsupportedDevice()
    }

    fun simulateTablet() {
        proximityEvents.simulateTablet()
    }

    fun simulateDeviceInProximity() {
        proximityEvents.simulateDeviceInProximity()
    }

    fun simulateBackendDelay(delayInMillis: Long) {
        server.simulateBackendDelay(delayInMillis)
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

        proximityEvents.testBluetoothModule.reset()
        testLocationHelper.reset()

        WorkManager.getInstance(app).cancelAllWork()

        closeNotificationPanel()
        ensureBluetoothEnabled()
    }

    fun userState() = component.getUserStateStorage().get()
}

fun stringFromResId(@StringRes stringRes: Int): String {
    val resources = ApplicationProvider.getApplicationContext<SonarApplication>().resources
    return resources.getString(stringRes)
}
