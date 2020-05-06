/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceStringProvider
import javax.inject.Inject

class ActivationCodeProvider @Inject constructor(context: Context) :
    SharedPreferenceStringProvider(
        context,
        preferenceName = "activationCode",
        preferenceKey = "ACTIVATION_CODE"
    )
