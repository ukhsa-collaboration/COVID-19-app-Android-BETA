package uk.nhs.nhsx.sonar.android.app.scenarios

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.FlowTestStartActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.MainOnboardingRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.PermissionRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.PostCodeRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot

class OnBoardingTest : EspressoTest() {

    private val mainOnBoardingRobot = MainOnboardingRobot()
    private val postCodeRobot = PostCodeRobot()
    private val permissionRobot = PermissionRobot()
    private val statusRobot = StatusRobot()

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @Before
    fun setupFlowTestActivity() {
        testAppContext.app.startTestActivity<FlowTestStartActivity>()
    }

    @Test
    fun registration() {
        testAppContext.simulateBackendDelay(400)

        startMainActivity()
        mainOnBoardingRobot.clickConfirmOnboarding()

        postCodeRobot.checkActivityIsDisplayed()
        postCodeRobot.enterPostCode("E1")
        postCodeRobot.clickContinue()

        permissionRobot.checkActivityIsDisplayed()
        permissionRobot.clickContinue()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
        statusRobot.checkFinalisingSetup()

        testAppContext.verifyRegistrationFlow()

        statusRobot.checkFeelUnwellIsDisplayed()
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }
}
