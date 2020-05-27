package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class IsolateActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val isolateRobot = IsolateRobot()
    private val applyForTestRobot = ApplyForTestRobot()
    private val currentAdviceRobot = CurrentAdviceRobot()
    private val bottomDialogRobot = BottomDialogRobot()
    private val expiredSymptomaticState =
        SymptomaticState(
            DateTime.now(UTC).minusSeconds(1),
            DateTime.now(UTC).minusSeconds(1),
            nonEmptySetOf(TEMPERATURE)
        )

    private fun startActivity(state: UserState) {
        testAppContext.setFullValidUser(state)
        app.startTestActivity<IsolateActivity>()
    }

    fun testBottomDialogWhenStateIsExpiredSelectingUpdatingSymptoms() {
        startActivity(expiredSymptomaticState)

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickFirstCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    fun testBottomDialogWhenStateIsExpiredSelectingNoSymptoms() {
        startActivity(expiredSymptomaticState)

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    fun testClickOrderTestCardShowsApplyForTest() {
        val date = DateTime.now(UTC).plusDays(1)
        val symptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        startActivity(symptomaticState)

        isolateRobot.clickBookTestCard()
        applyForTestRobot.checkActivityIsDisplayed()
    }

    fun testClickOnCurrentAdviceShowsCurrentAdvice() {
        val date = DateTime.now(UTC).plusDays(1)
        val symptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        startActivity(symptomaticState)

        isolateRobot.clickCurrentAdviceCard()

        currentAdviceRobot.checkActivityIsDisplayed()

        currentAdviceRobot.checkCorrectStateIsDisplay(symptomaticState)
    }

    fun testStartsViewAndSetsUpCorrectStatusForSymptomaticState() {
        val since = DateTime.now(UTC).minusDays(1)
        val until = DateTime.now(UTC).plusDays(1)
        val state = SymptomaticState(since, until, nonEmptySetOf(TEMPERATURE))
        startActivity(state)

        isolateRobot.checkStatusTitle(R.string.status_symptomatic_title)
        isolateRobot.checkStatusDescription(state)
    }

    fun testStartsViewAndSetsUpCorrectStatusForPositiveTestState() {
        val since = DateTime.now(UTC).minusDays(1)
        val until = DateTime.now(UTC).plusDays(1)
        val state = PositiveState(since, until, nonEmptySetOf(TEMPERATURE))
        startActivity(state)

        isolateRobot.checkStatusTitle(R.string.status_positive_test_title)
        isolateRobot.checkStatusDescription(state)
    }

    fun testBookVirusTestIsNotDisplayedWhenInSymptomaticTestState() {
        val since = DateTime.now(UTC).minusDays(1)
        val until = DateTime.now(UTC).plusDays(1)
        val state = SymptomaticState(since, until, nonEmptySetOf(TEMPERATURE))
        startActivity(state)

        isolateRobot.checkBookVirusTestCardIsDisplayed()
    }

    fun testBookVirusTestIsNotDisplayedWhenInPositiveTestState() {
        val since = DateTime.now(UTC).minusDays(1)
        val until = DateTime.now(UTC).plusDays(1)
        val state = PositiveState(since, until, nonEmptySetOf(TEMPERATURE))
        startActivity(state)

        isolateRobot.checkBookVirusTestCardIsNotDisplayed()
    }

    fun testShowsPositiveTestResultDialogOnResume() {
        showsTestResultDialogOnResume(TestResult.POSITIVE)
    }

    fun testShowsNegativeTestResultDialogOnResume() {
        showsTestResultDialogOnResume(TestResult.NEGATIVE)
    }

    fun testShowsInvalidTestResultDialogOnResume() {
        showsTestResultDialogOnResume(TestResult.INVALID)
    }

    private fun showsTestResultDialogOnResume(testResult: TestResult) {
        testAppContext.addTestInfo(TestInfo(testResult, DateTime.now()))

        val since = DateTime.now(UTC).minusDays(1)
        val until = DateTime.now(UTC).plusDays(1)
        val state = SymptomaticState(since, until, nonEmptySetOf(TEMPERATURE))
        startActivity(state)

        bottomDialogRobot.checkTestResultDialogIsDisplayed(testResult)
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }
}
