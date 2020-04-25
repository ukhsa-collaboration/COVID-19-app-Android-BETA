/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
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
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.EmberState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestAppComponent
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestCoLocateServiceDispatcher
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestCoLocateServiceDispatcher.Companion.REFERENCE_CODE

@RunWith(AndroidJUnit4::class)
class FlowTest {

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*permissions().toTypedArray())

    private fun permissions() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(ACCESS_BACKGROUND_LOCATION, ACCESS_FINE_LOCATION)
    } else {
        listOf(ACCESS_COARSE_LOCATION)
    }

    private lateinit var testAppContext: TestApplicationContext
    private val component: TestAppComponent
        get() = testAppContext.component

    @Before
    fun setup() {
        testAppContext = TestApplicationContext(activityRule)
        ensureBluetoothEnabled()
        testAppContext.closeNotificationPanel()
    }

    private fun resetApp() {
        component.apply {
            getAppDatabase().clearAllTables()
            getOnboardingStatusProvider().setOnboardingFinished(false)
            getStateStorage().clear()
            getSonarIdProvider().clear()
            getActivationCodeProvider().clear()
        }
        testAppContext.resetTestMockServer()

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
            ::testLaunchWhenOnboardingIsFinishedButNotRegistered
        )

        tests.forEach {
            resetApp()
            it()
        }
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
    }

    fun testLaunchWhenStateIsEmber() {
        setUserState(EmberState(DateTime.now(DateTimeZone.UTC).plusDays(1)))
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkAtRiskActivityIsShown()
        checkDisplayOfReferenceCode()
    }

    fun testLaunchWhenStateIsRed() {
        setUserState(
            RedState(
                DateTime.now(DateTimeZone.UTC).plusDays(1),
                setOf(Symptom.TEMPERATURE)
            )
        )
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkIsolateActivityIsShown()
        checkDisplayOfReferenceCode(scroll = false)
    }

    fun testLaunchWhenStateIsRedAndExpired() {
        setUserState(
            RedState(
                DateTime.now(DateTimeZone.UTC).minusDays(1),
                setOf(Symptom.TEMPERATURE)
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
                setOf(Symptom.TEMPERATURE)
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

    private fun verifyCheckMySymptomsButton(matcher: Matcher<View>) {
        onView(withId(R.id.status_not_feeling_well)).check(matches(matcher))
    }

    private fun waitForText(@StringRes stringId: Int, timeoutInMs: Long = 500) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = activityRule.activity
        val text = context.getString(stringId)

        device.wait(Until.findObject(By.text(text)), timeoutInMs)
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

    private fun checkDisplayOfReferenceCode(scroll: Boolean = true) {
        if (scroll)
            onView(withId(R.id.reference_code_link)).perform(scrollTo(), click())
        else
            onView(withId(R.id.reference_code_link)).perform(click())

        onView(withId(R.id.reference_code)).check(matches(withText(REFERENCE_CODE)))
        onView(withId(R.id.close)).perform(click())
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

    private fun ensureBluetoothEnabled() {
        val activity = activityRule.activity
        val context = activity.application.applicationContext
        val manager = context.getSystemService(BluetoothManager::class.java) as BluetoothManager
        val adapter = manager.adapter

        adapter.enable()

        await until {
            adapter.isEnabled
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

        onView(withId(R.id.status_red)).check(matches(isDisplayed()))
    }
}
