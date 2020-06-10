package uk.nhs.nhsx.sonar.android.app.edgecases

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.startTestActivity

@RunWith(AndroidJUnit4::class)
class TabletNotSupportedActivityTest: EspressoTest() {

    private val robot = TabletNotSupportedRobot()

    @Test
    fun testDisplaysExpectedViews() {
        testAppContext.app.startTestActivity<TabletNotSupportedActivity>()

        robot.checkScreenIsDisplayed()
    }
}
