/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.scenarios

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.ScenarioTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseCloseRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseQuestionRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseReviewRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseSubmitRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot

class QuestionnaireTest : ScenarioTest() {

    private val diagnoseQuestionRobot = DiagnoseQuestionRobot()
    private val diagnoseCloseRobot = DiagnoseCloseRobot()
    private val diagnoseReviewRobot = DiagnoseReviewRobot()
    private val diagnoseSubmitRobot = DiagnoseSubmitRobot()
    private val statusRobot = StatusRobot()
    private val bottomDialogRobot = BottomDialogRobot()

    @Test
    fun questionnaireFlowWithSymptoms() {
        startAppWith(testData.defaultState)

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
        startAppWith(testData.defaultState)

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
        startAppWith(testData.expiredSymptomaticState)

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
        startAppWith(testData.expiredSymptomaticState)

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
        startAppWith(testData.expiredSymptomaticState)

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }
}
