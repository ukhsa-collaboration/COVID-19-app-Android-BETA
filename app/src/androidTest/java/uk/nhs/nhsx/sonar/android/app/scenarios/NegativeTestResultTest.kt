package uk.nhs.nhsx.sonar.android.app.scenarios

import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.ExposedSymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.ScenarioTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.ReferenceCodeRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import kotlin.reflect.KClass

class NegativeTestResultTest : ScenarioTest() {

    private val statusRobot = StatusRobot()
    private val referenceCodeRobot = ReferenceCodeRobot()
    private val bottomDialogRobot = BottomDialogRobot()

    @Test
    fun tookTestWhileInNeutral() {
        startAppWith(testData.defaultState)

        receiveNegativeTestResult(testData.today)

        dismissNegativeTestResult()
        verifyStatusIs(DefaultState::class)
    }

    @Test
    fun tookTestAfterBecomingSymptomatic() {
        val state = testData.symptomaticYesterday()

        startAppWith(state)

        receiveNegativeTestResult(testData.today)

        dismissNegativeTestResult()
        verifyStatusIs(DefaultState::class)
    }

    @Test
    fun tookTestBeforeBecomingSymptomatic() {
        val state = testData.symptomaticTomorrow()

        startAppWith(state)

        receiveNegativeTestResult(testData.today)

        dismissNegativeTestResult()
        verifyStatusIs(SymptomaticState::class)
    }

    @Test
    fun tookTestWhileInExposed() {
        val state = testData.exposedYesterday()

        startAppWith(state)

        receiveNegativeTestResult(testData.today)

        dismissNegativeTestResult()
        verifyStatusIs(ExposedState::class)
    }

    @Test
    fun tookTestWhileInPositive() {
        val state = testData.positiveToday()

        startAppWith(state)

        receiveNegativeTestResult(testData.today)

        dismissNegativeTestResult()
        verifyStatusIs(PositiveState::class)
    }

    @Test
    fun tookTestWhileInExposedSymptomaticWithinExposureWindow() {
        val state = testData.exposedSymptomaticYesterday()

        startAppWith(state)

        receiveNegativeTestResult(testData.today)

        dismissNegativeTestResult()
        verifyStatusIs(ExposedState::class)
    }

    @Test
    fun tookTestWhileInExposedSymptomaticOutsideExposureWindow() {
        val exposedState = UserState.exposed(
            exposureDate = testData.yesterday.minusDays(14)
        )
        val state = UserState.exposedSymptomatic(
            symptomsDate = testData.yesterday,
            state = exposedState,
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )

        startAppWith(state)

        receiveNegativeTestResult(testData.today)

        dismissNegativeTestResult()
        verifyStatusIs(DefaultState::class)
    }

    @Test
    fun tookTestBeforeBecomingExposedSymptomatic() {
        val state = testData.exposedSymptomaticToday()

        startAppWith(state)

        receiveNegativeTestResult(testData.yesterday)

        dismissNegativeTestResult()
        verifyStatusIs(ExposedSymptomaticState::class)
    }

    @Test
    fun clicksTestResultMeaning() {
        startAppWith(testData.defaultState)

        receiveNegativeTestResult(testData.today)

        bottomDialogRobot.clickFirstCtaButton()

        referenceCodeRobot.checkActivityIsDisplayed()
    }

    private fun receiveNegativeTestResult(testDate: LocalDate) {
        testAppContext.apply {
            simulateTestResultNotificationReceived(
                TestInfo(
                    result = TestResult.NEGATIVE,
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

    private fun dismissNegativeTestResult() {
        bottomDialogRobot.checkTestResultDialogIsDisplayed(TestResult.NEGATIVE)
        bottomDialogRobot.clickSecondCtaButton()
    }
}
