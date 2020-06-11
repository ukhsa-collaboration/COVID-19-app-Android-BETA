package uk.nhs.nhsx.sonar.android.app.edgecases

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest

class TabletNotSupportedActivityTest : EspressoTest() {

    private val robot = TabletNotSupportedRobot()

    @Test
    fun testDisplaysExpectedViews() {
        testAppContext.app.startTestActivity<TabletNotSupportedActivity>()

        robot.checkScreenIsDisplayed()
    }
}
