package uk.nhs.nhsx.sonar.android.app.scenarios

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.MainActivity
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DeviceNotSupportedRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.TabletNotSupportedRobot

class StartAppTest : EspressoTest() {

    private val statusRobot = StatusRobot()

    private fun startMainActivity() {
        testAppContext.app.startTestActivity<MainActivity>()
    }

    @Test
    fun unsupportedDevice() {
        testAppContext.simulateUnsupportedDevice()

        startMainActivity()

        val robot = DeviceNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    @Test
    fun tabletNotSupported() {
        testAppContext.simulateTablet()

        startMainActivity()

        val robot = TabletNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    @Test
    fun launchWhenOnBoardingIsFinishedButNotRegistered() {
        testAppContext.setFinishedOnboarding()

        startMainActivity()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }
}
