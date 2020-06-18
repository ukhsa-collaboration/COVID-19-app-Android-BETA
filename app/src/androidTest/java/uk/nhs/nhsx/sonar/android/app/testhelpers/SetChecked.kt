/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.view.View
import android.widget.Checkable
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers
import org.hamcrest.Description

class SetChecked(private val checked: Boolean) : ViewAction {

    override fun getConstraints(): BaseMatcher<View> {
        return object : BaseMatcher<View>() {
            override fun matches(item: Any): Boolean {
                return CoreMatchers.isA(Checkable::class.java).matches(item)
            }

            override fun describeMismatch(item: Any, mismatchDescription: Description) {}

            override fun describeTo(description: Description) {}
        }
    }

    override fun getDescription(): String {
        return ""
    }

    override fun perform(uiController: UiController, view: View) {
        val checkableView = view as Checkable
        checkableView.isChecked = checked
    }

    companion object {
        fun setChecked(checked: Boolean) = SetChecked(checked)
    }
}
