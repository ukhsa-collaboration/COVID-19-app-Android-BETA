package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onAtRiskScreen(func: AtRiskScreenRobot.() -> Unit) = AtRiskScreenRobot().apply(func)

class AtRiskScreenRobot : ViewRobot() {

    fun checkAtRiskActivityIsShown() {
        checkViewWithIdIsDisplayed(R.id.status_amber)
    }
}
