package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText

class EdgeCaseRobot {

    fun clickTakeAction() {
        onView(withId(R.id.takeActionButton)).perform(click())
    }

    fun checkTitle(@StringRes titleId: Int) {
        checkViewHasText(R.id.edgeCaseTitle, titleId)
    }
}
