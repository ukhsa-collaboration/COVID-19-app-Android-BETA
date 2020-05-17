package uk.nhs.nhsx.sonar.android.app.robots

import android.os.Build
import uk.nhs.nhsx.sonar.android.app.R

fun onEdgeCaseScreen(func: EdgeCaseScreenRobot.() -> Unit) = EdgeCaseScreenRobot().apply(func)

class EdgeCaseScreenRobot : ScreenRobot() {
    fun checkBlueToothMessageIsDisplayed() {
        checkViewHasText(R.id.edgeCaseTitle, R.string.re_enable_bluetooth_title)
    }

    fun clickOnEnableBlueToothButton() {
        clickOnView(R.id.takeActionButton)
    }

    fun checkLocationPermissionMessageIsShown() {
        if (Build.VERSION.SDK_INT >= 29) {
            checkViewHasText(
                R.id.edgeCaseTitle,
                R.string.grant_location_permission_title)
        } else {
            checkViewHasText(
                R.id.edgeCaseTitle,
                R.string.grant_location_permission_title_pre_10
            )
        }
    }

    fun checkReAllowLocationPermissionMessageIsShown() {
        checkViewHasText(R.id.edgeCaseTitle, R.string.re_allow_location_permission_title)
    }

    fun waitUntilScreenIsNotSeen() {
        waitUntilCannotFindText(R.string.grant_location_permission_title)
        waitUntilCannotFindText(R.string.grant_location_permission_title_pre_10)
    }
}
