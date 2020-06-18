/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText

class DeviceNotSupportedRobot {

    fun checkScreenIsDisplayed() {
        checkToolbar()
        checkTitle()
        checkDescription()
        checkBottomUrlIsDisplayed()
    }

    private fun checkToolbar() {
        onView(withId(R.id.nhsLogo)).check(matches(isDisplayed()))
        checkViewHasText(R.id.nhsLogoName, R.string.app_title)
    }

    private fun checkTitle() {
        checkViewHasText(R.id.edgeCaseTitle, R.string.device_not_supported_title)
    }

    private fun checkDescription() {
        checkViewHasText(R.id.edgeCaseText, R.string.device_not_supported_rationale)
    }

    private fun checkBottomUrlIsDisplayed() {
        checkViewHasText(R.id.bleInfoUlr, R.string.ble_information_link)
        onView(withId(R.id.bleInfoUlr)).check(matches(isClickable()))
    }
}
