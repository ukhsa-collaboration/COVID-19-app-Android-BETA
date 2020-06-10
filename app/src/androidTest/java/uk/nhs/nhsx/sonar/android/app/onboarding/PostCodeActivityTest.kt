package uk.nhs.nhsx.sonar.android.app.onboarding

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.sonar.android.app.EspressoJunit5Test
import uk.nhs.nhsx.sonar.android.app.startTestActivity

class PostCodeActivityTest : EspressoJunit5Test() {

    private val postCodeRobot = PostCodeRobot()

    @Test
    fun pristineState() {
        testAppContext.app.startTestActivity<PostCodeActivity>()

        postCodeRobot.checkActivityIsDisplayed()
        postCodeRobot.checkTitleIsDisplayed()
        postCodeRobot.checkExampleIsDisplayed()
        postCodeRobot.checkInvalidHintIsNotDisplayed()
        postCodeRobot.checkEditTextIs("")
        postCodeRobot.checkRationaleIsVisible()
    }

    @Test
    fun emptyPostCodeShowsInvalidHint() {
        testAppContext.app.startTestActivity<PostCodeActivity>()

        postCodeRobot.clickContinue()
        postCodeRobot.checkInvalidHintIsDisplayed()
    }

    @Test
    fun invalidPostCodeShowsInvalidHint() {
        testAppContext.app.startTestActivity<PostCodeActivity>()

        postCodeRobot.enterPostCode("1")
        postCodeRobot.clickContinue()
        postCodeRobot.checkInvalidHintIsDisplayed()
    }

    @Test
    fun validPostCodeProceedsToNextView() {
        testAppContext.app.startTestActivity<PostCodeActivity>()

        postCodeRobot.enterPostCode("E1")
        postCodeRobot.clickContinue()
        postCodeRobot.checkActivityDoesNotExist()
    }
}
