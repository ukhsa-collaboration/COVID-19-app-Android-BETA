package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot

class OkActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val okRobot = OkRobot()
    private val bottomDialogRobot = BottomDialogRobot()

    fun testRegistrationRetry() {
        testAppContext.setFinishedOnboarding()
        testAppContext.simulateBackendResponse(error = true)

        app.startTestActivity<OkActivity>()
        okRobot.checkFinalisingSetup()

        testAppContext.simulateBackendResponse(error = false)
        testAppContext.verifyRegistrationRetry()

        okRobot.checkEverythingIsWorking()
    }

    fun testRegistrationPushNotificationNotReceived() {
        testAppContext.setFinishedOnboarding()
        testAppContext.simulateBackendDelay(400)

        app.startTestActivity<OkActivity>()
        okRobot.checkFinalisingSetup()

        testAppContext.verifyReceivedRegistrationRequest()
        testAppContext.verifyRegistrationFlow()

        okRobot.checkEverythingIsWorking()
    }

    fun testShowsRecoveryDialogOnResume() {
        testAppContext.setFullValidUser(DefaultState)
        testAppContext.addRecoveryMessage()

        app.startTestActivity<OkActivity>()

        bottomDialogRobot.checkRecoveryDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
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

        testAppContext.setFullValidUser(DefaultState)
        app.startTestActivity<OkActivity>()

        bottomDialogRobot.checkTestResultDialogIsDisplayed(testResult)
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }
}
