package uk.nhs.nhsx.sonar.android.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.edgecases.DeviceNotSupportedRobot
import uk.nhs.nhsx.sonar.android.app.edgecases.TabletNotSupportedRobot
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.StatusFooterRobot
import uk.nhs.nhsx.sonar.android.app.status.StatusRobot
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class MainActivityTest(private val testAppContext: TestApplicationContext) {

    private val statusRobot = StatusRobot()
    private val statusFooterRobot = StatusFooterRobot()

    fun testUnsupportedDevice() {
        testAppContext.simulateUnsupportedDevice()

        startMainActivity()

        val robot = DeviceNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    fun testTabletNotSupported() {
        testAppContext.simulateTablet()

        startMainActivity()

        val robot = TabletNotSupportedRobot()
        robot.checkScreenIsDisplayed()
    }

    fun testLaunchWhenOnboardingIsFinishedButNotRegistered() {
        testAppContext.setFinishedOnboarding()

        startMainActivity()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    fun testLaunchWhenStateIsDefault() {
        testAppContext.setFullValidUser(DefaultState)

        startMainActivity()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
        statusFooterRobot.checkFooterIsDisplayed()
    }

    fun testLaunchWhenStateIsExposed() {
        val exposedState = ExposedState(DateTime.now(UTC), DateTime.now(UTC).plusDays(1))

        testAppContext.setFullValidUser(exposedState)
        startMainActivity()

        statusRobot.checkActivityIsDisplayed(ExposedState::class)
        statusFooterRobot.checkFooterIsDisplayed()
    }

    fun testLaunchWhenStateIsSymptomatic() {
        val date = DateTime.now(UTC).plusDays(1)
        val symptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        testAppContext.setFullValidUser(symptomaticState)
        startMainActivity()

        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)
        statusFooterRobot.checkFooterIsDisplayed()
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }
}
