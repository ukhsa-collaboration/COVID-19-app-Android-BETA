package uk.nhs.nhsx.sonar.android.app.onboarding

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.ExplanationRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.MainOnboardingRobot

class MainOnboardingActivityTest : EspressoTest() {

    private val mainOnBoardingRobot = MainOnboardingRobot()
    private val explanationRobot = ExplanationRobot()

    @Test
    fun explanation() {
        startTestActivity<MainOnboardingActivity>()

        mainOnBoardingRobot.checkActivityIsDisplayed()
        mainOnBoardingRobot.clickExplanationLink()

        explanationRobot.checkActivityIsDisplayed()
        explanationRobot.clickBackButton()

        mainOnBoardingRobot.checkActivityIsDisplayed()
    }
}
