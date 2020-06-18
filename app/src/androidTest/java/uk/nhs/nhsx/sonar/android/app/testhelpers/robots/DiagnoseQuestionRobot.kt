/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText

class DiagnoseQuestionRobot {

    fun checkProgress(@StringRes progress: Int) {
        checkViewHasText(R.id.progress, progress)
    }

    fun answerYesTo(@IdRes questionId: Int) {
        answerTo(questionId, R.id.yes)
    }

    fun answerNoTo(@IdRes questionId: Int) {
        answerTo(questionId, R.id.no)
    }

    private fun answerTo(@IdRes questionId: Int, @IdRes answerId: Int) {
        onView(withId(questionId)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(answerId)).perform(scrollTo()).perform(click())
        onView(withId(answerId)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())
    }
}
