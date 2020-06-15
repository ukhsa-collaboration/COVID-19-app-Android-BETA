package uk.nhs.nhsx.sonar.android.app.scenarios

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.ScenarioTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DeviceNotSupportedRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.TabletNotSupportedRobot

class StartAppTest : ScenarioTest() {

    private val statusRobot = StatusRobot()

    @Test
    fun unsupportedDevice() {
        testAppContext.simulateUnsupportedDevice()

        startAppWith(testData.defaultState)

        val robot = DeviceNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    @Test
    fun tabletNotSupported() {
        testAppContext.simulateTablet()

        startAppWith(testData.defaultState)

        val robot = TabletNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    @Test
    fun launchWhenOnBoardingIsFinishedButNotRegistered() {
        testAppContext.setFinishedOnboarding()

        startAppWith(testData.defaultState)

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }
}
