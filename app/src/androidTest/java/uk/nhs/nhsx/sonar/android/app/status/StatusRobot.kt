package uk.nhs.nhsx.sonar.android.app.status

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.stringFromResId
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import kotlin.reflect.KClass

class StatusRobot {

    fun <T : UserState> checkActivityIsDisplayed(userState: KClass<T>) {
        val title = when (userState) {
            DefaultState::class -> R.string.status_initial_title
            ExposedState::class -> R.string.status_exposed_title
            SymptomaticState::class -> R.string.status_symptomatic_title
            CheckinState::class -> R.string.status_symptomatic_title
            PositiveState::class -> R.string.status_positive_test_title
            else -> throw IllegalArgumentException("Not able to match title: $userState")
        }

        onView(withId(R.id.statusTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(title)))
    }

    fun clickBookTestCard() {
        onView(withId(R.id.bookTest)).perform(click())
    }

    fun clickCurrentAdviceCard() {
        onView(withId(R.id.readLatestAdvice)).perform(click())
    }

    fun checkStatusTitle(@StringRes stringRes: Int) {
        val stringValue = stringFromResId(stringRes)
        onView(withId(R.id.statusTitle))
            .check(matches(withText(stringValue)))
            .check(matches(isDisplayed()))
    }

    fun checkStatusDescription(state: UserState) {
        val expected = "On ${state.until()
            .toUiFormat()} this app will notify you to update your symptoms. Please read your full advice below."
        onView(withId(R.id.statusDescription))
            .check(matches(withText(expected)))
            .check(matches(isDisplayed()))
    }

    fun checkBookVirusTestCardIsDisplayed() {
        onView(withId(R.id.bookTest))
            .check(matches(isDisplayed()))
    }

    fun checkBookVirusTestCardIsNotDisplayed() {
        onView(withId(R.id.bookTest))
            .check(matches(not(isDisplayed())))
    }
}
