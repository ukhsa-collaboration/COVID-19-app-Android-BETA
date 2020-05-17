package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onStatusFooterView(func: StatusFooterViewRobot.() -> Unit) = StatusFooterViewRobot().apply(func)

class StatusFooterViewRobot : ScreenRobot() {

    fun checkMedicalWorkersInstructionsNotDisplayed() {
        checkViewWithIdIsNotDisplayed(R.id.medicalWorkersInstructions)
    }
}
