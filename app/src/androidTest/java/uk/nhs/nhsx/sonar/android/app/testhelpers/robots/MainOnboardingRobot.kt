/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R

class MainOnboardingRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.confirm_onboarding)).check(matches(isDisplayed()))
    }

    fun clickExplanationLink() {
        onView(withId(R.id.explanation_link)).perform(scrollTo(), click())
    }

    fun clickConfirmOnboarding() {
        onView(withId(R.id.confirm_onboarding)).perform(click())
    }
}
