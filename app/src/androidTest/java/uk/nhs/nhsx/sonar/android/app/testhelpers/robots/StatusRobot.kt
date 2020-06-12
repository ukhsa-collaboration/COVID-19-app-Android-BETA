package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import android.view.View
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.ExposedSymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewContainsText
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText
import uk.nhs.nhsx.sonar.android.app.testhelpers.waitForText
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import kotlin.reflect.KClass

class StatusRobot {

    fun <T : UserState> checkActivityIsDisplayed(userState: KClass<T>) {
        val title: Int = when (userState) {
            DefaultState::class -> R.string.status_default_title
            ExposedState::class -> R.string.status_exposed_title
            SymptomaticState::class -> R.string.status_symptomatic_title
            ExposedSymptomaticState::class -> R.string.status_symptomatic_title
            PositiveState::class -> R.string.status_positive_test_title
            else -> throw IllegalArgumentException("StatusRobot: not able to match title: $userState")
        }

        waitForText(title, 6_000)
        checkStatusTitle(title)
    }

    fun swipeToBottom() {
        onView(withId(R.id.root_container)).perform(ViewActions.swipeUp())
    }

    fun waitForRegistrationToComplete() {
        // job retries after at least 10 seconds
        waitForText(R.string.registration_everything_is_working_ok, timeoutInMs = 20000)
    }

    fun clickCurrentAdviceCard() {
        onView(withId(R.id.readLatestAdvice)).perform(click())
    }

    fun clickBookTestCard() {
        onView(withId(R.id.bookTest)).perform(click())
    }

    fun clickNotFeelingWellCard() {
        onView(withId(R.id.feelUnwell)).perform(scrollTo(), click())
    }

    fun clickEnableNotifications() {
        onView(withId(R.id.notificationPanel)).perform(click())
    }

    fun checkFinalisingSetup() {
        checkViewHasText(R.id.registrationStatusText, R.string.registration_finalising_setup)
        verifyFeelUnwellCard(not(isEnabled()))
    }

    fun checkAppIsWorking() {
        checkViewHasText(R.id.registrationStatusText, R.string.registration_everything_is_working_ok)
    }

    private fun checkStatusTitle(@StringRes stringRes: Int) {
        checkViewHasText(R.id.statusTitle, stringRes)
    }

    fun checkStatusDescription(state: UserState) {
        val expected = "Please isolate until ${state.until().toUiFormat()}"
        checkViewContainsText(R.id.statusDescription, expected)
    }

    fun checkStatusDescriptionIsNotDisplayed() {
        onView(withId(R.id.statusDescription)).check(matches(not(isDisplayed())))
    }

    fun checkCurrentAdviceCardIsDisplayed() {
        onView(withId(R.id.readLatestAdvice)).check(matches(isDisplayed()))
    }

    fun checkInformationAboutTestingIsDisplayed() {
        onView(withId(R.id.reference_link_card)).check(matches(isDisplayed()))
    }

    fun checkWorkplaceGuidanceIsDisplayed() {
        onView(withId(R.id.workplace_guidance_card)).check(matches(isDisplayed()))
    }

    fun checkNhsServicesLinkIsDisplayed() {
        onView(withId(R.id.nhsServiceFooter)).check(matches(isDisplayed()))
    }

    fun checkBookVirusTestCardIsDisplayed() {
        onView(withId(R.id.bookTest)).check(matches(isDisplayed()))
    }

    fun checkBookVirusTestCardIsNotDisplayed() {
        onView(withId(R.id.bookTest)).check(matches(not(isDisplayed())))
    }

    fun checkFeelUnwellIsNotDisplayed() {
        verifyFeelUnwellCard(not(isDisplayed()))
    }

    fun checkFeelUnwellIsDisplayed() {
        verifyFeelUnwellCard(isDisplayed())
        verifyFeelUnwellCard(isEnabled())
    }

    fun checkFeelUnwellIsDisabled() {
        verifyFeelUnwellCard(isDisplayed())
        verifyFeelUnwellCard(not(isEnabled()))
    }

    fun checkEnableNotificationsIsDisplayed() {
        onView(withId(R.id.notificationPanel)).check(matches(isDisplayed()))
    }

    fun checkEnableNotificationsIsNotDisplayed() {
        onView(withId(R.id.notificationPanel)).check(matches(not(isDisplayed())))
    }

    private fun verifyFeelUnwellCard(matcher: Matcher<View>) {
        onView(withId(R.id.feelUnwell)).check(matches(matcher))
    }
}
