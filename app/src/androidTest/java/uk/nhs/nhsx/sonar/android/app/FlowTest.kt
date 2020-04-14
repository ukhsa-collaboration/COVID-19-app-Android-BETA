/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.bluetooth.BluetoothManager
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.closeSoftKeyboard
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
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.persistence.SharedPreferencesSonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.CovidStatus
import uk.nhs.nhsx.sonar.android.app.status.SharedPreferencesStatusStorage
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestCoLocateServiceDispatcher
import uk.nhs.nhsx.sonar.android.app.testhelpers.hasTextInputLayoutErrorText
import uk.nhs.nhsx.sonar.android.app.testhelpers.isToast

@RunWith(AndroidJUnit4::class)
class FlowTest {

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(ACCESS_FINE_LOCATION)

    private lateinit var testAppContext: TestApplicationContext

    @Before
    fun setup() {
        testAppContext = TestApplicationContext(activityRule)
        ensureBluetoothEnabled()
    }

    @After
    fun teardown() {
        testAppContext.shutdownMockServer()
    }

    @Test
    fun testRegistration() {
        resetStatusStorage()
        unsetSonarId()
        testAppContext.simulateBackendDelay(400)

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.confirm_onboarding)).perform(click())

        checkPostCodeActivityIsShown()

        onView(withId(R.id.postCodeContinue)).perform(click())

        onView(withId(R.id.postCodeTextInputLayout)).check(matches(hasTextInputLayoutErrorText(R.string.valid_post_code_is_required)))

        onView(withId(R.id.postCodeEditText)).perform(typeText("E1"))
        closeSoftKeyboard()

        onView(withId(R.id.postCodeContinue)).perform(click())

        checkPermissionActivityIsShown()

        onView(withId(R.id.permission_continue)).perform(click())

        checkOkActivityIsShown()

        testAppContext.simulateActivationCodeReceived()

        checkViewHasText(
            R.id.registrationStatusText,
            R.string.registration_finalising_setup
        )

        testAppContext.verifyRegistrationFlow()
        checkViewHasText(
            R.id.registrationStatusText,
            R.string.registration_everything_is_working_ok
        )
        verifyCheckMySymptomsButton(isEnabled())
    }

    @Test
    fun testRegistrationRetry() {
        resetStatusStorage()
        unsetSonarId()
        testAppContext.simulateBackendResponse(error = true)
        testAppContext.simulateBackendDelay(0)

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.confirm_onboarding)).perform(click())

        checkPostCodeActivityIsShown()

        onView(withId(R.id.postCodeEditText)).perform(typeText("E1"))
        closeSoftKeyboard()

        onView(withId(R.id.postCodeContinue)).perform(click())

        checkPermissionActivityIsShown()

        onView(withId(R.id.permission_continue)).perform(click())

        checkOkActivityIsShown()

        testAppContext.verifyReceivedRegistrationRequest()
        checkViewHasText(
            R.id.registrationStatusText,
            R.string.registration_finalising_setup
        )
        verifyCheckMySymptomsButton(not(isEnabled()))

        waitForText(R.string.registration_app_setup_failed, timeoutInMs = 2_000)

        checkViewHasText(
            R.id.registrationStatusText,
            R.string.registration_app_setup_failed
        )
        onView(withId(R.id.registrationRetryButton)).check(matches(isDisplayed()))
        verifyCheckMySymptomsButton(not(isEnabled()))

        testAppContext.simulateBackendResponse(error = false)

        onView(withId(R.id.registrationRetryButton)).perform(click())

        testAppContext.simulateActivationCodeReceived()

        testAppContext.verifyRegistrationFlow()
        checkViewHasText(
            R.id.registrationStatusText,
            R.string.registration_everything_is_working_ok
        )
        onView(withId(R.id.registrationRetryButton)).check(matches(not(isDisplayed())))
        verifyCheckMySymptomsButton(isEnabled())
    }

    private fun verifyCheckMySymptomsButton(matcher: Matcher<View>) {
        onView(withId(R.id.status_not_feeling_well)).check(matches(matcher))
    }

    @Test
    fun testBluetoothInteractions() {
        clearDatabase()
        setStatus(CovidStatus.OK)
        setValidSonarIdAndSecretKey()

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.simulateDeviceInProximity()

        checkCanTransitionToIsolateActivity()

        testAppContext.verifyReceivedProximityRequest()

        onView(withText(R.string.successfull_data_upload))
            .inRoot(isToast())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testReceivingStatusUpdateNotification() {
        setStatus(CovidStatus.OK)
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        testAppContext.apply {
            simulateStatusUpdateReceived()
            clickOnStatusNotification()
        }

        checkAtRiskActivityIsShown()
    }

    @Test
    fun testExplanation() {
        unsetSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.explanation_link)).perform(click())

        checkExplanationActivityIsShown()

        onView(withId(R.id.explanation_back)).perform(click())

        checkMainActivityIsShown()
    }

    @Test
    fun testLaunchWhenStateIsOk() {
        setStatus(CovidStatus.OK)
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkOkActivityIsShown()
    }

    @Test
    fun testLaunchWhenStateIsPotential() {
        setStatus(CovidStatus.POTENTIAL)
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkAtRiskActivityIsShown()
    }

    @Test
    fun testLaunchWhenStateIsRed() {
        setStatus(CovidStatus.RED)
        setValidSonarId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkIsolateActivityIsShown()
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

    private fun setStatus(covidStatus: CovidStatus) {
        val storage = activityRule.activity.statusStorage as SharedPreferencesStatusStorage
        storage.update(covidStatus)
    }

    private fun resetStatusStorage() {
        val storage = activityRule.activity.statusStorage as SharedPreferencesStatusStorage
        storage.reset()
    }

    private fun unsetSonarId() {
        val sonarIdProvider =
            activityRule.activity.sonarIdProvider as SharedPreferencesSonarIdProvider
        sonarIdProvider.clear()
    }

    private fun setValidSonarId() {
        val sonarIdProvider = activityRule.activity.sonarIdProvider
        sonarIdProvider.setSonarId(TestCoLocateServiceDispatcher.RESIDENT_ID)
    }

    private fun setValidSonarIdAndSecretKey() {
        setValidSonarId()

        val keyStorage = activityRule.activity.encryptionKeyStorage
        keyStorage.putBase64Key(TestCoLocateServiceDispatcher.encodedKey)
    }

    private fun clearDatabase() {
        val appDb = activityRule.activity.appDatabase
        appDb.clearAllTables()
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
        onView(withId(R.id.review_description)).check(matches(isDisplayed()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        onView(withId(R.id.status_red)).check(matches(isDisplayed()))
    }
}
