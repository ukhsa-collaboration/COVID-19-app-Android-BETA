package uk.nhs.nhsx.sonar.android.app.scenarios

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.ScenarioTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.CurrentAdviceRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot

class CurrentAdviceTest : ScenarioTest() {

    private val statusRobot = StatusRobot()
    private val currentAdviceRobot = CurrentAdviceRobot()

    @Test
    fun navigatesIntoCurrentAdviceBySelectingCard() {
        startAppWith(testData.symptomaticState)

        statusRobot.clickCurrentAdviceCard()

        currentAdviceRobot.checkActivityIsDisplayed()
    }
}
