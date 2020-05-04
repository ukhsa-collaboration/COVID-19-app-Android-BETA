/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose.review.spinner

import android.widget.Spinner
import androidx.appcompat.content.res.AppCompatResources.getDrawable

fun Spinner.setError() {
    background =
        getDrawable(context, uk.nhs.nhsx.sonar.android.app.R.drawable.spinner_background_error)
}

fun Spinner.setInitial() {
    background =
        getDrawable(context, uk.nhs.nhsx.sonar.android.app.R.drawable.spinner_background_normal)
}

fun Spinner.setFocused() {
    background =
        getDrawable(context, uk.nhs.nhsx.sonar.android.app.R.drawable.spinner_background_focused)
}
