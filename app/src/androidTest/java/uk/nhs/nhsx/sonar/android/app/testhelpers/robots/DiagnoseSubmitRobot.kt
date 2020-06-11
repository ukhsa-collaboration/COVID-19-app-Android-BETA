package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.SetChecked

class DiagnoseSubmitRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.submit_events_info)).check(matches(isDisplayed()))
    }

    fun submit() {
        onView(withId(R.id.submit_diagnosis)).perform(click())
    }

    fun checkConfirmationIsNeeded() {
        onView(withId(R.id.needConfirmationHint)).check(matches(isDisplayed()))
    }

    fun selectConfirmation() {
        onView(withId(R.id.confirmationCheckbox)).perform(SetChecked.setChecked(true))
    }
}
