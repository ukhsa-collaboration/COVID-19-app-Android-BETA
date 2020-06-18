/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import uk.nhs.nhsx.sonar.android.app.FlowTestStartActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.UserState

abstract class ScenarioTest : EspressoTest() {

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @Before
    fun setupFlowTestActivity() {
        startTestActivity<FlowTestStartActivity>()
    }

    protected fun startAppWith(state: UserState) {
        testAppContext.setFullValidUser(state)
        launchFlowTestStartActivity()
    }

    protected fun startAppWithEmptyState() {
        launchFlowTestStartActivity()
    }

    private fun launchFlowTestStartActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }
}
