package uk.nhs.nhsx.sonar.android.app.onboarding

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.startTestActivity

@RunWith(AndroidJUnit4::class)
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
