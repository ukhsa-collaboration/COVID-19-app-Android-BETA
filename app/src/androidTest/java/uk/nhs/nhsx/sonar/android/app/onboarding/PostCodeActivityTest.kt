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
        postCodeRobot.checkActivityIsDisplayed()
        postCodeRobot.checkTitleIsDisplayed()
        postCodeRobot.checkExampleIsDisplayed()
        postCodeRobot.checkInvalidHintIsNotDisplayed()
        postCodeRobot.checkEditTextIs("")
        postCodeRobot.checkRationaleIsVisible()
    }

    @Test
    fun emptyPostCodeShowsInvalidHint() {
        postCodeRobot.clickContinue()
        postCodeRobot.checkInvalidHintIsDisplayed()
    }

    @Test
    fun invalidPostCodeShowsInvalidHint() {
        postCodeRobot.enterPostCode("1")
        postCodeRobot.clickContinue()
        postCodeRobot.checkInvalidHintIsDisplayed()
    }

    @Test
    fun validPostCodeProceedsToNextView() {
        postCodeRobot.enterPostCode("E1")
        postCodeRobot.clickContinue()
        postCodeRobot.checkActivityDoesNotExist()
    }
}
