package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.anything
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.R

class DiagnoseReviewRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.symptoms_date_prompt))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.symptoms_date_prompt_all)))
    }

    fun submit() {
        onView(withId(R.id.submit_diagnosis)).perform(click())
    }

    fun checkNoDateSelected() {
        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo())
        onView(withId(R.id.symptoms_date_spinner)).check(matches(withSpinnerText(R.string.start_date)))
    }

    fun checkDateErrorIsDisplayed() {
        onView(withId(R.id.date_selection_error)).check(matches(isDisplayed()))
    }

    fun openCalendar() {
        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo(), click())
        onData(anything()).atPosition(3).perform(click())
    }

    fun cancelCalendar() {
        onView(withText("Cancel")).perform(click())
    }

    fun okCalendar() {
        onView(withText("OK")).perform(click())
    }

    fun checkSelectedDate(now: LocalDate) {
        val dateString = now.toString("EEEE, MMMM dd")
        onView(withId(R.id.symptoms_date_spinner)).check(matches(withSpinnerText(dateString)))
    }

    fun checkSelectedDate(@StringRes value: Int) {
        onView(withId(R.id.symptoms_date_spinner)).check(matches(withSpinnerText(value)))
    }

    fun selectYesterday() {
        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo(), click())
        onData(anything()).atPosition(1).perform(click())
    }
}
