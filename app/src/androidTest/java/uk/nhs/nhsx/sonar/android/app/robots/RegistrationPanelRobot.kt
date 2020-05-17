package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onRegistrationPanel(func: RegistrationPanelRobot.() -> Unit) = RegistrationPanelRobot().apply(func)

class RegistrationPanelRobot : ScreenRobot() {
    fun checkWorkingOkMessageIsShown() {
        checkViewHasText(
            R.id.registrationStatusText,
            R.string.registration_everything_is_working_ok
        )
    }

    fun checkFinialisingSetupMessageIsShown() {
        checkViewHasText(R.id.registrationStatusText, R.string.registration_finalising_setup)
    }

    fun waitForWorkingOkMessage(string: String) {
        waitForText(string, timeoutInMs = 20000)
    }
}
