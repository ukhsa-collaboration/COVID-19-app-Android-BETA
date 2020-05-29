package uk.nhs.nhsx.sonar.android.app.edgecases

import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext

class TabletNotSupportedActivityTest(private val testAppContext: TestApplicationContext) {

    private val robot = TabletNotSupportedRobot()

    fun testDisplaysExpectedViews() {
        testAppContext.app.startTestActivity<TabletNotSupportedActivity>()

        robot.checkScreenIsDisplayed()
    }
}
