/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.app.Activity

fun Activity.navigateTo(state: UserState) {
    when {
        state.isOk() && this !is OkActivity -> {
            OkActivity.start(this)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        state.isAtRisk() && this !is AtRiskActivity -> {
            AtRiskActivity.start(this)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        state.shouldIsolate() && this !is IsolateActivity -> {
            IsolateActivity.start(this)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
