package uk.nhs.nhsx.sonar.android.app.status

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher.Companion.REFERENCE_CODE

class StatusFooterRobot {

    fun checkFooterIsDisplayed() {
        checkDisplayOfReferenceCode()
        checkDisplayOfWorkplaceGuidance()
    }

    private fun checkDisplayOfReferenceCode() {
        onView(withId(R.id.reference_link_card)).perform(scrollTo(), click())
        onView(withId(R.id.reference_code)).check(matches(withText(REFERENCE_CODE)))
        onView(withContentDescription(R.string.go_back)).perform(click())
    }

    private fun checkDisplayOfWorkplaceGuidance() {
        onView(withId(R.id.workplaceGuidance)).perform(scrollTo(), click())
        onView(withId(R.id.workplace_guidance_title)).check(matches(isDisplayed()))
        onView(withContentDescription(R.string.go_back)).perform(click())
    }
}
