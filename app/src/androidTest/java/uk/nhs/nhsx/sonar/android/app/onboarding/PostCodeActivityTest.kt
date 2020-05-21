package uk.nhs.nhsx.sonar.android.app.onboarding

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostCodeActivityTest {

    @get:Rule
    val activityRule: ActivityTestRule<PostCodeActivity> =
        ActivityTestRule(PostCodeActivity::class.java)

    private val postCodeRobot = PostCodeRobot()

    @Test
    fun pristineState() {
        postCodeRobot.checkActivityIsShown()
        postCodeRobot.checkTitleIsDisplayed()
        postCodeRobot.checkExampleIsDisplayed()
        postCodeRobot.checkInvalidHintIsHidden()
        postCodeRobot.checkEditTextIs("")
        postCodeRobot.checkRationaleIsVisible()
    }

    @Test
    fun clickContinueWithEmptyPostCodeShouldShowHint() {
        postCodeRobot.clickContinue()
        postCodeRobot.checkInvalidHintIsVisible()
    }
}
