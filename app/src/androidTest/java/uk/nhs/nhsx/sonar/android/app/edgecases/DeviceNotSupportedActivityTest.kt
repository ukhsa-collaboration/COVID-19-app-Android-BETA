package uk.nhs.nhsx.sonar.android.app.edgecases

import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext

class DeviceNotSupportedActivityTest(private val testAppContext: TestApplicationContext) {

    private val robot = DeviceNotSupportedRobot()

    fun testDisplaysExpectedViews() {
        testAppContext.app.startTestActivity<DeviceNotSupportedActivity>()

        robot.checkToolbar()
        robot.checkTitle()
        robot.checkDescription()
        robot.checkBottomUrlIsDisplayed()
    }
}
