package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.ApplyForTestRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.CurrentAdviceRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class StatusActivityTest : EspressoTest() {

    // TODO: testBookTestCardIsDisplayedButNotEnabledWhenRegistrationIsNotFinished

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

    private val exposedState = UserState.exposed(LocalDate.now())

    private val exposedSymptomaticState = UserState.exposedSymptomatic(
        symptomsDate =  LocalDate.now().minusDays(1),
        state = exposedState,
        symptoms = nonEmptySetOf(TEMPERATURE)
    )

    private val positiveState = PositiveState(
        DateTime.now(UTC).minusDays(1),
        DateTime.now(UTC).plusDays(1),
        nonEmptySetOf(TEMPERATURE)
    )

    private val expiredPositiveState = PositiveState(
        DateTime.now(UTC).minusDays(15),
        DateTime.now(UTC).minusDays(1),
        nonEmptySetOf(TEMPERATURE)
    )

    private fun startActivity(state: UserState) {
        testAppContext.setFullValidUser(state)
        testAppContext.app.startTestActivity<StatusActivity>()
    }

    private fun showsTestResultDialogOnResume(testResult: TestResult, state: UserState) {
        testAppContext.addTestInfo(TestInfo(testResult, DateTime.now()))

        startActivity(state)

        bottomDialogRobot.checkTestResultDialogIsDisplayed(testResult)
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    @Test
    fun showsDefaultState() {
        startActivity(DefaultState)

        statusRobot.checkAppIsWorking()
        statusRobot.checkActivityIsDisplayed(DefaultState::class)
        statusRobot.checkStatusDescriptionIsNotDisplayed()

        statusRobot.checkCurrentAdviceCardIsDisplayed()
        statusRobot.checkFeelUnwellIsDisplayed()
        statusRobot.checkBookVirusTestCardIsNotDisplayed()

        statusRobot.swipeToBottom()

        statusRobot.checkInformationAboutTestingIsDisplayed()
        statusRobot.checkWorkplaceGuidanceIsDisplayed()
        statusRobot.checkNhsServicesLinkIsDisplayed()
    }

    @Test
    fun showsExposedState() {
        startActivity(exposedState)

        statusRobot.checkAppIsWorking()
        statusRobot.checkActivityIsDisplayed(ExposedState::class)
        statusRobot.checkStatusDescription(exposedState)

        statusRobot.checkCurrentAdviceCardIsDisplayed()
        statusRobot.checkFeelUnwellIsDisplayed()
        statusRobot.checkBookVirusTestCardIsNotDisplayed()

        statusRobot.swipeToBottom()

        statusRobot.checkInformationAboutTestingIsDisplayed()
        statusRobot.checkWorkplaceGuidanceIsDisplayed()
        statusRobot.checkNhsServicesLinkIsDisplayed()
    }

    @Test
    fun showsSymptomaticState() {
        startActivity(symptomaticState)

        statusRobot.checkAppIsWorking()
        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)
        statusRobot.checkStatusDescription(symptomaticState)

        statusRobot.checkCurrentAdviceCardIsDisplayed()
        statusRobot.checkFeelUnwellIsNotDisplayed()
        statusRobot.checkBookVirusTestCardIsDisplayed()

        statusRobot.swipeToBottom()

        statusRobot.checkInformationAboutTestingIsDisplayed()
        statusRobot.checkWorkplaceGuidanceIsDisplayed()
        statusRobot.checkNhsServicesLinkIsDisplayed()
    }

    @Test
    fun showsExposedSymptomaticState() {
        startActivity(exposedSymptomaticState)

        statusRobot.checkAppIsWorking()
        statusRobot.checkActivityIsDisplayed(ExposedSymptomaticState::class)
        statusRobot.checkStatusDescription(exposedSymptomaticState)

        statusRobot.checkCurrentAdviceCardIsDisplayed()
        statusRobot.checkFeelUnwellIsNotDisplayed()
        statusRobot.checkBookVirusTestCardIsDisplayed()

        statusRobot.swipeToBottom()

        statusRobot.checkInformationAboutTestingIsDisplayed()
        statusRobot.checkWorkplaceGuidanceIsDisplayed()
        statusRobot.checkNhsServicesLinkIsDisplayed()
    }

    @Test
    fun showsPositiveState() {
        startActivity(positiveState)

        statusRobot.checkAppIsWorking()
        statusRobot.checkActivityIsDisplayed(PositiveState::class)
        statusRobot.checkStatusDescription(positiveState)

        statusRobot.checkCurrentAdviceCardIsDisplayed()
        statusRobot.checkFeelUnwellIsNotDisplayed()
        statusRobot.checkBookVirusTestCardIsNotDisplayed()

        statusRobot.swipeToBottom()

        statusRobot.checkInformationAboutTestingIsDisplayed()
        statusRobot.checkWorkplaceGuidanceIsDisplayed()
        statusRobot.checkNhsServicesLinkIsDisplayed()
    }

    @Test
    fun registrationRetry() {
        testAppContext.setFinishedOnboarding()
        testAppContext.simulateBackendResponse(error = true)

        testAppContext.app.startTestActivity<StatusActivity>()
        statusRobot.checkFinalisingSetup()

        testAppContext.simulateBackendResponse(error = false)
        testAppContext.verifyRegistrationRetry()

        statusRobot.checkFeelUnwellIsDisplayed()
    }

    @Test
    fun registrationPushNotificationNotReceived() {
        testAppContext.setFinishedOnboarding()
        testAppContext.simulateBackendDelay(400)

        testAppContext.app.startTestActivity<StatusActivity>()
        statusRobot.checkFinalisingSetup()

        testAppContext.verifyReceivedRegistrationRequest()
        testAppContext.verifyRegistrationFlow()

        statusRobot.checkFeelUnwellIsDisplayed()
    }

    @Test
    fun showsRecoveryDialogOnResume() {
        testAppContext.setFullValidUser(DefaultState)
        testAppContext.addRecoveryMessage()

        testAppContext.app.startTestActivity<StatusActivity>()

        bottomDialogRobot.checkRecoveryDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    @Test
    fun bottomDialogWhenStateIsExpiredSelectingUpdatingSymptoms() {
        startActivity(expiredSymptomaticState)

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickFirstCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    @Test
    fun bottomDialogWhenStateIsExpiredSelectingNoSymptoms() {
        startActivity(expiredSymptomaticState)

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    @Test
    fun showsUpdateSymptomsDialogWhenPositiveStateExpired() {
        startActivity(expiredPositiveState)

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    @Test
    fun clickOrderTestCardShowsApplyForTest() {
        val date = DateTime.now(UTC).plusDays(1)
        val symptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        startActivity(symptomaticState)

        statusRobot.clickBookTestCard()
        applyForTestRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickOnCurrentAdviceShowsCurrentAdvice() {
        val date = DateTime.now(UTC).plusDays(1)
        val symptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        startActivity(symptomaticState)

        statusRobot.clickCurrentAdviceCard()

        currentAdviceRobot.checkActivityIsDisplayed()

        currentAdviceRobot.checkCorrectStateIsDisplay(symptomaticState)
    }

    @Test
    fun showsCorrectStatusForExposedState() {
        val state = UserState.exposed(LocalDate.now())
        startActivity(state)

        statusRobot.checkStatusTitle(R.string.status_exposed_title)
        statusRobot.checkStatusDescription(state)
    }

    @Test
    fun showsCorrectStatusForSymptomaticState() {
        val state = UserState.symptomatic(
            symptomsDate = LocalDate.now().minusDays(1),
            symptoms = nonEmptySetOf(TEMPERATURE)
        )
        startActivity(state)

        statusRobot.checkStatusTitle(R.string.status_symptomatic_title)
        statusRobot.checkStatusDescription(state)
    }

    @Test
    fun showsCorrectStatusForExposedSymptomaticState() {
        val since = LocalDate.now().minusDays(1)
        val state = UserState.exposedSymptomatic(
            symptomsDate = since,
            state = UserState.exposed(since),
            symptoms = nonEmptySetOf(TEMPERATURE)
        )

        startActivity(state)

        statusRobot.checkStatusTitle(R.string.status_symptomatic_title)
        statusRobot.checkStatusDescription(state)
    }

    @Test
    fun showsCorrectStatusForPositiveTestState() {
        val state = UserState.positive(
            testDate = DateTime.now(UTC).minusDays(1)
        )
        startActivity(state)

        statusRobot.checkStatusTitle(R.string.status_positive_test_title)
        statusRobot.checkStatusDescription(state)
    }

    @Test
    fun bookVirusTestIsNotDisplayedWhenInSymptomaticTestState() {
        startActivity(symptomaticState)

        statusRobot.checkBookVirusTestCardIsDisplayed()
    }

    @Test
    fun bookVirusTestIsNotDisplayedWhenInPositiveTestState() {
        startActivity(positiveState)

        statusRobot.checkBookVirusTestCardIsNotDisplayed()
    }

    @Test
    fun bookVirusTestIsNotDisplayedWhenInExposedState() {
        startActivity(exposedState)

        statusRobot.checkBookVirusTestCardIsNotDisplayed()
    }

    @Test
    fun showsPositiveTestResultDialogOnResumeForDefaultState() {
        showsTestResultDialogOnResume(TestResult.POSITIVE, DefaultState)
    }

    @Test
    fun showsNegativeTestResultDialogOnResumeForDefaultState() {
        showsTestResultDialogOnResume(TestResult.NEGATIVE, DefaultState)
    }

    @Test
    fun showsInvalidTestResultDialogOnResumeForDefaultState() {
        showsTestResultDialogOnResume(TestResult.INVALID, DefaultState)
    }

    @Test
    fun showsPositiveTestResultDialogOnResumeForSymptomaticState() {
        showsTestResultDialogOnResume(TestResult.POSITIVE, symptomaticState)
    }

    @Test
    fun showsNegativeTestResultDialogOnResumeForSymptomaticState() {
        showsTestResultDialogOnResume(TestResult.NEGATIVE, symptomaticState)
    }

    @Test
    fun showsInvalidTestResultDialogOnResumeForSymptomaticState() {
        showsTestResultDialogOnResume(TestResult.INVALID, symptomaticState)
    }

    @Test
    fun showsPositiveTestResultDialogOnResumeForExposedState() {
        showsTestResultDialogOnResume(TestResult.POSITIVE, exposedState)
    }

    @Test
    fun showsNegativeTestResultDialogOnResumeForExposedState() {
        showsTestResultDialogOnResume(TestResult.NEGATIVE, exposedState)
    }

    @Test
    fun showsInvalidTestResultDialogOnResumeForExposedState() {
        showsTestResultDialogOnResume(TestResult.INVALID, exposedState)
    }

    @Test
    fun hideStatusUpdateNotificationWhenNotClicked() {
        val notificationTitle = R.string.contact_alert_notification_title

        testAppContext.simulateExposureNotificationReceived()
        testAppContext.isNotificationDisplayed(notificationTitle, isDisplayed = true)

        startActivity(UserState.exposed(LocalDate.now()))

        testAppContext.isNotificationDisplayed(notificationTitle, isDisplayed = false)

        statusRobot.checkActivityIsDisplayed(ExposedState::class)
    }

    @Test
    fun showsEnableNotificationOnResume() {
        testAppContext.setFullValidUser(DefaultState)
        testAppContext.revokeNotificationsPermission()

        testAppContext.app.startTestActivity<StatusActivity>()

        statusRobot.checkEnableNotificationsIsDisplayed()
    }

    @Test
    fun doesNotEnableAllowNotificationOnResume() {
        testAppContext.setFullValidUser(DefaultState)
        testAppContext.grantNotificationsPermission()

        testAppContext.app.startTestActivity<StatusActivity>()

        statusRobot.checkEnableNotificationsIsNotDisplayed()
    }

    @Test
    fun grantNotificationPermission() {
        testAppContext.setFullValidUser(DefaultState)
        testAppContext.revokeNotificationsPermission()

        testAppContext.app.startTestActivity<StatusActivity>()

        statusRobot.clickEnableNotifications()
        testAppContext.waitUntilCannotFindText(R.string.enable_notifications_title)

        testAppContext.grantNotificationsPermission()
        testAppContext.device.pressBack()

        statusRobot.checkEnableNotificationsIsNotDisplayed()
    }
}
