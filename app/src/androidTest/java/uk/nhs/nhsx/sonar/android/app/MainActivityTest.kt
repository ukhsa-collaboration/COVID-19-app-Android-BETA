package uk.nhs.nhsx.sonar.android.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun displaysCorrectCopy() {
        onView(withId(R.id.title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.no_longer_in_use_title)))

        onView(withId(R.id.description))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.no_longer_in_use_description)))

        onView(withId(R.id.support))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.supporting_nhs)))
    }

    @Test
    fun containsClickableUrls() {
        onView(withId(R.id.feelUnwellUrl))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
            .check(matches(withText(R.string.feel_unwell)))

        onView(withId(R.id.uninstallUrl))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
            .check(matches(withText(R.string.uninstall)))

        onView(withId(R.id.aboutUrl))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
            .check(matches(withText(R.string.about)))
    }
}
