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
import com.example.colocate.persistence.SharedPreferencesResidentIdProvider
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.SharedPreferencesStatusStorage
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FlowTest {

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(ACCESS_FINE_LOCATION)

    private var testAppContext: TestApplicationContext? = null

    @Before
    fun setup() {
        testAppContext = TestApplicationContext(activityRule)
        ensureBluetoothEnabled()
    }

    @After
    fun teardown() {
        testAppContext?.shutdownMockServer()
    }

    @Test
    fun testRegistration() {
        unsetResidentId()

        onView(withId(R.id.start_main_activity)).perform(click())

        onView(withId(R.id.confirm_onboarding)).perform(click())

        checkRegistrationActivityIsShown()

        onView(withId(R.id.confirm_registration)).perform(click())

        testAppContext!!.apply {
            verifyReceivedRegistrationRequest()
            simulateActivationCodeReceived()
            verifyReceivedActivationRequest()
            verifyResidentIdAndSecretKey()
        }

        checkOkActivityIsShown()
    }

    @Test
    fun testLaunchWhenStateIsOk() {
        setStatus(CovidStatus.OK)
        setValidResidentId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkOkActivityIsShown()
        checkCanTransitionToIsolateActivity()
    }

    @Test
    fun testLaunchWhenStateIsPotential() {
        setStatus(CovidStatus.POTENTIAL)
        setValidResidentId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkAtRiskActivityIsShown()
    }

    @Test
    fun testLaunchWhenStateIsRed() {
        setStatus(CovidStatus.RED)
        setValidResidentId()

        onView(withId(R.id.start_main_activity)).perform(click())

        checkIsolateActivityIsShown()
    }

    private fun checkRegistrationActivityIsShown() {
        onView(withId(R.id.confirm_registration)).check(matches(isDisplayed()))
    }

    private fun checkOkActivityIsShown() {
        onView(withId(R.id.ok_title)).check(matches(isDisplayed()))
    }

    private fun checkAtRiskActivityIsShown() {
        onView(withId(R.id.potential_disclaimer_title)).check(matches(isDisplayed()))
    }

    private fun checkIsolateActivityIsShown() {
        onView(withId(R.id.isolate_disclaimer_title)).check(matches(isDisplayed()))
    }

    private fun setStatus(covidStatus: CovidStatus) {
        val storage = activityRule.activity.statusStorage as SharedPreferencesStatusStorage
        storage.update(covidStatus)
    }

    private fun unsetResidentId() {
        val residentIdProvider = activityRule.activity.residentIdProvider as SharedPreferencesResidentIdProvider
        residentIdProvider.clear()
    }

    private fun setValidResidentId() {
        val residentIdProvider = activityRule.activity.residentIdProvider
        residentIdProvider.setResidentId(UUID.randomUUID().toString())
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

    private fun checkCanTransitionToIsolateActivity() {
        onView(withId(R.id.re_diagnose_button)).perform(click())

        onView(withId(R.id.diagnosis)).check(matches(isDisplayed()))
        onView(withId(R.id.yes)).perform(click())
        onView(withId(R.id.yes)).check(matches(isChecked()))
        onView(withId(R.id.confirm_diagnosis)).perform(click())

        onView(withId(R.id.isolate_title)).check(matches(isDisplayed()))
    }
}
