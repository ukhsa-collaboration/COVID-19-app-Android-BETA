package uk.nhs.nhsx.sonar.android.app.scenarios

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestData
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.CurrentAdviceRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot

class CurrentAdviceTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val currentAdviceRobot = CurrentAdviceRobot()
    private val testData = TestData()

    @Test
    fun clickOnCurrentAdviceShowsCurrentAdvice() {
        startStatusActivityWith(testData.symptomaticState)

        statusRobot.clickCurrentAdviceCard()

        currentAdviceRobot.checkActivityIsDisplayed()

        currentAdviceRobot.checkCorrectStateIsDisplay(testData.symptomaticState)
    }
}
