/*
 * Copyright © 2020 NHSX. All rights reserved.
 */
package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.sonar.android.app.R

class ReferenceCodeRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.reference_code_title))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.reference_code_title)))
    }

    fun checkReferenceCodeIs(code: String) {
        onView(withId(R.id.reference_code))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(code)))
    }

    fun checkTestResultMeaningSection() {
        onView(withId(R.id.testResultMeaningTitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.test_result_meaning_title)))

        onView(withId(R.id.testResultMeaningDescription))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.test_result_meaning_description)))

        onView(withId(R.id.testResultMeaningUrl))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
            .check(matches(isClickable()))
    }
}
