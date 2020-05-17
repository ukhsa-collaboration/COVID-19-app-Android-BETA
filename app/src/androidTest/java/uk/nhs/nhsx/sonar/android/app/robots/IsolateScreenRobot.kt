package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onIsolateScreen(func: IsolateScreenRobot.() -> Unit) = IsolateScreenRobot().apply(func)

class IsolateScreenRobot : ScreenRobot() {

    fun checkIsolateActivityIsShown() {
        checkViewWithIdIsDisplayed(R.id.status_red)
    }

    fun clickOnReferenceCodeLink() {
        scrollAndClickOnView(R.id.reference_link_card)
    }
}
