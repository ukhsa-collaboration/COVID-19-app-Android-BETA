/*
 * Copyright © 2020 NHSX. All rights reserved.
 */
package uk.nhs.nhsx.sonar.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.sonar.android.app.R

class ReferenceCodeRobot {

    fun checkReferenceCodeIs(code: String) {
        onView(withId(R.id.reference_code)).check(matches(withText(code)))
    }
}
