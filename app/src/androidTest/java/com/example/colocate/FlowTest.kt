/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.bluetooth.BluetoothManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.example.colocate.status.SharedPreferencesStatusStorage
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlowTest {

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(ACCESS_FINE_LOCATION)

    @Test
    fun test() {
        ensureBluetoothEnabled()
        resetStatusStorage()

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.ok_title)).check(matches(isDisplayed()))

        shouldShowOkStatusPageIfNotDiagnosed()

        shouldShowIsolationPageIfDiagnosed()
    }

    private fun resetStatusStorage() {
        val storage = activityRule.activity.statusStorage as SharedPreferencesStatusStorage
        storage.reset()
    }

    private fun ensureBluetoothEnabled() {
        val activity = activityRule.activity
        val context = activity.application.applicationContext
        val manager = context.getSystemService(BluetoothManager::class.java) as BluetoothManager
        val adapter = manager.adapter

        adapter.enable()

        var attempts = 1
        while (attempts <= 20 && !adapter.isEnabled) {
            Thread.sleep(200)
            attempts++
        }

        if (!adapter.isEnabled) {
            fail("Failed enabling bluetooth")
        }
    }

    private fun shouldShowOkStatusPageIfNotDiagnosed() {
        onView(withId(R.id.re_diagnose_button)).perform(click())

        onView(withId(R.id.diagnosis)).check(matches(isDisplayed()))
        onView(withId(R.id.no)).perform(click())
        onView(withId(R.id.no)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        onView(withId(R.id.ok_title)).check(matches(isDisplayed()))
    }

    private fun shouldShowIsolationPageIfDiagnosed() {
        onView(withId(R.id.re_diagnose_button)).perform(click())

        onView(withId(R.id.diagnosis)).check(matches(isDisplayed()))
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.yes)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        onView(withId(R.id.isolate_title)).check(matches(isDisplayed()))
    }
}
