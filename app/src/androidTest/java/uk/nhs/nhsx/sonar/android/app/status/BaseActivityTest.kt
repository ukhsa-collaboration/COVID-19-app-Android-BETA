/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.EdgeCaseRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.StatusRobot

class BaseActivityTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val edgeCaseRobot = EdgeCaseRobot()

    private fun startActivity() {
        testAppContext.setFullValidUser(DefaultState)
        startTestActivity<StatusActivity>()
    }

    @Test
    fun resumeWhenBluetoothIsDisabled() {
        startActivity()

        triggerResumeAfter {
            testAppContext.bluetoothSettings.ensureBluetoothDisabled()
        }

        edgeCaseRobot.checkTitle(R.string.re_enable_bluetooth_title)
        edgeCaseRobot.clickTakeAction()

        // TODO: fix flaky check
        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    @Test
    fun resumeWhenLocationAccessIsDisabled() {
        startActivity()

        testAppContext.appPermissions.disableLocationAccess()
        edgeCaseRobot.checkTitle(R.string.re_enable_location_title)

        testAppContext.appPermissions.enableLocationAccess()
        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    @Test
    fun resumeWhenLocationPermissionIsRevoked() {
        startActivity()

        triggerResumeAfter {
            testAppContext.appPermissions.revokeLocationPermission()
        }

        edgeCaseRobot.checkTitle(R.string.re_allow_location_permission_title)
        edgeCaseRobot.clickTakeAction()

        testAppContext.device.wait(Until.gone(By.text("Allow this app to access your location to continue")), 500)
        testAppContext.appPermissions.grantLocationPermission()
        testAppContext.device.pressBack()

        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    private fun triggerResumeAfter(function: () -> Unit) {
        statusRobot.clickCurrentAdviceCard()
        function()
        testAppContext.device.pressBack()
    }
}
