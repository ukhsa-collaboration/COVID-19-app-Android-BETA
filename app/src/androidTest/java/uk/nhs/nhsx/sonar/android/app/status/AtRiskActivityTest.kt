package uk.nhs.nhsx.sonar.android.app.status

import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext

class AtRiskActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val atRiskRobot = AtRiskRobot()

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
}
