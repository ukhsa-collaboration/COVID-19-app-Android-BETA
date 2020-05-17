package uk.nhs.nhsx.sonar.android.app.robots

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matcher
import uk.nhs.nhsx.sonar.android.app.R

fun onOkScreen(func: OkScreenRobot.() -> Unit) = OkScreenRobot().apply(func)

class OkScreenRobot : ScreenRobot() {

    fun checkOkActivityIsShown() {
        checkTitleIsDisplayed()
        onView(withId(R.id.status_initial)).check(matches(isDisplayed()))
    }

    fun verifyCheckMySymptomsButton(matcher: Matcher<View>) {
        onView(withId(R.id.status_not_feeling_well)).check(matches(matcher))
    }

    fun clickOnNotFeelingWell() {
        scrollAndClickOnView(R.id.status_not_feeling_well)
    }

    fun clickOnExplanationLink() {
        scrollAndClickOnView(R.id.explanation_link)
    }

    fun clickOnLatestAdvice() {
        clickOnView(R.id.latest_advice_ok)
    }

    fun checkTitleIsDisplayed() {
        waitForText(R.string.status_initial_title, timeoutInMs = 6_000)
    }

    fun checkScreenIsResumed() {
        waitForText(R.string.status_initial_title)
    }
}
