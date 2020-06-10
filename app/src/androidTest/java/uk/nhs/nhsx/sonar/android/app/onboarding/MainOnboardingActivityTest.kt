package uk.nhs.nhsx.sonar.android.app.onboarding

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.sonar.android.app.EspressoJunit5Test
import uk.nhs.nhsx.sonar.android.app.startTestActivity

class MainOnboardingActivityTest : EspressoJunit5Test() {

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
