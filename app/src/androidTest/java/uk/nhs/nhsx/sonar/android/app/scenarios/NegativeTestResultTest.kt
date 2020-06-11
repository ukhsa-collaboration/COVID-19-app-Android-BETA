package uk.nhs.nhsx.sonar.android.app.scenarios

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.FlowTestStartActivity
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
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import kotlin.reflect.KClass

class NegativeTestResultTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val bottomDialogRobot = BottomDialogRobot()
    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)
    private val tomorrow = today.plusDays(1)

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @Before
    fun setupFlowTestActivity() {
        testAppContext.app.startTestActivity<FlowTestStartActivity>()
    }

    @Test
    fun tookTestWhileInNeutral() {
        startAppWith(UserState.default())

        receiveNegativeTestResult(today)

        dismissBottomDialog()
        verifyStatusIs(DefaultState::class)
    }

    @Test
    fun tookTestAfterBecomingSymptomatic() {
        val state = UserState.symptomatic(
            symptomsDate = yesterday,
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )

        startAppWith(state)

        receiveNegativeTestResult(today)

        dismissBottomDialog()
        verifyStatusIs(DefaultState::class)
    }

    @Test
    fun tookTestBeforeBecomingSymptomatic() {
        val state = UserState.symptomatic(
            symptomsDate = tomorrow,
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )

        startAppWith(state)

        receiveNegativeTestResult(today)

        dismissBottomDialog()
        verifyStatusIs(SymptomaticState::class)
    }

    @Test
    fun tookTestWhileInExposed() {
        val state = UserState.exposed(
            exposureDate = yesterday
        )

        startAppWith(state)

        receiveNegativeTestResult(today)

        dismissBottomDialog()
        verifyStatusIs(ExposedState::class)
    }

    @Test
    fun tookTestWhileInPositive() {
        val state = UserState.positive(
            testDate = today.toDateTime(LocalTime.now())
        )

        startAppWith(state)

        receiveNegativeTestResult(today)

        dismissBottomDialog()
        verifyStatusIs(PositiveState::class)
    }

    @Test
    fun tookTestWhileInExposedSymptomaticWithinExposureWindow() {
        val exposedState = UserState.exposed(
            exposureDate = yesterday
        )
        val state = UserState.exposedSymptomatic(
            symptomsDate = yesterday,
            state = exposedState,
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )

        startAppWith(state)

        receiveNegativeTestResult(today)

        dismissBottomDialog()
        verifyStatusIs(ExposedState::class)
    }

    @Test
    fun tookTestWhileInExposedSymptomaticOutsideExposureWindow() {
        val exposedState = UserState.exposed(
            exposureDate = yesterday.minusDays(14)
        )
        val state = UserState.exposedSymptomatic(
            symptomsDate = yesterday,
            state = exposedState,
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )

        startAppWith(state)

        receiveNegativeTestResult(today)

        dismissBottomDialog()
        verifyStatusIs(DefaultState::class)
    }

    @Test
    fun tookTestBeforeBecomingExposedSymptomatic() {
        val exposedState = UserState.exposed(
            exposureDate = today
        )
        val state = UserState.exposedSymptomatic(
            symptomsDate = today,
            state = exposedState,
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )

        startAppWith(state)

        receiveNegativeTestResult(yesterday)

        dismissBottomDialog()
        verifyStatusIs(ExposedSymptomaticState::class)
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
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

    private fun dismissBottomDialog() {
        bottomDialogRobot.checkTestResultDialogIsDisplayed(TestResult.NEGATIVE)
        bottomDialogRobot.clickSecondCtaButton()
    }

    private fun startAppWith(state: UserState) {
        testAppContext.setFullValidUser(state)
        startMainActivity()
    }
}
