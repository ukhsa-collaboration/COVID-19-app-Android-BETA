package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.appcompat.widget.AppCompatImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewContainsText
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat

class CurrentAdviceRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.current_advice_title))
            .check(matches(isDisplayed()))
    }

    fun checkIconIsShowing() {
        onView(withId(R.id.scrollView))
            .check(matches(hasDescendant(isAssignableFrom(AppCompatImageView::class.java))))
            .check(matches(isDisplayed()))
    }

    fun checkCorrectStateIsDisplay(userState: UserState) {
        userState.until()
            ?.let { checkViewContainsText(R.id.current_advice_desc, it.toUiFormat()) }
            ?: checkViewHasText(R.id.current_advice_desc, R.string.current_advice_desc_simple)
    }

    fun checkAdviceUrlIsDisplayed() {
        onView(withId(R.id.read_specific_advice))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }
}
