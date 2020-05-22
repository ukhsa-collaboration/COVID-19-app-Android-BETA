package uk.nhs.nhsx.sonar.android.app.onboarding

import androidx.core.text.HtmlCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.SonarApplication

class PostCodeRobot {

    fun enterPostCode(postCode: String) {
        onView(withId(R.id.postCodeEditText)).perform(typeText(postCode))
        closeSoftKeyboard()
    }

    fun clickContinue() {
        onView(withId(R.id.postCodeContinue)).perform(click())
    }

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.postCodeContinue)).check(matches(isDisplayed()))
    }

    fun checkActivityDoesNotExist() {
        onView(withId(R.id.postCodeContinue)).check(doesNotExist())
    }

    fun checkTitleIsDisplayed() {
        onView(withText(R.string.post_code_title)).check(matches(isDisplayed()))
    }

    fun checkExampleIsDisplayed() {
        onView(withText(R.string.post_code_example)).check(matches(isDisplayed()))
    }

    fun checkInvalidHintIsNotDisplayed() {
        onView(withId(R.id.invalidPostCodeHint)).check(matches(not(isDisplayed())))
    }

    fun checkInvalidHintIsDisplayed() {
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
