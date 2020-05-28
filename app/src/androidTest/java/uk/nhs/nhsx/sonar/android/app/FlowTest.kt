package uk.nhs.nhsx.sonar.android.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseCloseRobot
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseQuestionRobot
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseReviewRobot
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseSubmitRobot
import uk.nhs.nhsx.sonar.android.app.onboarding.MainOnboardingRobot
import uk.nhs.nhsx.sonar.android.app.onboarding.PermissionRobot
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeRobot
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.StatusRobot
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class FlowTest(private val testAppContext: TestApplicationContext) {

    private val mainOnboardingRobot = MainOnboardingRobot()
    private val postCodeRobot = PostCodeRobot()
    private val permissionRobot = PermissionRobot()
    private val diagnoseQuestionRobot = DiagnoseQuestionRobot()
    private val diagnoseCloseRobot = DiagnoseCloseRobot()
    private val diagnoseReviewRobot = DiagnoseReviewRobot()
    private val diagnoseSubmitRobot = DiagnoseSubmitRobot()
    private val statusRobot = StatusRobot()
    private val bottomDialogRobot = BottomDialogRobot()

    fun testRegistration() {
        testAppContext.simulateBackendDelay(400)

        startMainActivity()
        mainOnboardingRobot.clickConfirmOnboarding()

        postCodeRobot.checkActivityIsDisplayed()
        postCodeRobot.enterPostCode("E1")
        postCodeRobot.clickContinue()

        permissionRobot.checkActivityIsDisplayed()
        permissionRobot.clickContinue()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
        statusRobot.checkFinalisingSetup()

        testAppContext.verifyRegistrationFlow()

        statusRobot.checkFeelUnwellIsDisplayed()
    }

    fun testProximityDataUploadOnSymptomaticState() {
        testAppContext.setFullValidUser()
        startMainActivity()
        testAppContext.simulateDeviceInProximity()

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

        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)
        testAppContext.verifyReceivedProximityRequest()
    }

    fun testQuestionnaireFlowWithNoSymptoms() {
        testAppContext.setFullValidUser()
        startMainActivity()

        clickNotFeelingWellCard()

        diagnoseQuestionRobot.answerNoTo(R.id.temperature_question)
        diagnoseQuestionRobot.answerNoTo(R.id.cough_question)
        diagnoseQuestionRobot.answerNoTo(R.id.anosmia_question)
        diagnoseQuestionRobot.answerNoTo(R.id.sneeze_question)
        diagnoseQuestionRobot.answerNoTo(R.id.stomach_question)

        diagnoseCloseRobot.checkActivityIsDisplayed()
        diagnoseCloseRobot.close()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    fun testReceivingStatusUpdateNotification() {
        testAppContext.setFullValidUser()
        startMainActivity()

        testAppContext.apply {
            simulateStatusUpdateReceived()
            clickOnNotification(R.string.contact_alert_notification_title, R.string.contact_alert_notification_text)
        }

        statusRobot.checkActivityIsDisplayed(ExposedState::class)
    }

    fun testExpiredSymptomaticStateRevisitsQuestionnaireAndRemainsToSymptomaticState() {
        val date = DateTime.now(UTC).minusSeconds(1)
        val expiredSymptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        testAppContext.setFullValidUser(expiredSymptomaticState)
        startMainActivity()

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickFirstCtaButton()

        diagnoseQuestionRobot.checkProgress(R.string.progress_one_third)
        diagnoseQuestionRobot.answerYesTo(R.id.temperature_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_two_third)
        diagnoseQuestionRobot.answerNoTo(R.id.cough_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_three_third)
        diagnoseQuestionRobot.answerNoTo(R.id.anosmia_question)

        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)
    }

    fun testExpiredSymptomaticStateUpdatingWithNoSymptomsNavigatesToOkActivity() {
        val date = DateTime.now(UTC).minusSeconds(1)
        val expiredSymptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        testAppContext.setFullValidUser(expiredSymptomaticState)
        startMainActivity()

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
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
        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    private fun clickNotFeelingWellCard() {
        onView(withId(R.id.feelUnwell)).perform(scrollTo(), click())
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }
}
