package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot

class AtRiskActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val atRiskRobot = AtRiskRobot()
    private val bottomDialogRobot = BottomDialogRobot()

    private fun startActivity() {
        testAppContext.setFullValidUser(UserState.exposed())

        app.startTestActivity<AtRiskActivity>()
    }

    fun testHideStatusUpdateNotificationWhenNotClicked() {
        val notificationTitle = R.string.contact_alert_notification_title

        testAppContext.simulateStatusUpdateReceived()
        testAppContext.isNotificationDisplayed(notificationTitle, isDisplayed = true)

        startActivity()

        testAppContext.isNotificationDisplayed(notificationTitle, isDisplayed = false)

        atRiskRobot.checkActivityIsDisplayed()
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

        startActivity()

        bottomDialogRobot.checkTestResultDialogIsDisplayed(testResult)
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }
}
