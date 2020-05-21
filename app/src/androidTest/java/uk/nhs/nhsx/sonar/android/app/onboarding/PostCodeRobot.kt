package uk.nhs.nhsx.sonar.android.app.onboarding

import androidx.core.text.HtmlCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.SonarApplication

class PostCodeRobot {

    fun clickContinue() {
        onView(withId(R.id.postCodeContinue)).perform(click())
    }

    fun checkActivityIsShown() {
        onView(withId(R.id.postCodeContinue)).check(matches(isDisplayed()))
    }

    fun checkTitleIsDisplayed() {
        onView(withText(R.string.post_code_title)).check(matches(isDisplayed()))
    }

    fun checkExampleIsDisplayed() {
        onView(withText(R.string.post_code_example)).check(matches(isDisplayed()))
    }

    fun checkInvalidHintIsHidden() {
        onView(withId(R.id.invalidPostCodeHint)).check(matches(not(isDisplayed())))
    }

    fun checkInvalidHintIsVisible() {
        onView(withId(R.id.invalidPostCodeHint)).check(matches(isDisplayed()))
    }

    fun checkEditTextIs(expectedValue: String) {
        onView(withId(R.id.postCodeEditText)).check(matches(withText(expectedValue)))
    }

    fun checkRationaleIsVisible() {
        onView(withText(R.string.post_code_rationale_title)).check(matches(isDisplayed()))

        val resources = ApplicationProvider.getApplicationContext<SonarApplication>().resources
        val postCodeRationaleRes: String = resources.getString(R.string.post_code_rationale)
        val postCodeRationale =
            HtmlCompat.fromHtml(postCodeRationaleRes, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()

        onView(withText(postCodeRationale)).check(matches(isDisplayed()))
    }
}
