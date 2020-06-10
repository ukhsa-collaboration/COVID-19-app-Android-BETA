package uk.nhs.nhsx.sonar.android.app.onboarding

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.startTestActivity

class MainOnboardingActivityTest : EspressoTest() {

    private val mainOnBoardingRobot = MainOnboardingRobot()
    private val explanationRobot = ExplanationRobot()

    @Test
    fun testExplanation() {
        testAppContext.app.startTestActivity<MainOnboardingActivity>()

        mainOnBoardingRobot.checkActivityIsDisplayed()
        mainOnBoardingRobot.clickExplanationLink()

        explanationRobot.checkActivityIsDisplayed()
        explanationRobot.clickBackButton()

        mainOnBoardingRobot.checkActivityIsDisplayed()
    }
}
