package uk.nhs.nhsx.sonar.android.app.edgecases

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText

class DeviceNotSupportedRobot {

    fun checkToolbar() {
        onView(withId(R.id.nhsLogo)).check(matches(isDisplayed()))
        checkViewHasText(R.id.nhsLogoName, R.string.app_title)
    }

    fun checkTitle() {
        checkViewHasText(R.id.edgeCaseTitle, R.string.device_not_supported_title)
    }

    fun checkDescription() {
        checkViewHasText(R.id.edgeCaseText, R.string.device_not_supported_rationale)
    }

    fun checkBottomUrlIsDisplayed() {
        checkViewHasText(R.id.nhsServicesUrl, R.string.nhs_online_service)
        onView(withId(R.id.nhsServicesUrl)).check(matches(isClickable()))
    }
}
