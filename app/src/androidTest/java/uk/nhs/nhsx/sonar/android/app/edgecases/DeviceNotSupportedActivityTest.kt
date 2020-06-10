package uk.nhs.nhsx.sonar.android.app.edgecases

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.startTestActivity

class DeviceNotSupportedActivityTest : EspressoTest() {

    private val robot = DeviceNotSupportedRobot()

    @Test
    fun testDisplaysExpectedViews() {
        testAppContext.app.startTestActivity<DeviceNotSupportedActivity>()

        robot.checkScreenIsDisplayed()
    }
}
