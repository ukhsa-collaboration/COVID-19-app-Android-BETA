/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.anything
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.onboarding.PermissionActivityTest
import uk.nhs.nhsx.sonar.android.app.onboarding.PermissionRobot
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeRobot
import uk.nhs.nhsx.sonar.android.app.status.AtRiskRobot
import uk.nhs.nhsx.sonar.android.app.status.BaseActivityTest
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.IsolateActivityTest
import uk.nhs.nhsx.sonar.android.app.status.IsolateRobot
import uk.nhs.nhsx.sonar.android.app.status.OkActivityTest
import uk.nhs.nhsx.sonar.android.app.status.OkRobot
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.testhelpers.SetChecked.Companion.setChecked
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestAppComponent
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText
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

    lateinit var testAppContext: TestApplicationContext
    private val app: SonarApplication get() = testAppContext.app
    private val component: TestAppComponent get() = testAppContext.component

    private val mainRobot = MainRobot()
    private val postCodeRobot = PostCodeRobot()
    private val permissionRobot = PermissionRobot()
    private val okRobot: OkRobot get() = OkRobot(app)
    private val atRiskRobot = AtRiskRobot()
    private val isolateRobot = IsolateRobot()

    @Before
    fun setup() {
        testAppContext = TestApplicationContext(activityRule)
        testAppContext.closeNotificationPanel()
        testAppContext.ensureBluetoothEnabled()
    }

    private fun resetApp() {
        testAppContext.reset()
        app.startTestActivity<FlowTestStartActivity>()
    }

    @After
    fun teardown() {
        testAppContext.shutdownMockServer()
    }

    @Test
    fun testRunner() {
        val tests = listOf<() -> Unit>(
            { MainActivityTest(testAppContext).testExplanation() },
            { MainActivityTest(testAppContext).testUnsupportedDevice() },
            { MainActivityTest(testAppContext).testTabletNotSupported() },
            { MainActivityTest(testAppContext).testLaunchWhenOnboardingIsFinishedButNotRegistered() },
            { MainActivityTest(testAppContext).testLaunchWhenStateIsDefault() },
            { MainActivityTest(testAppContext).testLaunchWhenStateIsAmber() },
            { MainActivityTest(testAppContext).testLaunchWhenStateIsRed() },

            { PermissionActivityTest(testAppContext).testUnsupportedDevice() },
            { PermissionActivityTest(testAppContext).testEnableBluetooth() },
            { PermissionActivityTest(testAppContext).testGrantLocationPermission() },
            { PermissionActivityTest(testAppContext).testEnableLocationAccess() },

            { BaseActivityTest(testAppContext).testResumeWhenBluetoothIsDisabled() },
            { BaseActivityTest(testAppContext).testResumeWhenLocationAccessIsDisabled() },
            { BaseActivityTest(testAppContext).testResumeWhenLocationPermissionIsRevoked() },

            { OkActivityTest(testAppContext).testRegistrationRetry() },
            { OkActivityTest(testAppContext).testRegistrationPushNotificationNotReceived() },

            { IsolateActivityTest(testAppContext).testWhenStateIsExpired() },
            { IsolateActivityTest(testAppContext).testClickOrderTestCardShowsApplyForTest() },

            ::testRegistration,
            ::testProximityDataUploadOnSymptomaticState,
            ::testQuestionnaireFlowWithNoSymptom,
            ::testReceivingStatusUpdateNotification,
            ::testHideStatusUpdateNotificationWhenNotClicked,
            ::testExpiredRedStateRevisitsQuestionnaireAndRemainsToRedState,
            ::testEnableBluetoothThroughNotification
        )

        tests.forEach {
            resetApp()
            it()
        }
    }

    fun testRegistration() {
        testAppContext.simulateBackendDelay(400)

        startMainActivity()
        mainRobot.clickConfirmOnboarding()

        postCodeRobot.checkActivityIsDisplayed()
        postCodeRobot.clickContinue()
        postCodeRobot.checkInvalidHintIsDisplayed()
        postCodeRobot.enterPostCode("E1")
        postCodeRobot.clickContinue()

        permissionRobot.checkActivityIsDisplayed()
        permissionRobot.clickContinue()

        okRobot.checkActivityIsDisplayed()
        okRobot.checkFinalisingSetup()

        testAppContext.verifyRegistrationFlow()

        okRobot.checkEverythingIsWorking()
    }

    fun testProximityDataUploadOnSymptomaticState() {
        testAppContext.setUserState(DefaultState)
        testAppContext.setValidSonarIdAndSecretKeyAndPublicKey()
        testAppContext.setReferenceCode()

        startMainActivity()

        testAppContext.simulateDeviceInProximity()

        checkQuestionnaireFlowWithSymptoms()

        testAppContext.verifyReceivedProximityRequest()

        isolateRobot.checkActivityIsDisplayed()
    }

    fun testQuestionnaireFlowWithNoSymptom() {
        testAppContext.setUserState(DefaultState)
        testAppContext.setValidSonarIdAndSecretKeyAndPublicKey()
        testAppContext.setReferenceCode()

        startMainActivity()
        checkQuestionnaireFlowWithNoSymptom()

        okRobot.checkActivityIsDisplayed()
    }

    fun testReceivingStatusUpdateNotification() {
        testAppContext.setUserState(DefaultState)
        testAppContext.setValidSonarId()
        testAppContext.setReferenceCode()

        startMainActivity()

        testAppContext.apply {
            simulateStatusUpdateReceived()
            clickOnNotification(R.string.notification_title, R.string.notification_text)
        }

        atRiskRobot.checkActivityIsDisplayed()
    }

    fun testHideStatusUpdateNotificationWhenNotClicked() {
        testAppContext.setUserState(DefaultState)
        testAppContext.setValidSonarId()
        testAppContext.setReferenceCode()

        val notificationTitle = R.string.notification_title
        testAppContext.simulateStatusUpdateReceived()
        testAppContext.isNotificationDisplayed(notificationTitle, isDisplayed = true)

        startMainActivity()

        testAppContext.isNotificationDisplayed(notificationTitle, isDisplayed = false)

        atRiskRobot.checkActivityIsDisplayed()
    }

    fun testExpiredRedStateRevisitsQuestionnaireAndRemainsToRedState() {
        val expiredRedState = RedState(DateTime.now(UTC).minusSeconds(1), nonEmptySetOf(TEMPERATURE))

        testAppContext.setUserState(expiredRedState)
        testAppContext.setValidSonarId()
        testAppContext.setReferenceCode()

        startMainActivity()

        isolateRobot.checkPopUpIsDisplayed()

        onView(withId(R.id.have_symptoms)).perform(click())

        checkCanTransitionToIsolateActivitySimplified()
    }

    fun testEnableBluetoothThroughNotification() {
        testAppContext.setUserState(DefaultState)
        testAppContext.setValidSonarId()
        testAppContext.setReferenceCode()

        startMainActivity()

        testAppContext.ensureBluetoothDisabled()

        testAppContext.clickOnNotificationAction(
            notificationTitleRes = R.string.notification_bluetooth_disabled_title,
            notificationTextRes = R.string.notification_bluetooth_disabled_text,
            notificationActionRes = R.string.notification_bluetooth_disabled_action
        )

        testAppContext.verifyBluetoothIsEnabled()
        okRobot.checkActivityIsDisplayed()
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }

    private fun checkCanTransitionToIsolateActivitySimplified() {

        // Temperature Step
        checkViewHasText(R.id.progress, R.string.progress_one_third)
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        // Cough Step
        checkViewHasText(R.id.progress, R.string.progress_two_third)
        onView(withId(R.id.no)).perform(click())
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        // Anosmia Step
        checkViewHasText(R.id.progress, R.string.progress_three_third)
        onView(withId(R.id.no)).perform(click())
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        isolateRobot.checkActivityIsDisplayed()
    }

    private fun checkQuestionnaireFlowWithNoSymptom() {
        onView(withId(R.id.status_not_feeling_well)).perform(scrollTo(), click())

        answerNoTo(R.id.temperature_question)
        answerNoTo(R.id.cough_question)
        answerNoTo(R.id.anosmia_question)
        answerNoTo(R.id.sneeze_question)
        answerNoTo(R.id.stomach_question)

        // Close Activity
        onView(withId(R.id.close_review_btn)).check(matches(isDisplayed()))
        onView(withId(R.id.close_review_btn)).perform(click())
    }

    private fun checkQuestionnaireFlowWithSymptoms() {
        onView(withId(R.id.status_not_feeling_well)).perform(scrollTo(), click())

        answerYesTo(R.id.temperature_question)
        answerYesTo(R.id.cough_question)
        answerYesTo(R.id.anosmia_question)
        answerYesTo(R.id.sneeze_question)
        answerYesTo(R.id.stomach_question)

        // Review Step
        onView(withId(R.id.symptoms_date_prompt))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.symptoms_date_prompt_all)))

        onView(withId(R.id.review_answer_temperature))
            .check(matches(withText(R.string.i_do_temperature)))

        onView(withId(R.id.review_answer_cough))
            .check(matches(withText(R.string.i_do_cough)))

        onView(withId(R.id.submit_diagnosis)).perform(click())
        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo())
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
        onView(withId(R.id.status_red_title)).check(matches(isDisplayed()))
    }

    private fun answerYesTo(questionId: Int) {
        answerTo(questionId, R.id.yes)
    }

    private fun answerNoTo(questionId: Int) {
        answerTo(questionId, R.id.no)
    }

    private fun answerTo(questionId: Int, answerId: Int) {
        onView(withId(questionId)).check(matches(isDisplayed()))
        onView(withId(answerId)).perform(click())
        onView(withId(answerId)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())
    }
}

inline fun <reified T : Activity> Context.startTestActivity(config: Intent.() -> Unit = {}) {
    val intent = Intent(this, T::class.java)
        .apply { addFlags(FLAG_ACTIVITY_NEW_TASK) }
        .apply(config)

    InstrumentationRegistry
        .getInstrumentation()
        .startActivitySync(intent)
}
