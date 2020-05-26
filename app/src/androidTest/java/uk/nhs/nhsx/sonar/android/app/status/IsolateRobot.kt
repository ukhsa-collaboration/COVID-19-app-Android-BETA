package uk.nhs.nhsx.sonar.android.app.status

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.stringFromResId
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat

class IsolateRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.statusTitle)).check(matches(isDisplayed()))
    }

    fun clickBookTestCard() {
        onView(withId(R.id.book_test_card)).perform(click())
    }

    fun clickCurrentAdviceCard() {
        onView(withId(R.id.latest_advice_symptomatic)).perform(click())
    }

    fun checkStatusTitle(@StringRes stringRes: Int) {
        val stringValue = stringFromResId(stringRes)
        onView(withId(R.id.statusTitle))
            .check(matches(withText(stringValue)))
            .check(matches(isDisplayed()))
    }

    fun checkStatusDescription(state: UserState) {
        val expected = "On ${state.until().toUiFormat()} this app will notify you to update your symptoms. Please read your full advice below."
        onView(withId(R.id.statusDescription))
            .check(matches(withText(expected)))
            .check(matches(isDisplayed()))
    }
}
