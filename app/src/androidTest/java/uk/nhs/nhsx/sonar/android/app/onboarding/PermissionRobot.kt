package uk.nhs.nhsx.sonar.android.app.onboarding

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R

class PermissionRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.permission_continue)).check(matches(isDisplayed()))
    }

    fun clickContinue() {
        onView(withId(R.id.permission_continue)).perform(click())
    }
}
