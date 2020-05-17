package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestSonarServiceDispatcher

fun onReferenceCodePanel(func: ReferenceCodePanelRobot.() -> Unit) = ReferenceCodePanelRobot().apply(func)

class ReferenceCodePanelRobot : ScreenRobot() {

    fun checkDisplayOfReferenceCode() {
        checkViewHasText(R.id.reference_code, TestSonarServiceDispatcher.REFERENCE_CODE)
    }

    fun clickCloseButton() {
        clickOnView(R.id.close)
    }
}
