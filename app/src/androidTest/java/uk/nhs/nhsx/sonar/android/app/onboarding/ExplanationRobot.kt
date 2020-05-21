package uk.nhs.nhsx.sonar.android.app.onboarding

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R

class ExplanationRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.explanation_back)).check(matches(isDisplayed()))
    }

    fun clickBackButton() {
        onView(withId(R.id.explanation_back)).perform(click())
    }
}
