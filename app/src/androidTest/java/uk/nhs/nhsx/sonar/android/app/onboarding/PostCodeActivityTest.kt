package uk.nhs.nhsx.sonar.android.app.onboarding

import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext

class PostCodeActivityTest(testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val postCodeRobot = PostCodeRobot()

    fun pristineState() {
        app.startTestActivity<PostCodeActivity>()

        postCodeRobot.checkActivityIsDisplayed()
        postCodeRobot.checkTitleIsDisplayed()
        postCodeRobot.checkExampleIsDisplayed()
        postCodeRobot.checkInvalidHintIsNotDisplayed()
        postCodeRobot.checkEditTextIs("")
        postCodeRobot.checkRationaleIsVisible()
    }

    fun emptyPostCodeShowsInvalidHint() {
        app.startTestActivity<PostCodeActivity>()

        postCodeRobot.clickContinue()
        postCodeRobot.checkInvalidHintIsDisplayed()
    }

    fun invalidPostCodeShowsInvalidHint() {
        app.startTestActivity<PostCodeActivity>()

        postCodeRobot.enterPostCode("1")
        postCodeRobot.clickContinue()
        postCodeRobot.checkInvalidHintIsDisplayed()
    }

    fun validPostCodeProceedsToNextView() {
        app.startTestActivity<PostCodeActivity>()

        postCodeRobot.enterPostCode("E1")
        postCodeRobot.clickContinue()
        postCodeRobot.checkActivityDoesNotExist()
    }
}
