package uk.nhs.nhsx.sonar.android.app.scenarios

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestData
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.ScenarioTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot

class ExposureTest : ScenarioTest() {

    private val statusRobot = StatusRobot()
    private val testData = TestData()

    @Test
    fun whileInNeutral() {
        startAppWith(testData.defaultState)

        statusRobot.checkActivityIsDisplayed(DefaultState::class)

        testAppContext.apply {
            simulateExposureNotificationReceived()
            clickOnNotification(
                R.string.contact_alert_notification_title,
                R.string.contact_alert_notification_text
            )
        }

        statusRobot.checkActivityIsDisplayed(ExposedState::class)
        statusRobot.checkStatusDescription(userState())
    }

    @Test
    fun whileInSymptomatic() {
        startAppWith(testData.symptomaticYesterday())

        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)

        testAppContext.apply {
            simulateExposureNotificationReceived()
        }

        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)
    }
}
