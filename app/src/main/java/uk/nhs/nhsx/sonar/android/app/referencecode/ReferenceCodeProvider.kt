/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import android.content.Context
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceSerializingProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceCodeProvider @Inject constructor(context: Context) :
    SharedPreferenceSerializingProvider<ReferenceCode?>(
        context,
        preferenceName = "referenceCode",
        preferenceKey = "REFERENCE_CODE",
        serialize = { it?.value },
        deserialize = { code -> code?.let { ReferenceCode(it) } }
    )
