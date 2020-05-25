/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.inbox

import android.content.Context
import javax.inject.Inject

class UserInbox @Inject constructor(context: Context) {
    fun hasTestResult(): Boolean = false

    fun dismissTestResult(): Unit = Unit
}
