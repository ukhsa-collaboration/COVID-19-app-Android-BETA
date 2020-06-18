package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult

class BottomDialogRobot {

    fun checkRecoveryDialogIsDisplayed() {
        onView(withId(R.id.bottomDialogTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomDialogText)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomDialogFirstCta)).check(matches(not(isDisplayed())))
        onView(withId(R.id.bottomDialogSecondCta)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomDialogTitle)).check(matches(withText(R.string.recovery_dialog_title)))
        onView(withId(R.id.bottomDialogText)).check(matches(withText(R.string.recovery_dialog_description)))
        onView(withId(R.id.bottomDialogSecondCta)).check(matches(withText(R.string.close)))
    }

    fun checkUpdateSymptomsDialogIsDisplayed() {
        onView(withId(R.id.bottomDialogTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomDialogText)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomDialogFirstCta)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomDialogSecondCta)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomDialogTitle)).check(matches(withText(R.string.status_today_feeling)))
        onView(withId(R.id.bottomDialogText)).check(matches(withText(R.string.update_symptoms_prompt)))
        onView(withId(R.id.bottomDialogFirstCta)).check(matches(withText(R.string.update_my_symptoms)))
        onView(withId(R.id.bottomDialogSecondCta)).check(matches(withText(R.string.no_symptoms)))
    }

    fun checkAdviceDialogIsDisplayed() {
        onView(withId(R.id.bottomDialogTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.advice_dialog_title)))

        onView(withId(R.id.bottomDialogText))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.advice_dialog_description)))

        onView(withId(R.id.bottomDialogFirstCta))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.bottomDialogSecondCta))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.close)))
    }

    fun checkTestResultDialogIsDisplayed(testResult: TestResult) {
        when (testResult) {
            TestResult.NEGATIVE -> checkNegativeTestResultDialog()
            TestResult.POSITIVE -> checkPositiveTestResultDialog()
            TestResult.INVALID -> checkInvalidTestResultDialog()
        }
    }

    private fun checkNegativeTestResultDialog() {
        onView(withId(R.id.bottomDialogTitle))
            .check(matches(withText(R.string.negative_test_result_title)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.bottomDialogText))
            .check(matches(withText(R.string.negative_test_result_description)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.bottomDialogFirstCta))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.test_result_meaning_title)))

        onView(withId(R.id.bottomDialogSecondCta))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.close)))
    }

    private fun checkPositiveTestResultDialog() {
        onView(withId(R.id.bottomDialogTitle))
            .check(matches(withText(R.string.positive_test_result_title)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.bottomDialogText))
            .check(matches(withText(R.string.positive_test_result_description)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.bottomDialogFirstCta))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.bottomDialogSecondCta))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.close)))
    }

    private fun checkInvalidTestResultDialog() {
        onView(withId(R.id.bottomDialogTitle))
            .check(matches(withText(R.string.invalid_test_result_title)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.bottomDialogText))
            .check(matches(withText(R.string.invalid_test_result_description)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.bottomDialogFirstCta))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.bottomDialogSecondCta))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.close)))
    }

    fun clickFirstCtaButton() {
        onView(withId(R.id.bottomDialogFirstCta)).perform(click())
    }

    fun clickSecondCtaButton() {
        onView(withId(R.id.bottomDialogSecondCta)).perform(click())
    }

    fun checkBottomDialogIsNotDisplayed() {
        onView(withId(R.id.bottomSheet)).check(doesNotExist())
    }
}
