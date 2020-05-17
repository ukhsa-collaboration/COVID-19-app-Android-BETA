package uk.nhs.nhsx.sonar.android.app.robots

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlin.test.fail

abstract class ScreenRobot {

    protected fun checkViewHasText(@IdRes viewId: Int, @StringRes stringId: Int) {
        onView(withId(viewId)).check(matches(withText(stringId)))
    }

    protected fun checkViewHasText(@IdRes viewId: Int, text: String) {
        onView(withId(viewId)).check(matches(withText(text)))
    }

    protected fun checkViewWithIdIsDisplayed(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    protected fun checkViewWithIdIsNotDisplayed(@IdRes viewId: Int) {
        onView(withId(viewId)).check(doesNotExist())
    }

    protected fun clickOnView(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(click())
    }

    protected fun scrollAndClickOnView(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(scrollTo(), click())
    }

    protected fun typeTextInEditText(@IdRes viewId: Int, text: String) {
        onView(withId(viewId)).perform(typeText("E1"))
    }

    protected fun waitForText(text: String, timeoutInMs: Long = 500) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.wait(Until.findObject(By.text(text)), timeoutInMs)
            ?: fail("Timed out waiting for text: $text")
    }

    protected fun waitForText(@StringRes stringId: Int, timeoutInMs: Long = 500) {
        val text = getString(stringId)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.wait(Until.findObject(By.text(text)), timeoutInMs)
            ?: fail("Timed out waiting for text: $text")
    }

    protected fun waitUntilCannotFindText(@StringRes stringId: Int, timeoutInMs: Long = 500) {
        val text = getString(stringId)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.gone(By.text(text)), timeoutInMs)
    }

    private fun getString(@StringRes stringId: Int): String {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        return targetContext.resources.getString(stringId)
    }
}
