/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.app.Activity
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.AT_RISK
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.OK
import uk.nhs.nhsx.sonar.android.app.status.widgets.StatusLayoutFactory

fun Activity.navigateTo(state: UserState) {
    when (state.displayState()) {
        OK, ISOLATE, AT_RISK -> {
            if (this is StatusActivity) {
                statusLayout = StatusLayoutFactory.from(userStateStorage.get())
                statusLayout.refreshStatusLayout(this)
                return
            }

            StatusActivity.start(this)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
