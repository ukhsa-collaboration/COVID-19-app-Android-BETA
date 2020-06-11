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
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot

class BluetoothTest : EspressoTest() {

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
    fun enableBluetoothThroughNotification() {
        testAppContext.setFullValidUser()
        startMainActivity()
        testAppContext.ensureBluetoothDisabled()

        testAppContext.clickOnNotificationAction(
            notificationTitleRes = R.string.notification_bluetooth_disabled_title,
            notificationTextRes = R.string.notification_bluetooth_disabled_text,
            notificationActionRes = R.string.notification_bluetooth_disabled_action
        )

        testAppContext.verifyBluetoothIsEnabled()
        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }
}
