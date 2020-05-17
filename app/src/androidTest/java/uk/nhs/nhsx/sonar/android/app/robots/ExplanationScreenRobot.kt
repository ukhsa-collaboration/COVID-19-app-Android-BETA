package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onExplanationScreen(func: ExplanationScreenRobot.() -> Unit) = ExplanationScreenRobot().apply(func)

class ExplanationScreenRobot : ScreenRobot() {

    fun checkExplanationActivityIsShown() {
        checkViewWithIdIsDisplayed(R.id.explanation_back)
    }

    fun clickCloseButton() {
        clickOnView(R.id.explanation_back)
    }
}
