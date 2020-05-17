package uk.nhs.nhsx.sonar.android.app.robots

import uk.nhs.nhsx.sonar.android.app.R

fun onIsolateBottomSheetView(func: IsolateBottomSheetViewRobot.() -> Unit) = IsolateBottomSheetViewRobot().apply(func)

class IsolateBottomSheetViewRobot : ScreenRobot() {

    fun checkIsolateActivityPopUpIsShown() {
        checkViewWithIdIsDisplayed(R.id.bottom_sheet_isolate)
    }

    fun clickOnHaveSymptoms() {
        clickOnView(R.id.have_symptoms)
    }
}
