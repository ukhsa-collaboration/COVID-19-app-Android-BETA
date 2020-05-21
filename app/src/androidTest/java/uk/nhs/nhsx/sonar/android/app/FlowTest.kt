/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseCloseRobot
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseQuestionRobot
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseReviewActivityTest
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseReviewRobot
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseSubmitActivityTest
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseSubmitRobot
import uk.nhs.nhsx.sonar.android.app.onboarding.PermissionActivityTest
import uk.nhs.nhsx.sonar.android.app.onboarding.PermissionRobot
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeActivityTest
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeRobot
import uk.nhs.nhsx.sonar.android.app.status.AtRiskActivityTest
import uk.nhs.nhsx.sonar.android.app.status.AtRiskRobot
import uk.nhs.nhsx.sonar.android.app.status.BaseActivityTest
import uk.nhs.nhsx.sonar.android.app.status.IsolateActivityTest
import uk.nhs.nhsx.sonar.android.app.status.IsolateRobot
import uk.nhs.nhsx.sonar.android.app.status.OkActivityTest
import uk.nhs.nhsx.sonar.android.app.status.OkRobot
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestAppComponent
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
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
    private val diagnoseQuestionRobot = DiagnoseQuestionRobot()
    private val diagnoseCloseRobot = DiagnoseCloseRobot()
    private val diagnoseReviewRobot = DiagnoseReviewRobot()
    private val diagnoseSubmitRobot = DiagnoseSubmitRobot()
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

            { PostCodeActivityTest(testAppContext).pristineState() },
            { PostCodeActivityTest(testAppContext).emptyPostCodeShowsInvalidHint() },
            { PostCodeActivityTest(testAppContext).invalidPostCodeShowsInvalidHint() },
            { PostCodeActivityTest(testAppContext).validPostCodeProceedsToNextView() },

            { PermissionActivityTest(testAppContext).testUnsupportedDevice() },
            { PermissionActivityTest(testAppContext).testEnableBluetooth() },
            { PermissionActivityTest(testAppContext).testGrantLocationPermission() },
            { PermissionActivityTest(testAppContext).testEnableLocationAccess() },

            { BaseActivityTest(testAppContext).testResumeWhenBluetoothIsDisabled() },
            { BaseActivityTest(testAppContext).testResumeWhenLocationAccessIsDisabled() },
            { BaseActivityTest(testAppContext).testResumeWhenLocationPermissionIsRevoked() },

            { OkActivityTest(testAppContext).testRegistrationRetry() },
            { OkActivityTest(testAppContext).testRegistrationPushNotificationNotReceived() },

            { AtRiskActivityTest(testAppContext).testHideStatusUpdateNotificationWhenNotClicked() },

            { DiagnoseReviewActivityTest(testAppContext).testDisplayingYesAnswers() },
            { DiagnoseReviewActivityTest(testAppContext).testDisplayingNoAnswers() },
            { DiagnoseReviewActivityTest(testAppContext).testSubmittingWithoutDate() },
            { DiagnoseReviewActivityTest(testAppContext).testShowingCalendarAndCanceling() },
            { DiagnoseReviewActivityTest(testAppContext).testSelectingTodayFromCalendar() },
            { DiagnoseReviewActivityTest(testAppContext).testSelectingYesterdayFromSpinner() },

            { DiagnoseSubmitActivityTest(testAppContext).testConfirmationIsRequired() },

            { IsolateActivityTest(testAppContext).testWhenStateIsExpired() },
            { IsolateActivityTest(testAppContext).testClickOrderTestCardShowsApplyForTest() },

            ::testRegistration,
            ::testProximityDataUploadOnSymptomaticState,
            ::testQuestionnaireFlowWithNoSymptom,
            ::testReceivingStatusUpdateNotification,
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
        testAppContext.setFullValidUser()
        startMainActivity()
        testAppContext.simulateDeviceInProximity()

        checkQuestionnaireFlowWithSymptoms()

        testAppContext.verifyReceivedProximityRequest()
        isolateRobot.checkActivityIsDisplayed()
    }

    fun testQuestionnaireFlowWithNoSymptom() {
        testAppContext.setFullValidUser()
        startMainActivity()

        checkQuestionnaireFlowWithNoSymptom()

        okRobot.checkActivityIsDisplayed()
    }

    fun testReceivingStatusUpdateNotification() {
        testAppContext.setFullValidUser()
        startMainActivity()

        testAppContext.apply {
            simulateStatusUpdateReceived()
            clickOnNotification(R.string.notification_title, R.string.notification_text)
        }

        atRiskRobot.checkActivityIsDisplayed()
    }

    fun testExpiredRedStateRevisitsQuestionnaireAndRemainsToRedState() {
        val expiredRedState = RedState(DateTime.now(UTC).minusSeconds(1), nonEmptySetOf(TEMPERATURE))

        testAppContext.setFullValidUser(expiredRedState)
        startMainActivity()

        isolateRobot.checkPopUpIsDisplayed()

        onView(withId(R.id.have_symptoms)).perform(click())

        checkCanTransitionToIsolateActivitySimplified()
    }

    fun testEnableBluetoothThroughNotification() {
        testAppContext.setFullValidUser()
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

    private fun checkCanTransitionToIsolateActivitySimplified() {
        diagnoseQuestionRobot.checkProgress(R.string.progress_one_third)
        diagnoseQuestionRobot.answerYesTo(R.id.temperature_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_two_third)
        diagnoseQuestionRobot.answerNoTo(R.id.cough_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_three_third)
        diagnoseQuestionRobot.answerNoTo(R.id.anosmia_question)

        isolateRobot.checkActivityIsDisplayed()
    }

    private fun checkQuestionnaireFlowWithNoSymptom() {
        clickNotFeelingWellCard()

        diagnoseQuestionRobot.answerNoTo(R.id.temperature_question)
        diagnoseQuestionRobot.answerNoTo(R.id.cough_question)
        diagnoseQuestionRobot.answerNoTo(R.id.anosmia_question)
        diagnoseQuestionRobot.answerNoTo(R.id.sneeze_question)
        diagnoseQuestionRobot.answerNoTo(R.id.stomach_question)

        diagnoseCloseRobot.checkActivityIsDisplayed()
        diagnoseCloseRobot.close()
    }

    private fun checkQuestionnaireFlowWithSymptoms() {
        clickNotFeelingWellCard()

        diagnoseQuestionRobot.answerYesTo(R.id.temperature_question)
        diagnoseQuestionRobot.answerYesTo(R.id.cough_question)
        diagnoseQuestionRobot.answerYesTo(R.id.anosmia_question)
        diagnoseQuestionRobot.answerYesTo(R.id.sneeze_question)
        diagnoseQuestionRobot.answerYesTo(R.id.stomach_question)

        diagnoseReviewRobot.checkActivityIsDisplayed()
        diagnoseReviewRobot.selectYesterday()
        diagnoseReviewRobot.submit()

        diagnoseSubmitRobot.checkActivityIsDisplayed()
        diagnoseSubmitRobot.selectConfirmation()
        diagnoseSubmitRobot.submit()

        isolateRobot.checkActivityIsDisplayed()
    }

    private fun clickNotFeelingWellCard() {
        onView(withId(R.id.status_not_feeling_well)).perform(scrollTo(), click())
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
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
