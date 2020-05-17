package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onPermissionScreen(func: PermissionScreenRobot.() -> Unit) = PermissionScreenRobot().apply(func)

class PermissionScreenRobot : ViewRobot() {

    fun checkPermissionActivityIsShown() {
        checkViewWithIdIsDisplayed(R.id.permission_continue)
    }

    fun clickOnContinueButton() {
        clickOnView(R.id.permission_continue)
    }
}
