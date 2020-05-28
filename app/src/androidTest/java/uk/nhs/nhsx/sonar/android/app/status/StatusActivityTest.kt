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

class StatusActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val statusRobot = StatusRobot()
    private val applyForTestRobot = ApplyForTestRobot()
    private val currentAdviceRobot = CurrentAdviceRobot()
    private val bottomDialogRobot = BottomDialogRobot()
    private val expiredSymptomaticState = SymptomaticState(
        DateTime.now(UTC).minusSeconds(1),
        DateTime.now(UTC).minusSeconds(1),
        nonEmptySetOf(TEMPERATURE)
    )

    private val symptomaticState = SymptomaticState(
        DateTime.now(UTC).minusDays(1),
        DateTime.now(UTC).plusDays(1),
        nonEmptySetOf(TEMPERATURE)
    )

    private val exposedState = UserState.exposed()

    private val positiveState = PositiveState(
        DateTime.now(UTC).minusDays(1),
        DateTime.now(UTC).plusDays(1),
        nonEmptySetOf(TEMPERATURE)
    )

    private fun startActivity(state: UserState) {
        testAppContext.setFullValidUser(state)
        app.startTestActivity<StatusActivity>()
    }

    private fun showsTestResultDialogOnResume(testResult: TestResult, state: UserState) {
        testAppContext.addTestInfo(TestInfo(testResult, DateTime.now()))

        startActivity(state)

        bottomDialogRobot.checkTestResultDialogIsDisplayed(testResult)
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    fun testFeelUnwellCardIsDisplayedWhenInDefaultState() {
        startActivity(DefaultState)
        statusRobot.checkFeelUnwellIsDisplayed()
    }

    fun testFeelUnwellCardIsDisplayedWhenInExposedState() {
        startActivity(exposedState)
        statusRobot.checkFeelUnwellIsDisplayed()
    }

    fun testFeelUnwellCardIsNotDisplayedWhenInSymptomaticState() {
        startActivity(symptomaticState)
        statusRobot.checkFeelUnwellIsNotDisplayed()
    }

    fun testFeelUnwellCardIsNotDisplayedWhenInPositiveState() {
        startActivity(positiveState)
        statusRobot.checkFeelUnwellIsNotDisplayed()
    }

    fun testRegistrationRetry() {
        testAppContext.setFinishedOnboarding()
        testAppContext.simulateBackendResponse(error = true)

        app.startTestActivity<StatusActivity>()
        statusRobot.checkFinalisingSetup()

        testAppContext.simulateBackendResponse(error = false)
        testAppContext.verifyRegistrationRetry()

        statusRobot.checkFeelUnwellIsDisplayed()
    }

    fun testRegistrationPushNotificationNotReceived() {
        testAppContext.setFinishedOnboarding()
        testAppContext.simulateBackendDelay(400)

        app.startTestActivity<StatusActivity>()
        statusRobot.checkFinalisingSetup()

        testAppContext.verifyReceivedRegistrationRequest()
        testAppContext.verifyRegistrationFlow()

        statusRobot.checkFeelUnwellIsDisplayed()
    }

    fun testShowsRecoveryDialogOnResume() {
        testAppContext.setFullValidUser(DefaultState)
        testAppContext.addRecoveryMessage()

        app.startTestActivity<StatusActivity>()

        bottomDialogRobot.checkRecoveryDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
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

        statusRobot.clickBookTestCard()
        applyForTestRobot.checkActivityIsDisplayed()
    }

    fun testClickOnCurrentAdviceShowsCurrentAdvice() {
        val date = DateTime.now(UTC).plusDays(1)
        val symptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        startActivity(symptomaticState)

        statusRobot.clickCurrentAdviceCard()

        currentAdviceRobot.checkActivityIsDisplayed()

        currentAdviceRobot.checkCorrectStateIsDisplay(symptomaticState)
    }

    fun testStartsViewAndSetsUpCorrectStatusForSymptomaticState() {
        val since = DateTime.now(UTC).minusDays(1)
        val until = DateTime.now(UTC).plusDays(1)
        val state = SymptomaticState(since, until, nonEmptySetOf(TEMPERATURE))
        startActivity(state)

        statusRobot.checkStatusTitle(R.string.status_symptomatic_title)
        statusRobot.checkStatusDescription(state)
    }

    fun testStartsViewAndSetsUpCorrectStatusForPositiveTestState() {
        val since = DateTime.now(UTC).minusDays(1)
        val until = DateTime.now(UTC).plusDays(1)
        val state = PositiveState(since, until, nonEmptySetOf(TEMPERATURE))
        startActivity(state)

        statusRobot.checkStatusTitle(R.string.status_positive_test_title)
        statusRobot.checkStatusDescription(state)
    }

    fun testBookVirusTestIsNotDisplayedWhenInSymptomaticTestState() {
        startActivity(symptomaticState)

        statusRobot.checkBookVirusTestCardIsDisplayed()
    }

    fun testBookVirusTestIsNotDisplayedWhenInPositiveTestState() {
        startActivity(positiveState)

        statusRobot.checkBookVirusTestCardIsNotDisplayed()
    }

    fun testBookVirusTestIsNotDisplayedWhenInExposedState() {
        startActivity(exposedState)

        statusRobot.checkBookVirusTestCardIsNotDisplayed()
    }

    fun testShowsPositiveTestResultDialogOnResumeForDefaultState() {
        showsTestResultDialogOnResume(TestResult.POSITIVE, DefaultState)
    }

    fun testShowsNegativeTestResultDialogOnResumeForDefaultState() {
        showsTestResultDialogOnResume(TestResult.NEGATIVE, DefaultState)
    }

    fun testShowsInvalidTestResultDialogOnResumeForDefaultState() {
        showsTestResultDialogOnResume(TestResult.INVALID, DefaultState)
    }

    fun testShowsPositiveTestResultDialogOnResumeForSymptomaticState() {
        showsTestResultDialogOnResume(TestResult.POSITIVE, symptomaticState)
    }

    fun testShowsNegativeTestResultDialogOnResumeForSymptomaticState() {
        showsTestResultDialogOnResume(TestResult.NEGATIVE, symptomaticState)
    }

    fun testShowsInvalidTestResultDialogOnResumeForSymptomaticState() {
        showsTestResultDialogOnResume(TestResult.INVALID, symptomaticState)
    }

    fun testShowsPositiveTestResultDialogOnResumeForExposedState() {
        showsTestResultDialogOnResume(TestResult.POSITIVE, exposedState)
    }

    fun testShowsNegativeTestResultDialogOnResumeForExposedState() {
        showsTestResultDialogOnResume(TestResult.NEGATIVE, exposedState)
    }

    fun testShowsInvalidTestResultDialogOnResumeForExposedState() {
        showsTestResultDialogOnResume(TestResult.INVALID, exposedState)
    }

    fun testHideStatusUpdateNotificationWhenNotClicked() {
        val notificationTitle = R.string.contact_alert_notification_title

        testAppContext.simulateStatusUpdateReceived()
        testAppContext.isNotificationDisplayed(notificationTitle, isDisplayed = true)

        startActivity(UserState.exposed())

        testAppContext.isNotificationDisplayed(notificationTitle, isDisplayed = false)

        statusRobot.checkActivityIsDisplayed(ExposedState::class)
    }

    fun testShowsEnableNotificationOnResume() {
        testAppContext.setFullValidUser(DefaultState)
        testAppContext.revokeNotificationsPermission()

        app.startTestActivity<StatusActivity>()

        statusRobot.checkEnableNotificationsIsDisplayed()
    }

    fun testDoesNotEnableAllowNotificationOnResume() {
        testAppContext.setFullValidUser(DefaultState)
        testAppContext.grantNotificationsPermission()

        app.startTestActivity<StatusActivity>()

        statusRobot.checkEnableNotificationsIsNotDisplayed()
    }

    fun testGrantNotificationPermission() {
        testAppContext.setFullValidUser(DefaultState)
        testAppContext.revokeNotificationsPermission()

        app.startTestActivity<StatusActivity>()

        statusRobot.clickEnableNotifications()
        testAppContext.waitUntilCannotFindText(R.string.enable_notifications_title)

        testAppContext.grantNotificationsPermission()
        testAppContext.device.pressBack()

        statusRobot.checkEnableNotificationsIsNotDisplayed()
    }
}
