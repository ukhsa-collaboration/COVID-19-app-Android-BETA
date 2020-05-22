package uk.nhs.nhsx.sonar.android.app.testhelpers

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import uk.nhs.nhsx.sonar.android.app.SonarApplication
import kotlin.test.fail

fun checkViewHasText(@IdRes viewId: Int, @StringRes stringId: Int) {
    onView(withId(viewId)).check(matches(withText(stringId)))
}

fun waitForText(@StringRes stringId: Int, timeoutInMs: Long = 500) {
    val resources = ApplicationProvider.getApplicationContext<SonarApplication>().resources
    waitForText(resources.getString(stringId), timeoutInMs)
}

fun waitForText(text: String, timeoutInMs: Long = 500) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    device.wait(Until.findObject(By.text(text)), timeoutInMs)
        ?: fail("Timed out waiting for text: $text")
}
