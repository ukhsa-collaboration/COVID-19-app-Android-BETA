package uk.nhs.nhsx.sonar.android.app

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DeviceNotSupportedRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.TabletNotSupportedRobot
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusFooterRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class MainActivityTest : EspressoTest() {

    private val statusRobot =
        StatusRobot()
    private val statusFooterRobot =
        StatusFooterRobot()

    @Test
    fun unsupportedDevice() {
        testAppContext.simulateUnsupportedDevice()

        startMainActivity()

        val robot =
            DeviceNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    @Test
    fun tabletNotSupported() {
        testAppContext.simulateTablet()

        startMainActivity()

        val robot =
            TabletNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    @Test
    fun launchWhenOnBoardingIsFinishedButNotRegistered() {
        testAppContext.setFinishedOnboarding()

        startMainActivity()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    @Test
    fun launchWhenStateIsDefault() {
        testAppContext.setFullValidUser(DefaultState)

        startMainActivity()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
        statusFooterRobot.checkFooterIsDisplayed()
    }

    @Test
    fun launchWhenStateIsExposed() {
        val exposedState = ExposedState(DateTime.now(UTC), DateTime.now(UTC).plusDays(1))

        testAppContext.setFullValidUser(exposedState)
        startMainActivity()

        statusRobot.checkActivityIsDisplayed(ExposedState::class)
        statusFooterRobot.checkFooterIsDisplayed()
    }

    @Test
    fun launchWhenStateIsSymptomatic() {
        val date = DateTime.now(UTC).plusDays(1)
        val symptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        testAppContext.setFullValidUser(symptomaticState)
        startMainActivity()

        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)
        statusFooterRobot.checkFooterIsDisplayed()
    }

    private fun startMainActivity() {
        testAppContext.app.startTestActivity<MainActivity>()
    }
}
