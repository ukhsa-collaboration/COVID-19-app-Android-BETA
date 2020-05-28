package uk.nhs.nhsx.sonar.android.app.onboarding

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R

class MainOnboardingActivityTest {

    private val mainOnboardingRobot = MainOnboardingRobot()
    private val explanationRobot = ExplanationRobot()

    fun testExplanation() {
        startMainActivity()

        mainOnboardingRobot.checkActivityIsDisplayed()
        mainOnboardingRobot.clickExplanationLink()

        explanationRobot.checkActivityIsDisplayed()
        explanationRobot.clickBackButton()

        mainOnboardingRobot.checkActivityIsDisplayed()
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }
}
