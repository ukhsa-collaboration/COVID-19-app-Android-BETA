/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceSerializingProvider
import javax.inject.Inject

class UserStateStorage @Inject constructor(context: Context) :
    SharedPreferenceSerializingProvider<UserState>(
        context,
        preferenceName = "user_state_storage",
        preferenceKey = "user_state",
        serialize = UserStateSerialization::serialize,
        deserialize = UserStateSerialization::deserialize
    )
