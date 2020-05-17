package uk.nhs.nhsx.sonar.android.app.robots

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.SetChecked

fun onDiagnosisScreen(func: DiagnosisScreenRobot.() -> Unit) = DiagnosisScreenRobot().apply(func)

class DiagnosisScreenRobot : ViewRobot() {

    fun checkCanTransitionToIsolateActivity() {

        // Temperature step
        onView(withId(R.id.temperature_question)).check(matches(isDisplayed()))
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.yes)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        // Cough step
        onView(withId(R.id.cough_question)).check(matches(isDisplayed()))
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.yes)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        // Review Step
        onView(withId(R.id.symptoms_date_prompt))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.symptoms_date_prompt_all)))

        onView(withId(R.id.review_answer_temperature))
            .check(matches(withText(R.string.i_do_temperature)))

        onView(withId(R.id.review_answer_cough))
            .check(matches(withText(R.string.i_do_cough)))

        onView(withId(R.id.submit_diagnosis)).perform(click())
        onView(withId(R.id.date_selection_error)).check(matches(isDisplayed()))

        onView(withId(R.id.symptoms_date_spinner))
            .check(matches(withSpinnerText(R.string.start_date)))

        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo(), click())
        onData(CoreMatchers.anything()).atPosition(3).perform(click())
        onView(withText("Cancel")).perform(click())
        onView(withId(R.id.symptoms_date_spinner))
            .check(matches(withSpinnerText(R.string.start_date)))

        val todayAsString = LocalDate.now().toString("EEEE, MMMM dd")
        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo(), click())
        onData(CoreMatchers.anything()).atPosition(3).perform(click())
        onView(withText("OK")).perform(click())
        onView(withId(R.id.symptoms_date_spinner)).check(matches(withSpinnerText(todayAsString)))

        onView(withId(R.id.symptoms_date_spinner)).perform(scrollTo(), click())
        onData(CoreMatchers.anything()).atPosition(1).perform(click())
        onView(withId(R.id.symptoms_date_spinner))
            .check(matches(withSpinnerText(R.string.yesterday)))

        onView(withId(R.id.submit_diagnosis)).perform(click())

        // Confirmation Step
        onView(withId(R.id.submit_events_info)).check(matches(isDisplayed()))
        onView(withId(R.id.submit_diagnosis)).perform(click())

        onView(withId(R.id.needConfirmationHint)).check(matches(isDisplayed()))
        onView(withId(R.id.confirmationCheckbox)).perform(scrollTo(), SetChecked.setChecked(true))
        onView(withId(R.id.submit_diagnosis)).perform(click())
    }

    fun checkCanTransitionToIsolateActivitySimplified() {

        // Temperature Step
        checkViewHasText(R.id.progress, R.string.progress_half)
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        // Cough Step
        checkViewHasText(R.id.progress, R.string.progress_two_out_of_two)
        onView(withId(R.id.no)).perform(click())
        onView(withId(R.id.confirm_diagnosis)).perform(click())
    }
}
