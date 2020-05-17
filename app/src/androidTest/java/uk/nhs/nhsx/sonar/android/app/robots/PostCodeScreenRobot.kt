package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onPostCodeScreen(func: PostCodeScreenRobot.() -> Unit) = PostCodeScreenRobot().apply(func)

class PostCodeScreenRobot : ViewRobot() {
    fun checkPostCodeActivityIsShown() {
        checkViewWithIdIsDisplayed(R.id.postCodeContinue)
    }

    fun clickOnContinueButton() {
        clickOnView(R.id.postCodeContinue)
    }

    fun checkInvalidPostCodeHintIsDisplayed() {
        checkViewWithIdIsDisplayed(R.id.invalidPostCodeHint)
    }

    fun typePostCode(text: String) {
        typeTextInEditText(R.id.postCodeEditText, text)
    }
}
