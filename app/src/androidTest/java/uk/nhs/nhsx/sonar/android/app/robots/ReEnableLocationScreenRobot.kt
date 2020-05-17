package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onReEnableLocationScreen(func: ReEnableLocationScreenRobot.() -> Unit) = ReEnableLocationScreenRobot().apply(func)

class ReEnableLocationScreenRobot : ViewRobot() {

    fun checkReEnableLocationTitleIsShown() {
        waitForText(R.string.re_enable_location_title)
    }
}
