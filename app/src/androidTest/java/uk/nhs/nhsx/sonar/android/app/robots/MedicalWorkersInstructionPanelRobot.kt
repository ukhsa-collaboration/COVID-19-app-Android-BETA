package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onMedicalWorkersInstructionPanel(func: MedicalWorkersInstructionPanelRobot.() -> Unit) = MedicalWorkersInstructionPanelRobot().apply(func)

class MedicalWorkersInstructionPanelRobot : ScreenRobot() {

    fun checkDisplayOfMedicalWorkersInstructions() {
        scrollAndClickOnView(R.id.medicalWorkersInstructions)
        checkViewHasText(
            R.id.medicalWorkersInstructionsTitle,
            R.string.medical_workers_instructions_title
        )
        checkViewHasText(
            R.id.medicalWorkersInstructionsText,
            R.string.medical_workers_instructions_text
        )
        clickOnView(R.id.closeButton)
    }
}
