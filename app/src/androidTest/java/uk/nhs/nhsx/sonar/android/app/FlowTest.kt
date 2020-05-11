/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCode
import uk.nhs.nhsx.sonar.android.app.status.AmberState
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.SetChecked.Companion.setChecked
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestAppComponent
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.REFERENCE_CODE
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
            ::testTabletNotSupported,
            ::testRegistration,
            ::testRegistrationRetry,
            ::testBluetoothInteractions,
            ::testReceivingStatusUpdateNotification,
            ::testHideStatusUpdateNotificationWhenNotClicked,
            ::testExplanation,
            ::testLaunchWhenStateIsDefault,
            ::testLaunchWhenStateIsAmber,
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

    fun testTabletNotSupported() {
        testAppContext.simulateTablet()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkViewHasText(R.id.edgeCaseTitle, R.string.tablet_support_title)
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
        setUserState(DefaultState)
        setValidSonarIdAndSecretKeyAndPublicKey()
        setReferenceCode()

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.simulateDeviceInProximity()

        checkCanTransitionToIsolateActivity()

        testAppContext.verifyReceivedProximityRequest()

        checkIsolateActivityIsShown()
    }

    fun testReceivingStatusUpdateNotification() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.apply {
            simulateStatusUpdateReceived()
            clickOnNotification(R.string.notification_title, R.string.notification_text)
        }

        checkAtRiskActivityIsShown()
    }

    fun testHideStatusUpdateNotificationWhenNotClicked() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        val notificationTitle = R.string.notification_title

        testAppContext.apply {
            simulateStatusUpdateReceived()
            isNotificationDisplayed(notificationTitle, isDisplayed = true)
        }

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.apply {
            isNotificationDisplayed(notificationTitle, isDisplayed = false)
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
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkOkActivityIsShown()
        checkDisplayOfMedicalWorkersInstructions()
    }

    fun testLaunchWhenStateIsAmber() {
        setUserState(AmberState(DateTime.now(DateTimeZone.UTC).plusDays(1)))
        setValidSonarId()
        setReferenceCode()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkAtRiskActivityIsShown()
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
        setReferenceCode()

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
        setReferenceCode()

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
        setReferenceCode()

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

    private fun findButton(text: String): UiObject2? =
        testAppContext.device.let {
            it.findObject(By.text(text))
                ?: it.findObject(By.text(text.toLowerCase()))
                ?: it.findObject(By.text(text.toUpperCase()))
        }

    fun testOnboarding_WhenPermissionsNeedToBeSet() {
        fun testEnableBluetooth() {
            onView(withId(R.id.permission_continue)).perform(click())

            testAppContext.device.apply {
                wait(Until.hasObject(By.textContains("wants to turn on Bluetooth")), 500)

                val button = sequenceOf("Allow", "Yes", "Ok", "Accept")
                    .mapNotNull(::findButton)
                    .firstOrNull()
                    ?: fail("Looks like we could not find the acceptance button for bluetooth.")

                button.click()
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

            // ensure we leave the screen before moving on
            waitUntilCannotFindText(R.string.grant_location_permission_title)
            waitUntilCannotFindText(R.string.grant_location_permission_title_pre_10)

            // moving on...
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
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.latest_advice_ok)).perform(click())
        ensureBluetoothDisabled()
        testAppContext.device.pressBack()

        checkViewHasText(R.id.edgeCaseTitle, R.string.re_enable_bluetooth_title)

        onView(withId(R.id.takeActionButton)).perform(click())

        waitForText(R.string.status_initial_title, timeoutInMs = 6_000)
    }

    fun testResumeWhenLocationAccessIsDisabled() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.disableLocationAccess()

        waitForText(R.string.re_enable_location_title)

        testAppContext.enableLocationAccess()

        waitForText(R.string.status_initial_title)
    }

    fun testResumeWhenLocationPermissionIsRevoked() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.latest_advice_ok)).perform(click())
        testAppContext.revokeLocationPermission()
        testAppContext.device.pressBack()

        checkViewHasText(R.id.edgeCaseTitle, R.string.re_allow_location_permission_title)

        onView(withId(R.id.takeActionButton)).perform(click())
        testAppContext.device.wait(
            Until.gone(By.text("Allow this app to access your location to continue")),
            500
        )
        testAppContext.grantLocationPermission()
        testAppContext.device.pressBack()

        waitForText(R.string.status_initial_title)
    }

    fun testEnableBluetoothThroughNotification() {
        setUserState(DefaultState)
        setValidSonarId()
        setReferenceCode()

        onView(withId(R.id.start_main_activity)).perform(click())

        ensureBluetoothDisabled()

        testAppContext.clickOnNotificationAction(
            notificationTitleRes = R.string.notification_bluetooth_disabled_title,
            notificationTextRes = R.string.notification_bluetooth_disabled_text,
            notificationActionRes = R.string.notification_bluetooth_disabled_action
        )

        verifyBluetoothIsEnabled()
        checkOkActivityIsShown()
    }

    private fun verifyCheckMySymptomsButton(matcher: Matcher<View>) {
        onView(withId(R.id.status_not_feeling_well)).check(matches(matcher))
    }

    private fun waitUntilCannotFindText(@StringRes stringId: Int, timeoutInMs: Long = 500) {
        testAppContext.device.wait(Until.gone(By.text(getString(stringId))), timeoutInMs)
    }

    private fun getString(@StringRes stringId: Int): String {
        val context = testAppContext.app.applicationContext
        return context.getString(stringId)
    }

    private fun waitForText(@StringRes stringId: Int, timeoutInMs: Long = 500) {
        waitForText(getString(stringId), timeoutInMs)
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
        onView(withId(R.id.reference_link_card)).perform(scrollTo(), click())

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
        onView(withId(R.id.medicalWorkersInstructions)).check(doesNotExist())
    }

    private fun setUserState(state: UserState) {
        component.getUserStateStorage().set(state)
    }

    private fun setFinishedOnboarding() {
        val storage = component.getOnboardingStatusProvider()
        storage.set(true)
    }

    private fun setValidSonarId() {
        val sonarIdProvider = component.getSonarIdProvider()
        sonarIdProvider.set(TestSonarServiceDispatcher.RESIDENT_ID)
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
        onView(withId(R.id.symptoms_date_prompt))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.symptoms_date_prompt_all)))

        onView(withId(R.id.review_answer_temperature))
            .check(matches(withText(R.string.i_do_temperature)))

        onView(withId(R.id.review_answer_cough))
            .check(matches(withText(R.string.i_do_cough)))

        onView(withId(R.id.submit_diagnosis)).perform(click())
        onView(withId(R.id.date_selection_error)).check(matches(isDisplayed()))

        onView(withId(R.id.symptoms_date_spinner)).check(matches(withSpinnerText(R.string.start_date)))

        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo(), click())
        onData(anything()).atPosition(3).perform(click())
        onView(withText("Cancel")).perform(click())
        onView(withId(R.id.symptoms_date_spinner)).check(matches(withSpinnerText(R.string.start_date)))

        val todayAsString = LocalDate.now().toString("EEEE, MMMM dd")
        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo(), click())
        onData(anything()).atPosition(3).perform(click())
        onView(withText("OK")).perform(click())
        onView(withId(R.id.symptoms_date_spinner)).check(matches(withSpinnerText(todayAsString)))

        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo(), click())
        onData(anything()).atPosition(1).perform(click())
        onView(withId(R.id.symptoms_date_spinner)).check(matches(withSpinnerText(R.string.yesterday)))

        onView(withId(R.id.submit_diagnosis)).perform(click())

        // Confirmation Step
        onView(withId(R.id.submit_events_info)).check(matches(isDisplayed()))
        onView(withId(R.id.submit_diagnosis)).perform(click())

        onView(withId(R.id.needConfirmationHint)).check(matches(isDisplayed()))
        onView(withId(R.id.confirmationCheckbox)).perform(scrollTo(), setChecked(true))
        onView(withId(R.id.submit_diagnosis)).perform(click())

        // Red State
        onView(withId(R.id.status_red)).check(matches(isDisplayed()))
    }
}
