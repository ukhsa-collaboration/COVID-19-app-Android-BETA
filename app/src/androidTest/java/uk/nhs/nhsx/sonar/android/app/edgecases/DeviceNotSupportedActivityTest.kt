package uk.nhs.nhsx.sonar.android.app.edgecases

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest

class DeviceNotSupportedActivityTest : EspressoTest() {

    private val robot = DeviceNotSupportedRobot()

    @Test
    fun displaysExpectedViews() {
        testAppContext.app.startTestActivity<DeviceNotSupportedActivity>()

        robot.checkScreenIsDisplayed()
    }
}
