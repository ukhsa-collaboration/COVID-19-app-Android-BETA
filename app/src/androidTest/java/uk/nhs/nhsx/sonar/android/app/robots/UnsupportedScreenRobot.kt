package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onDeviceUnsupportedScreen(func: UnsupportedScreenRobot.() -> Unit) = UnsupportedScreenRobot().apply(func)

class UnsupportedScreenRobot : ScreenRobot() {

    fun verifyUnsupportedMessageIsShown() {
        checkViewHasText(R.id.edgeCaseTitle, R.string.device_not_supported_title)
    }

    fun verifyTabletUnsupportedMessageIsShown() {
        checkViewHasText(R.id.edgeCaseTitle, R.string.tablet_support_title)
    }
}
