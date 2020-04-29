/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.EmberState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestAppComponent
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestCoLocateServiceDispatcher
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestCoLocateServiceDispatcher.Companion.REFERENCE_CODE
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import kotlin.test.fail

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
    fun testRunner() {
        val tests = listOf(
            ::testUnsupportedDevice,
            ::testUnsupportedDeviceOnThePermissionScreen,
            ::testRegistration,
            ::testRegistrationRetry,
            ::testBluetoothInteractions,
            ::testReceivingStatusUpdateNotification,
            ::testExplanation,
            ::testLaunchWhenStateIsDefault,
            ::testLaunchWhenStateIsEmber,
            ::testLaunchWhenStateIsRed,
            ::testLaunchWhenStateIsRedAndExpired,
            ::testExpiredRedStateRevisitsQuestionnaireAndRemainsToRedState,
            ::testLaunchWhenOnboardingIsFinishedButNotRegistered,
            ::testOnboarding_WhenPermissionsNeedToBeSet,
            ::testResumeWhenBluetoothIsDisabled,
            ::testResumeWhenLocationAccessIsDisabled,
            ::testResumeWhenLocationPermissionIsRevoked,
            ::testEnableBluetoothThroughNotification
        )

        tests +

        tests.forEach {
            resetApp()
            it()
        }
    }

    fun testUnsupportedDevice() {
        testAppContext.simulateUnsupportedDevice()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkViewHasText(R.id.edgeCaseTitle, R.string.device_not_supported_title)
    }

    fun testUnsupportedDeviceOnThePermissionScreen() {
        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.confirm_onboarding)).perform(click())

        checkPostCodeActivityIsShown()

        onView(withId(R.id.postCodeContinue)).perform(click())

        onView(withId(R.id.invalidPostCodeHint)).check(matches(isDisplayed()))

        onView(withId(R.id.postCodeEditText)).perform(typeText("E1"))
        closeSoftKeyboard()

        testAppContext.simulateUnsupportedDevice()

        onView(withId(R.id.postCodeContinue)).perform(click())

        checkPermissionActivityIsShown()

        onView(withId(R.id.permission_continue)).perform(click())

        checkViewHasText(R.id.edgeCaseTitle, R.string.device_not_supported_title)
    }

    fun testRegistration() {
        testAppContext.simulateBackendDelay(400)

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.confirm_onboarding)).perform(click())

        checkPostCodeActivityIsShown()

        onView(withId(R.id.postCodeContinue)).perform(click())

        onView(withId(R.id.invalidPostCodeHint)).check(matches(isDisplayed()))

        onView(withId(R.id.postCodeEditText)).perform(typeText("E1"))
        closeSoftKeyboard()

        onView(withId(R.id.postCodeContinue)).perform(click())

        checkPermissionActivityIsShown()

        onView(withId(R.id.permission_continue)).perform(click())

        checkOkActivityIsShown()

        checkViewHasText(R.id.registrationStatusText, R.string.registration_finalising_setup)

        testAppContext.verifyRegistrationFlow()
        checkViewHasText(
            R.id.registrationStatusText,
            R.string.registration_everything_is_working_ok
        )
        verifyCheckMySymptomsButton(isEnabled())
    }

    fun testRegistrationRetry() {
        testAppContext.simulateBackendResponse(error = true)

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.confirm_onboarding)).perform(click())

        checkPostCodeActivityIsShown()

        onView(withId(R.id.postCodeEditText)).perform(typeText("E1"))
        closeSoftKeyboard()

        onView(withId(R.id.postCodeContinue)).perform(click())

        checkPermissionActivityIsShown()

        onView(withId(R.id.permission_continue)).perform(click())

        checkOkActivityIsShown()

        checkViewHasText(R.id.registrationStatusText, R.string.registration_finalising_setup)
        verifyCheckMySymptomsButton(not(isEnabled()))

        testAppContext.simulateBackendResponse(error = false)

        testAppContext.verifyRegistrationRetry()

        // job retries after at least 10 seconds
        waitForText(R.string.registration_everything_is_working_ok, timeoutInMs = 20000)

        checkViewHasText(
            R.id.registrationStatusText,
            R.string.registration_everything_is_working_ok
        )
        verifyCheckMySymptomsButton(isEnabled())
    }

    fun testBluetoothInteractions() {
        setUserState(DefaultState())
        setValidSonarIdAndSecretKeyAndPublicKey()

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.simulateDeviceInProximity()

        checkCanTransitionToIsolateActivity()

        testAppContext.verifyReceivedProximityRequest()

        checkIsolateActivityIsShown()
    }

    fun testReceivingStatusUpdateNotification() {
        setUserState(DefaultState())
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.apply {
            simulateStatusUpdateReceived()
            clickOnNotification(R.string.notification_title, R.string.notification_text)
        }

        checkAtRiskActivityIsShown()
    }

    fun testExplanation() {
        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.explanation_link)).perform(scrollTo(), click())

        checkExplanationActivityIsShown()

        onView(withId(R.id.explanation_back)).perform(click())

        checkMainActivityIsShown()
    }

    fun testLaunchWhenStateIsDefault() {
        setUserState(DefaultState())
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkOkActivityIsShown()
        checkDisplayOfReferenceCode()
        checkDisplayOfMedicalWorkersInstructions()
    }

    fun testLaunchWhenStateIsEmber() {
        setUserState(EmberState(DateTime.now(DateTimeZone.UTC).plusDays(1)))
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkAtRiskActivityIsShown()
        checkDisplayOfReferenceCode()
        checkDisplayOfMedicalWorkersInstructions()
    }

    fun testLaunchWhenStateIsRed() {
        setUserState(
            RedState(
                DateTime.now(DateTimeZone.UTC).plusDays(1),
                nonEmptySetOf(Symptom.TEMPERATURE)
            )
        )
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkIsolateActivityIsShown()
        checkDisplayOfReferenceCode()
        checkMedicalWorkersInstructionsNotDisplayed()
    }

    fun testLaunchWhenStateIsRedAndExpired() {
        setUserState(
            RedState(
                DateTime.now(DateTimeZone.UTC).minusDays(1),
                nonEmptySetOf(Symptom.TEMPERATURE)
            )
        )
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkIsolateActivityPopUpIsShown()
    }

    fun testExpiredRedStateRevisitsQuestionnaireAndRemainsToRedState() {
        setUserState(
            RedState(
                DateTime.now(DateTimeZone.UTC).minusDays(1),
                nonEmptySetOf(Symptom.TEMPERATURE)
            )
        )
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkIsolateActivityPopUpIsShown()

        onView(withId(R.id.have_symptoms)).perform(click())

        checkCanTransitionToIsolateActivitySimplified()
    }

    fun testLaunchWhenOnboardingIsFinishedButNotRegistered() {
        setFinishedOnboarding()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkOkActivityIsShown()
    }

    fun testOnboarding_WhenPermissionsNeedToBeSet() {
        fun testEnableBluetooth() {
            onView(withId(R.id.permission_continue)).perform(click())

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
            if (Build.VERSION.SDK_INT >= 29) {
                checkViewHasText(R.id.edgeCaseTitle, R.string.grant_location_permission_title)
            } else {
                checkViewHasText(
                    R.id.edgeCaseTitle,
                    R.string.grant_location_permission_title_pre_10
                )
            }

            onView(withId(R.id.takeActionButton)).perform(click())
            testAppContext.grantLocationPermission()
            testAppContext.device.pressBack()

            checkPermissionActivityIsShown()
        }

        fun testEnableLocationAccess() {
            onView(withId(R.id.permission_continue)).perform(click())

            onView(withId(R.id.edgeCaseTitle)).check(matches(withText(R.string.enable_location_service_title)))

            onView(withId(R.id.takeActionButton)).perform(click())
            testAppContext.enableLocationAccess()
            testAppContext.device.pressBack()

            checkPermissionActivityIsShown()
        }

        onBoardUntilPermissionsScreen()

        ensureBluetoothDisabled()
        testAppContext.disableLocationAccess()
        testAppContext.revokeLocationPermission()

        testEnableBluetooth()
        testGrantLocationPermission()
        testEnableLocationAccess()

        onView(withId(R.id.permission_continue)).perform(click())

        checkOkActivityIsShown()
    }

    private fun onBoardUntilPermissionsScreen() {
        onView(withId(R.id.start_main_activity)).perform(click())
        onView(withId(R.id.confirm_onboarding)).perform(click())
        onView(withId(R.id.postCodeEditText)).perform(typeText("E1"))
        closeSoftKeyboard()
        onView(withId(R.id.postCodeContinue)).perform(click())
    }

    fun testResumeWhenBluetoothIsDisabled() {
        setUserState(DefaultState())
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.latest_advice_ok)).perform(click())
        ensureBluetoothDisabled()
        testAppContext.device.pressBack()

        checkViewHasText(R.id.edgeCaseTitle, R.string.re_enable_bluetooth_title)

        onView(withId(R.id.takeActionButton)).perform(click())

        waitForText(R.string.status_initial_title, timeoutInMs = 6_000)
    }

    fun testResumeWhenLocationAccessIsDisabled() {
        setUserState(DefaultState())
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.disableLocationAccess()

        waitForText(R.string.re_enable_location_title)

        testAppContext.enableLocationAccess()

        waitForText(R.string.status_initial_title)
    }

    fun testResumeWhenLocationPermissionIsRevoked() {
        setUserState(DefaultState())
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.latest_advice_ok)).perform(click())
        testAppContext.revokeLocationPermission()
        testAppContext.device.pressBack()

        checkViewHasText(R.id.edgeCaseTitle, R.string.re_allow_location_permission_title)

        onView(withId(R.id.takeActionButton)).perform(click())
        testAppContext.grantLocationPermission()
        testAppContext.device.pressBack()

        waitForText(R.string.status_initial_title)
    }

    fun testEnableBluetoothThroughNotification() {
        // This test fails on the moto g(6) play device that we use in Firebase test lab
        if (runningInFirebaseTestLab()) {
            Timber.w("Disabling the testEnableBluetoothThroughNotification test as it does not work in Firebase")
            return
        }
        setUserState(DefaultState())
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        ensureBluetoothDisabled()

        testAppContext.clickOnNotificationAction(
            notificationTitleRes = R.string.notification_bluetooth_disabled_title,
            notificationTextRes = R.string.notification_bluetooth_disabled_text,
            notificationActionRes = R.string.notification_bluetooth_disabled_action
        )

        checkViewHasText(R.id.edgeCaseTitle, R.string.re_enable_bluetooth_title)
        verifyBluetoothIsEnabled()
        checkOkActivityIsShown()
    }

    private fun runningInFirebaseTestLab(): Boolean = Settings.System.getString(
        testAppContext.app.contentResolver,
        "firebase.test.lab"
    ) == "true"

    private fun verifyCheckMySymptomsButton(matcher: Matcher<View>) {
        onView(withId(R.id.status_not_feeling_well)).check(matches(matcher))
    }

    private fun waitForText(@StringRes stringId: Int, timeoutInMs: Long = 500) {
        val context = testAppContext.app.applicationContext
        waitForText(context.getString(stringId), timeoutInMs)
    }

    private fun waitForText(text: String, timeoutInMs: Long = 500) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.wait(Until.findObject(By.text(text)), timeoutInMs)
            ?: fail("Timed out waiting for text: $text")
    }

    private fun checkViewHasText(@IdRes viewId: Int, @StringRes stringId: Int) {
        onView(withId(viewId)).check(matches(withText(stringId)))
    }

    private fun checkMainActivityIsShown() {
        onView(withId(R.id.confirm_onboarding)).check(matches(isDisplayed()))
    }

    private fun checkPostCodeActivityIsShown() {
        onView(withId(R.id.postCodeContinue)).check(matches(isDisplayed()))
    }

    private fun checkPermissionActivityIsShown() {
        onView(withId(R.id.permission_continue)).check(matches(isDisplayed()))
    }

    private fun checkExplanationActivityIsShown() {
        onView(withId(R.id.explanation_back)).check(matches(isDisplayed()))
    }

    private fun checkOkActivityIsShown() {
        waitForText(R.string.status_initial_title, 1000)
        onView(withId(R.id.status_initial)).check(matches(isDisplayed()))
    }

    private fun checkAtRiskActivityIsShown() {
        onView(withId(R.id.status_amber)).check(matches(isDisplayed()))
    }

    private fun checkIsolateActivityIsShown() {
        onView(withId(R.id.status_red)).check(matches(isDisplayed()))
    }

    private fun checkIsolateActivityPopUpIsShown() {
        onView(withId(R.id.bottom_sheet_isolate)).check(matches(isDisplayed()))
    }

    private fun checkDisplayOfReferenceCode() {
        onView(withId(R.id.reference_code_link)).perform(scrollTo(), click())

        onView(withId(R.id.reference_code)).check(matches(withText(REFERENCE_CODE)))
        onView(withId(R.id.close)).perform(click())
    }

    private fun checkDisplayOfMedicalWorkersInstructions() {
        onView(withId(R.id.medicalWorkersInstructions)).perform(scrollTo(), click())
        checkViewHasText(
            R.id.medicalWorkersInstructionsTitle,
            R.string.medical_workers_instructions_title
        )
        checkViewHasText(
            R.id.medicalWorkersInstructionsText,
            R.string.medical_workers_instructions_text
        )
        onView(withId(R.id.closeButton)).perform(click())
    }

    private fun checkMedicalWorkersInstructionsNotDisplayed() {
        val notDisplayed = matches(not(isDisplayed()))
        onView(withId(R.id.medicalWorkersInstructions)).check(notDisplayed)
    }

    private fun setUserState(state: UserState) {
        component.getStateStorage().update(state)
    }

    private fun setFinishedOnboarding() {
        val storage = component.getOnboardingStatusProvider()
        storage.setOnboardingFinished(true)
    }

    private fun setValidSonarId() {
        val sonarIdProvider = component.getSonarIdProvider()
        sonarIdProvider.setSonarId(TestCoLocateServiceDispatcher.RESIDENT_ID)
    }

    private fun setValidSonarIdAndSecretKeyAndPublicKey() {
        setValidSonarId()

        val keyStorage = component.getKeyStorage()
        keyStorage.storeSecretKey(TestCoLocateServiceDispatcher.encodedSecretKey)
        keyStorage.storeServerPublicKey(TestCoLocateServiceDispatcher.PUBLIC_KEY)
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

    private fun checkCanTransitionToIsolateActivitySimplified() {

        // Temperature Step
        checkViewHasText(R.id.progress, R.string.progress_half)
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        // Cough Step
        checkViewHasText(R.id.progress, R.string.progress_two_out_of_two)
        onView(withId(R.id.no)).perform(click())
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        checkIsolateActivityIsShown()
    }

    private fun checkCanTransitionToIsolateActivity() {
        onView(withId(R.id.status_not_feeling_well)).perform(scrollTo(), click())

        // Temperature step
        onView(withId(R.id.temperature_question)).check(matches(isDisplayed()))
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.yes)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        // Cough step
        onView(withId(R.id.cough_question)).check(matches(isDisplayed()))
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.yes)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        // Review Step
        onView(withId(R.id.review_answer_temperature)).check(matches(isDisplayed()))

        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo(), click())
        onData(anything()).atPosition(1).perform(click())
        onView(withId(R.id.submit_diagnosis)).perform(click())

        // Confirmation Step
        onView(withId(R.id.submit_events_info)).check(matches(isDisplayed()))
        onView(withId(R.id.submit_diagnosis)).perform(click())

        // Red State
        onView(withId(R.id.status_red)).check(matches(isDisplayed()))
    }
}
