package uk.nhs.nhsx.sonar.android.app.scenarios

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestData
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.ApplyForTestRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot

class BookTestForCoronavirusTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val applyForTestRobot = ApplyForTestRobot()
    private val testData = TestData()

    @Test
    fun clickOrderTestCardShowsApplyForTest() {
        startStatusActivityWith(testData.symptomaticState)

        statusRobot.clickBookTestCard()
        applyForTestRobot.checkActivityIsDisplayed()
    }
}
