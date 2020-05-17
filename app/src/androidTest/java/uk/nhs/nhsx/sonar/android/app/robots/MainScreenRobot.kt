package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onMainScreen(func: MainScreenRobot.() -> Unit) = MainScreenRobot().apply(func)

class MainScreenRobot : ScreenRobot() {
    fun clickOnConfirmButton() {
        clickOnView(R.id.confirm_onboarding)
    }

    fun checkMainActivityIsShown() {
        checkViewWithIdIsDisplayed(R.id.confirm_onboarding)
    }
}
