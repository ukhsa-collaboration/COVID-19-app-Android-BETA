package uk.nhs.nhsx.sonar.android.app.scenarios

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.FlowTestStartActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseCloseRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseQuestionRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseReviewRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseSubmitRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class QuestionnaireTest : EspressoTest() {

    private val diagnoseQuestionRobot = DiagnoseQuestionRobot()
    private val diagnoseCloseRobot = DiagnoseCloseRobot()
    private val diagnoseReviewRobot = DiagnoseReviewRobot()
    private val diagnoseSubmitRobot = DiagnoseSubmitRobot()
    private val statusRobot = StatusRobot()
    private val bottomDialogRobot = BottomDialogRobot()

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @Before
    fun setupFlowTestActivity() {
        testAppContext.app.startTestActivity<FlowTestStartActivity>()
    }

    @Test
    fun questionnaireFlowWithSymptoms() {
        testAppContext.setFullValidUser()
        startMainActivity()
        testAppContext.simulateDeviceInProximity()

        statusRobot.clickNotFeelingWellCard()

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

    @Test
    fun questionnaireFlowWithoutSymptoms() {
        testAppContext.setFullValidUser()
        startMainActivity()

        statusRobot.clickNotFeelingWellCard()

        diagnoseQuestionRobot.answerNoTo(R.id.temperature_question)
        diagnoseQuestionRobot.answerNoTo(R.id.cough_question)
        diagnoseQuestionRobot.answerNoTo(R.id.anosmia_question)
        diagnoseQuestionRobot.answerNoTo(R.id.sneeze_question)
        diagnoseQuestionRobot.answerNoTo(R.id.stomach_question)

        diagnoseCloseRobot.checkActivityIsDisplayed()
        diagnoseCloseRobot.close()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    @Test
    fun checkInQuestionnaireWithTemperature() {
        val date = DateTime.now(UTC).minusSeconds(1)
        val expiredSymptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        testAppContext.setFullValidUser(expiredSymptomaticState)
        startMainActivity()

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickFirstCtaButton()

        diagnoseQuestionRobot.checkProgress(R.string.progress_one_fifth)
        diagnoseQuestionRobot.answerYesTo(R.id.temperature_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_two_fifth)
        diagnoseQuestionRobot.answerNoTo(R.id.cough_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_three_fifth)
        diagnoseQuestionRobot.answerNoTo(R.id.anosmia_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_four_fifth)
        diagnoseQuestionRobot.answerNoTo(R.id.sneeze_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_five_fifth)
        diagnoseQuestionRobot.answerNoTo(R.id.stomach_question)

        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)
    }

    @Test
    fun checkInQuestionnaireWithoutTemperature() {
        val date = DateTime.now(UTC).minusSeconds(1)
        val expiredSymptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        testAppContext.setFullValidUser(expiredSymptomaticState)
        startMainActivity()

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickFirstCtaButton()

        diagnoseQuestionRobot.checkProgress(R.string.progress_one_fifth)
        diagnoseQuestionRobot.answerNoTo(R.id.temperature_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_two_fifth)
        diagnoseQuestionRobot.answerNoTo(R.id.cough_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_three_fifth)
        diagnoseQuestionRobot.answerNoTo(R.id.anosmia_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_four_fifth)
        diagnoseQuestionRobot.answerNoTo(R.id.sneeze_question)

        diagnoseQuestionRobot.checkProgress(R.string.progress_five_fifth)
        diagnoseQuestionRobot.answerNoTo(R.id.stomach_question)

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    @Test
    fun checkInOverlayTapNoSymptoms() {
        val date = DateTime.now(UTC).minusSeconds(1)
        val expiredSymptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        testAppContext.setFullValidUser(expiredSymptomaticState)
        startMainActivity()

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }
}
