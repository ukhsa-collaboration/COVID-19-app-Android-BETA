package uk.nhs.nhsx.sonar.android.app.status

import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot

class OkActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val okRobot = OkRobot(app)
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
        testAppContext.setFullValidUser(RecoveryState())

        app.startTestActivity<OkActivity>()

        bottomDialogRobot.checkRecoveryDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }
}
