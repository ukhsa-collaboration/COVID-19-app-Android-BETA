package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText

class TabletNotSupportedRobot {

    fun checkScreenIsDisplayed() {
        checkToolbar()
        checkTitle()
        checkDescription()
        checkBottomUrlIsDisplayed()
    }

    private fun checkToolbar() {
        onView(withId(R.id.nhsLogo)).check(matches(isDisplayed()))
        checkViewHasText(R.id.nhsLogoName, R.string.app_title)
    }

    private fun checkTitle() {
        checkViewHasText(R.id.edgeCaseTitle, R.string.tablet_support_title)
    }

    private fun checkDescription() {
        checkViewHasText(R.id.edgeCaseText, R.string.tablet_support_description)
    }

    private fun checkBottomUrlIsDisplayed() {
        checkViewHasText(R.id.tabletInformationUrl, R.string.tablet_information_url)
        onView(withId(R.id.tabletInformationUrl)).check(matches(isClickable()))
    }
}
