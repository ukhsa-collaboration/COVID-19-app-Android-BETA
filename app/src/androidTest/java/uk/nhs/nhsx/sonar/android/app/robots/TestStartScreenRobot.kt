package uk.nhs.nhsx.sonar.android.app.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R

fun onTestStartScreen(func: TestStartScreenRobot.() -> Unit) = TestStartScreenRobot().apply(func)

class TestStartScreenRobot : ScreenRobot() {
    fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }
}
