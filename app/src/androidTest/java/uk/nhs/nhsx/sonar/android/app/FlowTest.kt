/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.hamcrest.CoreMatchers.not
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCode
import uk.nhs.nhsx.sonar.android.app.robots.onAtRiskScreen
import uk.nhs.nhsx.sonar.android.app.robots.onTestStartScreen
import uk.nhs.nhsx.sonar.android.app.robots.onDeviceUnsupportedScreen
import uk.nhs.nhsx.sonar.android.app.robots.onIsolateScreen
import uk.nhs.nhsx.sonar.android.app.robots.onMainScreen
import uk.nhs.nhsx.sonar.android.app.robots.onOkScreen
import uk.nhs.nhsx.sonar.android.app.robots.onPermissionScreen
import uk.nhs.nhsx.sonar.android.app.robots.onPostCodeScreen
import uk.nhs.nhsx.sonar.android.app.robots.onRegistrationPanel
import uk.nhs.nhsx.sonar.android.app.robots.onDiagnosisScreen
import uk.nhs.nhsx.sonar.android.app.robots.onEdgeCaseScreen
import uk.nhs.nhsx.sonar.android.app.robots.onExplanationScreen
import uk.nhs.nhsx.sonar.android.app.robots.onIsolateBottomSheetView
import uk.nhs.nhsx.sonar.android.app.robots.onMedicalWorkersInstructionPanel
import uk.nhs.nhsx.sonar.android.app.robots.onReEnableLocationScreen
import uk.nhs.nhsx.sonar.android.app.robots.onReferenceCodePanel
import uk.nhs.nhsx.sonar.android.app.robots.onStatusFooterView
import uk.nhs.nhsx.sonar.android.app.status.AmberState
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestAppComponent
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.REFERENCE_CODE
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

@RunWith(AndroidJUnit4::class)
class FlowTest {

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*AndroidLocationHelper.requiredLocationPermissions)

    private lateinit var testAppContext: TestApplicationContext
    private val component: TestAppComponent
        get() = testAppContext.component

    @Before
    fun setup() {
        testAppContext = TestApplicationContext(activityRule)
        testAppContext.closeNotificationPanel()
        ensureBluetoothEnabled()
        resetApp()
    }

    private fun resetApp() {
        testAppContext.reset()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(testAppContext.app, FlowTestStartActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }
        instrumentation.startActivitySync(intent)
    }

    @After
    fun teardown() {
        testAppContext.shutdownMockServer()
    }

    @Test
    fun testUnsupportedDevice() {
        testAppContext.simulateUnsupportedDevice()

        onTestStartScreen {
            startMainActivity()
        }
        onDeviceUnsupportedScreen {
            verifyUnsupportedMessageIsShown()
        }
    }

    @Test
    fun testTabletNotSupported() {
        testAppContext.simulateTablet()

        onTestStartScreen {
            startMainActivity()
        }
        onDeviceUnsupportedScreen {
            verifyTabletUnsupportedMessageIsShown()
        }
    }

    @Test
    fun testUnsupportedDeviceOnThePermissionScreen() {

        onTestStartScreen {
            startMainActivity()
        }
        onMainScreen {
            clickOnConfirmButton()
        }
        onPostCodeScreen {
            checkPostCodeActivityIsShown()
            clickOnContinueButton()
            checkInvalidPostCodeHintIsDisplayed()
            typePostCode("E1")

            closeSoftKeyboard()
            testAppContext.simulateUnsupportedDevice()
            clickOnContinueButton()
        }
        onPermissionScreen {
            checkPermissionActivityIsShown()
            clickOnContinueButton()
        }
        onDeviceUnsupportedScreen {
            verifyUnsupportedMessageIsShown()
        }
    }

    @Test
    fun testRegistration() {
        testAppContext.simulateBackendDelay(400)

        onTestStartScreen {
            startMainActivity()
        }
        onMainScreen {
            clickOnConfirmButton()
        }
        onPostCodeScreen {
            checkPostCodeActivityIsShown()
            clickOnContinueButton()
            checkInvalidPostCodeHintIsDisplayed()
            typePostCode("E1")
            closeSoftKeyboard()
            clickOnContinueButton()
        }
        onPermissionScreen {
            checkPermissionActivityIsShown()
            clickOnContinueButton()
        }
        onOkScreen {
            checkOkActivityIsShown()

            onRegistrationPanel {
                checkFinialisingSetupMessageIsShown()
                testAppContext.verifyRegistrationFlow()
                checkWorkingOkMessageIsShown()
            }

            verifyCheckMySymptomsButton(isEnabled())
        }
    }

    @Test
    fun testRegistrationRetry() {
        testAppContext.simulateBackendResponse(error = true)

        onTestStartScreen {
            startMainActivity()
        }
        onMainScreen {
            clickOnConfirmButton()
        }
        onPostCodeScreen {
            checkPostCodeActivityIsShown()
            typePostCode("E1")
            closeSoftKeyboard()
            clickOnContinueButton()
        }
        onPermissionScreen {
            checkPermissionActivityIsShown()
            clickOnContinueButton()
        }
        onOkScreen {
            checkOkActivityIsShown()
            verifyCheckMySymptomsButton(not(isEnabled()))

            onRegistrationPanel {
                checkFinialisingSetupMessageIsShown()

                testAppContext.simulateBackendResponse(error = false)
                testAppContext.verifyRegistrationRetry()

                // job retries after at least 10 seconds
                waitForWorkingOkMessage(getString(R.string.registration_everything_is_working_ok))
                checkWorkingOkMessageIsShown()
            }
            verifyCheckMySymptomsButton(isEnabled())
        }
    }

    @Test
    fun testBluetoothInteractions() {
        setUserState(DefaultState)
        setValidSonarIdAndSecretKeyAndPublicKey()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }
        onOkScreen {
            clickOnNotFeelingWell()
        }

        testAppContext.simulateDeviceInProximity()

        onDiagnosisScreen {
            checkCanTransitionToIsolateActivity()
        }

        testAppContext.verifyReceivedProximityRequest()

        onIsolateScreen {
            checkIsolateActivityIsShown()
        }
    }

    @Test
    fun testReceivingStatusUpdateNotification() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.apply {
            simulateStatusUpdateReceived()
            clickOnNotification(R.string.notification_title, R.string.notification_text)
        }

        onAtRiskScreen {
            checkAtRiskActivityIsShown()
        }
    }

    @Test
    fun testExplanation() {

        onTestStartScreen {
            startMainActivity()
        }
        onOkScreen {
            clickOnExplanationLink()
        }
        onExplanationScreen {
            checkExplanationActivityIsShown()
            clickCloseButton()
        }
        onMainScreen {
            checkMainActivityIsShown()
        }
    }

    @Test
    fun testLaunchWhenStateIsDefault() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }
        onOkScreen {
            checkOkActivityIsShown()
        }
        onMedicalWorkersInstructionPanel {
            checkDisplayOfMedicalWorkersInstructions()
        }
    }

    @Test
    fun testLaunchWhenStateIsAmber() {
        setUserState(AmberState(DateTime.now(DateTimeZone.UTC).plusDays(1)))
        setValidSonarId()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }
        onAtRiskScreen {
            checkAtRiskActivityIsShown()
        }
        onMedicalWorkersInstructionPanel {
            checkDisplayOfMedicalWorkersInstructions()
        }
    }

    @Test
    fun testLaunchWhenStateIsRed() {
        setUserState(
            RedState(
                DateTime.now(DateTimeZone.UTC).plusDays(1),
                nonEmptySetOf(Symptom.TEMPERATURE)
            )
        )
        setValidSonarId()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }
        onIsolateScreen {
            checkIsolateActivityIsShown()
            clickOnReferenceCodeLink()
        }
        onReferenceCodePanel {
            checkDisplayOfReferenceCode()
            clickCloseButton()
        }
        onStatusFooterView {
            checkMedicalWorkersInstructionsNotDisplayed()
        }
    }

    @Test
    fun testLaunchWhenStateIsRedAndExpired() {
        setUserState(
            RedState(
                DateTime.now(DateTimeZone.UTC).minusDays(1),
                nonEmptySetOf(Symptom.TEMPERATURE)
            )
        )
        setValidSonarId()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }
        onIsolateBottomSheetView {
            checkIsolateActivityPopUpIsShown()
        }
    }

    @Test
    fun testExpiredRedStateRevisitsQuestionnaireAndRemainsToRedState() {
        setUserState(
            RedState(
                DateTime.now(DateTimeZone.UTC).minusDays(1),
                nonEmptySetOf(Symptom.TEMPERATURE)
            )
        )
        setValidSonarId()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }
        onIsolateBottomSheetView {
            checkIsolateActivityPopUpIsShown()
            clickOnHaveSymptoms()
        }
        onDiagnosisScreen {
            checkCanTransitionToIsolateActivitySimplified()
        }
        onIsolateScreen {
            checkIsolateActivityIsShown()
        }
    }

    @Test
    fun testLaunchWhenOnboardingIsFinishedButNotRegistered() {
        setFinishedOnboarding()

        onTestStartScreen {
            startMainActivity()
        }
        onOkScreen {
            checkOkActivityIsShown()
        }
    }

    @Test
    fun testOnboarding_WhenPermissionsNeedToBeSet() {
        fun testEnableBluetooth() {

            onPermissionScreen {
                clickOnContinueButton()
            }

            testAppContext.device.apply {
                wait(Until.hasObject(By.textContains("wants to turn on Bluetooth")), 500)
                val buttonText = "Allow"
                val allowButton = findObject(By.text(buttonText))
                if (allowButton != null) {
                    allowButton.click()
                    return@apply
                }
                findObject(By.text(buttonText.toUpperCase())).click()
            }
        }

        fun testGrantLocationPermission() {

            onEdgeCaseScreen {
                checkLocationPermissionMessageIsShown()
                clickOnEnableBlueToothButton()
                // ensure we leave the screen before moving on
                waitUntilScreenIsNotSeen()
            }

            // moving on...
            testAppContext.grantLocationPermission()
            testAppContext.device.pressBack()

            onPermissionScreen {
                checkPermissionActivityIsShown()
            }
        }

        fun testEnableLocationAccess() {
            onView(withId(R.id.permission_continue)).perform(click())

            onView(withId(R.id.edgeCaseTitle)).check(matches(withText(R.string.enable_location_service_title)))

            onView(withId(R.id.takeActionButton)).perform(click())
            testAppContext.enableLocationAccess()
            testAppContext.device.pressBack()

            onPermissionScreen {
                checkPermissionActivityIsShown()
            }
        }

        onBoardUntilPermissionsScreen()

        ensureBluetoothDisabled()
        testAppContext.disableLocationAccess()
        testAppContext.revokeLocationPermission()

        testEnableBluetooth()
        testGrantLocationPermission()
        testEnableLocationAccess()

        onPermissionScreen {
            clickOnContinueButton()
        }
        onOkScreen {
            checkOkActivityIsShown()
        }
    }

    private fun onBoardUntilPermissionsScreen() {
        onTestStartScreen {
            startMainActivity()
        }
        onMainScreen {
            clickOnConfirmButton()
        }
        onPostCodeScreen {
            typePostCode("E1")
            closeSoftKeyboard()
            clickOnContinueButton()
        }
    }

    @Test
    fun testResumeWhenBluetoothIsDisabled() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }
        onOkScreen {
            clickOnLatestAdvice()
        }

        ensureBluetoothDisabled()
        testAppContext.device.pressBack()

        onEdgeCaseScreen {
            checkBlueToothMessageIsDisplayed()
            clickOnEnableBlueToothButton()
        }
        onOkScreen {
            checkTitleIsDisplayed()
        }
    }

    @Test
    fun testResumeWhenLocationAccessIsDisabled() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }

        testAppContext.disableLocationAccess()

        onReEnableLocationScreen {
            checkReEnableLocationTitleIsShown()
        }

        testAppContext.enableLocationAccess()

        onOkScreen {
            checkTitleIsDisplayed()
        }
    }

    @Test
    fun testResumeWhenLocationPermissionIsRevoked() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }
        onOkScreen {
            clickOnLatestAdvice()
        }

        testAppContext.revokeLocationPermission()
        testAppContext.device.pressBack()

        onEdgeCaseScreen {
            checkReAllowLocationPermissionMessageIsShown()
            clickOnEnableBlueToothButton()
        }

        testAppContext.device.wait(
            Until.gone(By.text("Allow this app to access your location to continue")),
            500
        )
        testAppContext.grantLocationPermission()
        testAppContext.device.pressBack()

        onOkScreen {
            checkScreenIsResumed()
        }
    }

    @Test
    fun testEnableBluetoothThroughNotification() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onTestStartScreen {
            startMainActivity()
        }

        ensureBluetoothDisabled()

        testAppContext.clickOnNotificationAction(
            notificationTitleRes = R.string.notification_bluetooth_disabled_title,
            notificationTextRes = R.string.notification_bluetooth_disabled_text,
            notificationActionRes = R.string.notification_bluetooth_disabled_action
        )

        verifyBluetoothIsEnabled()

        onOkScreen {
            checkOkActivityIsShown()
        }
    }

    private fun getString(@StringRes stringId: Int): String {
        val context = testAppContext.app.applicationContext
        return context.getString(stringId)
    }

    private fun setUserState(state: UserState) {
        component.getUserStateStorage().update(state)
    }

    private fun setFinishedOnboarding() {
        val storage = component.getOnboardingStatusProvider()
        storage.setOnboardingFinished(true)
    }

    private fun setValidSonarId() {
        val sonarIdProvider = component.getSonarIdProvider()
        sonarIdProvider.setSonarId(TestSonarServiceDispatcher.RESIDENT_ID)
    }

    private fun setValidSonarIdAndSecretKeyAndPublicKey() {
        setValidSonarId()

        val keyStorage = component.getKeyStorage()
        keyStorage.storeSecretKey(TestSonarServiceDispatcher.encodedSecretKey)
        keyStorage.storeServerPublicKey(TestSonarServiceDispatcher.PUBLIC_KEY)
    }

    private fun setReferenceCode() {
        val refCodeProvider = component.getReferenceCodeProvider()
        refCodeProvider.set(ReferenceCode(REFERENCE_CODE))
    }

    private fun bluetoothAdapter(): BluetoothAdapter {
        val context = testAppContext.app.applicationContext
        val manager = context.getSystemService(BluetoothManager::class.java) as BluetoothManager
        return manager.adapter
    }

    private fun ensureBluetoothEnabled() {
        bluetoothAdapter().let {
            it.enable()
            await until { it.isEnabled }
        }
    }

    private fun verifyBluetoothIsEnabled() {
        bluetoothAdapter().let {
            await until { it.isEnabled }
        }
    }

    private fun ensureBluetoothDisabled() {
        bluetoothAdapter().let {
            it.disable()
            await until { !it.isEnabled }
        }
    }
}
