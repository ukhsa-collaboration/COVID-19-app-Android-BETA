/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.scenarios

import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.ScenarioTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot
import kotlin.reflect.KClass

class PositiveTestResultTest : ScenarioTest() {

    private val statusRobot = StatusRobot()
    private val bottomDialogRobot = BottomDialogRobot()

    @Test
    fun tookTestWhileInNeutral() {
        startAppWith(testData.defaultState)

        receivePositiveTestResult(testData.today)

        dismissPositiveTestResult()
        verifyStatusIs(PositiveState::class)
    }

    @Test
    fun tookTestWhileInSymptomatic() {
        val state = testData.symptomaticYesterday()

        startAppWith(state)

        receivePositiveTestResult(testData.today)

        dismissPositiveTestResult()
        verifyStatusIs(PositiveState::class)
    }

    @Test
    fun tookTestWhileInExposed() {
        val state = testData.exposedYesterday()

        startAppWith(state)

        receivePositiveTestResult(testData.today)

        dismissPositiveTestResult()
        verifyStatusIs(PositiveState::class)
    }

    @Test
    fun tookTestWhileInPositive() {
        val state = testData.positiveToday()

        startAppWith(state)

        receivePositiveTestResult(testData.today)

        dismissPositiveTestResult()
        verifyStatusIs(PositiveState::class)
    }

    @Test
    fun tookTestWhileInExposedSymptomaticWithinExposureWindow() {
        val state = testData.exposedSymptomaticYesterday()

        startAppWith(state)

        receivePositiveTestResult(testData.today)

        dismissPositiveTestResult()
        verifyStatusIs(PositiveState::class)
    }

    private fun receivePositiveTestResult(testDate: LocalDate) {
        testAppContext.apply {
            simulateTestResultNotificationReceived(
                TestInfo(
                    result = TestResult.POSITIVE,
                    date = testDate.toDateTime(LocalTime.now())
                )
            )
            clickOnNotification(
                R.string.test_result_notification_title,
                R.string.test_result_notification_text
            )
        }
    }

    private fun <T : UserState> verifyStatusIs(userState: KClass<T>) {
        statusRobot.checkActivityIsDisplayed(userState)
    }

    private fun dismissPositiveTestResult() {
        bottomDialogRobot.checkTestResultDialogIsDisplayed(TestResult.POSITIVE)
        bottomDialogRobot.clickSecondCtaButton()
    }
}
