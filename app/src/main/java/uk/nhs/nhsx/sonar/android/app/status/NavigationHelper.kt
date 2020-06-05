/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.app.Activity

fun Activity.startStatusActivity() {
    StatusActivity.start(this)
    finish()
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}
