/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceStringProvider
import javax.inject.Inject

class PostCodeProvider @Inject constructor(context: Context) :
    SharedPreferenceStringProvider(
        context,
        preferenceName = "postCode",
        preferenceKey = "POST_CODE"
    )
