package uk.nhs.nhsx.sonar.android.app.status

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.sonar.android.app.R

class AtRiskRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.statusTitle))
            .check(matches(withText(R.string.status_exposed_title)))
            .check(matches(isDisplayed()))
    }
}
