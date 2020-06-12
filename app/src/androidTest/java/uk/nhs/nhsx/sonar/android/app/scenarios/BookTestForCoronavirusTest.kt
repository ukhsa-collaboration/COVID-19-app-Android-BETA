package uk.nhs.nhsx.sonar.android.app.scenarios

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestData
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.ScenarioTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.ApplyForTestRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot

class BookTestForCoronavirusTest : ScenarioTest() {

    private val statusRobot = StatusRobot()
    private val applyForTestRobot = ApplyForTestRobot()
    private val testData = TestData()

    @Test
    fun clickOrderTestCardShowsApplyForTest() {
        startAppWith(testData.symptomaticState)

        statusRobot.clickBookTestCard()

        applyForTestRobot.checkActivityIsDisplayed()
    }
}
