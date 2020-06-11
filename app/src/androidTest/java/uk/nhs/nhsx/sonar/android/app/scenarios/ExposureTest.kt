package uk.nhs.nhsx.sonar.android.app.scenarios

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import org.joda.time.LocalDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.FlowTestStartActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class ExposureTest : EspressoTest() {

    private val statusRobot = StatusRobot()

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @Before
    fun setupFlowTestActivity() {
        testAppContext.app.startTestActivity<FlowTestStartActivity>()
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }

    @Test
    fun whileInNeutral() {
        startAppWith(UserState.default())

        statusRobot.checkActivityIsDisplayed(DefaultState::class)

        testAppContext.apply {
            simulateExposureNotificationReceived()
            clickOnNotification(
                R.string.contact_alert_notification_title,
                R.string.contact_alert_notification_text
            )
        }

        statusRobot.checkActivityIsDisplayed(ExposedState::class)
        statusRobot.checkStatusDescription(userState())
    }

    @Test
    fun whileInSymptomatic() {
        startAppWith(
            UserState.symptomatic(
                LocalDate.now(),
                nonEmptySetOf(Symptom.TEMPERATURE)
            )
        )

        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)

        testAppContext.apply {
            simulateExposureNotificationReceived()
        }

        statusRobot.checkActivityIsDisplayed(SymptomaticState::class)
    }

    private fun startAppWith(state: UserState) {
        testAppContext.setFullValidUser(state)
        startMainActivity()
    }
}
