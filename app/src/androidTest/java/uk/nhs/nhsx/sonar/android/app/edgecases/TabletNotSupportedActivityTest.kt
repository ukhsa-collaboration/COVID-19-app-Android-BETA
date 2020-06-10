package uk.nhs.nhsx.sonar.android.app.edgecases

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.sonar.android.app.EspressoJunit5Test
import uk.nhs.nhsx.sonar.android.app.startTestActivity

class TabletNotSupportedActivityTest : EspressoJunit5Test() {

    private val robot = TabletNotSupportedRobot()

    @Test
    fun testDisplaysExpectedViews() {
        testAppContext.app.startTestActivity<TabletNotSupportedActivity>()

        robot.checkScreenIsDisplayed()
    }
}
