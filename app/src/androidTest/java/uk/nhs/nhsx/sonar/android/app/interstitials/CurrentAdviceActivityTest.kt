package uk.nhs.nhsx.sonar.android.app.interstitials

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestData
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.CurrentAdviceRobot

class CurrentAdviceActivityTest : EspressoTest() {

    private val currentAdviceRobot = CurrentAdviceRobot()
    private val testData = TestData()

    @Test
    fun showsAdviceSpecificForStateWithoutUntilDate() {
        startActivity(CurrentAdviceActivity::class, testData.defaultState)

        currentAdviceRobot.checkIconIsShowing()
        currentAdviceRobot.checkActivityIsDisplayed()
        currentAdviceRobot.checkCorrectStateIsDisplay(testData.defaultState)
        currentAdviceRobot.checkAdviceUrlIsDisplayed()
    }

    @Test
    fun showsAdviceSpecificForStateWithUntilDate() {
        startActivity(CurrentAdviceActivity::class, testData.symptomaticState)

        currentAdviceRobot.checkIconIsShowing()
        currentAdviceRobot.checkActivityIsDisplayed()
        currentAdviceRobot.checkCorrectStateIsDisplay(testData.symptomaticState)
        currentAdviceRobot.checkAdviceUrlIsDisplayed()
    }
}
