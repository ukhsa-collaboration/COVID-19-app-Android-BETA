package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewContainsText
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat

class CurrentAdviceRobot {

    fun checkActivityIsDisplayed() {
        Espresso.onView(withId(R.id.current_advice_title))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun checkCorrectStateIsDisplay(userState: UserState) {
        userState.until()?.let { expiryDate ->
            checkViewContainsText(R.id.current_advice_desc, expiryDate.toUiFormat())
        } ?: checkViewHasText(R.id.current_advice_desc, R.string.current_advice_desc_simple)
    }
}
