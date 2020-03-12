package com.example.colocate

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class DiagnoseActivityTest {


    @get:Rule
    val activityRule: ActivityTestRule<DiagnoseActivity> = ActivityTestRule(DiagnoseActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    fun shouldShowOkStatusPageIfNotDiagnosed() {
        onView(withId(R.id.diagnosis)).check(matches(isDisplayed()))
        onView(withText(R.string.no)).perform(click())
        onView(withId(R.id.confirm_diagnosis)).perform(click())
        onView(withId(R.id.ok_title)).check(matches(isDisplayed()))
    }

    @Test
    fun shouldShowIsolationPageIfDiagnosed() {
        onView(withId(R.id.diagnosis)).check(matches(isDisplayed()))
        onView(withText(R.string.yes)).perform(click())
        onView(withId(R.id.confirm_diagnosis)).perform(click())
        onView(withId(R.id.isolate_title)).check(matches(isDisplayed()))
    }

}