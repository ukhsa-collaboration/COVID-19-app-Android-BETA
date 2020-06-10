package uk.nhs.nhsx.sonar.android.app

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.sonar.android.app.edgecases.DeviceNotSupportedRobot
import uk.nhs.nhsx.sonar.android.app.edgecases.TabletNotSupportedRobot
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.StatusFooterRobot
import uk.nhs.nhsx.sonar.android.app.status.StatusRobot
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class MainActivityTest : EspressoJunit5Test() {

    private val statusRobot = StatusRobot()
    private val statusFooterRobot = StatusFooterRobot()

    @DisplayName("Unsupported device")
    @Test
    fun testUnsupportedDevice() {
        testAppContext.simulateUnsupportedDevice()

        startMainActivity()

        val robot = DeviceNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    @Test
    fun testTabletNotSupported() {
        testAppContext.simulateTablet()

        startMainActivity()

        val robot = TabletNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    @DisplayName("Launch when on-boarding is finished but not registered")
    @Test
    fun testLaunchWhenOnBoardingIsFinishedButNotRegistered() {
        testAppContext.setFinishedOnboarding()

        startMainActivity()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    @Test
    fun testLaunchWhenStateIsDefault() {
        testAppContext.setFullValidUser(DefaultState)

        startMainActivity()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
        statusFooterRobot.checkFooterIsDisplayed()
    }

    @Test
    fun testLaunchWhenStateIsExposed() {
        val exposedState = ExposedState(DateTime.now(UTC), DateTime.now(UTC).plusDays(1))

        testAppContext.setFullValidUser(exposedState)
        startMainActivity()

        statusRobot.checkActivityIsDisplayed(ExposedState::class)
        statusFooterRobot.checkFooterIsDisplayed()
    }

    @Test
    fun testLaunchWhenStateIsSymptomatic() {
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
