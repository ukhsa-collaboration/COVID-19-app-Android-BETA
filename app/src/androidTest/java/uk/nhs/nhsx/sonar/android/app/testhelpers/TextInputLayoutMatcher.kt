package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun hasTextInputLayoutErrorText(@StringRes expectedErrorResource: Int): Matcher<View> =
    object : TypeSafeMatcher<View>() {

        override fun describeTo(description: Description) {
        }

        override fun matchesSafely(item: View?): Boolean {
            if (item !is TextInputLayout) return false
            val error = item.error?.toString() ?: return false
            val expectedErrorText = item.context.getString(expectedErrorResource)
            return expectedErrorText == error
        }
    }
