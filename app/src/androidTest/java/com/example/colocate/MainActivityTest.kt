/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    fun test() {
        onView(withId(R.id.confirm_onboarding)).perform(click())

        shouldShowOkStatusPageIfNotDiagnosed()

        onView(withId(R.id.re_diagnose_button)).perform(click())

        shouldShowIsolationPageIfDiagnosed()
    }

    private fun shouldShowOkStatusPageIfNotDiagnosed() {
        onView(withId(R.id.diagnosis)).check(matches(isDisplayed()))
        onView(withId(R.id.no)).perform(click())
        onView(withId(R.id.no)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())
        onView(withId(R.id.ok_title)).check(matches(isDisplayed()))
    }

    private fun shouldShowIsolationPageIfDiagnosed() {
        onView(withId(R.id.diagnosis)).check(matches(isDisplayed()))
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.yes)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())
        onView(withId(R.id.isolate_title)).check(matches(isDisplayed()))
    }
}
