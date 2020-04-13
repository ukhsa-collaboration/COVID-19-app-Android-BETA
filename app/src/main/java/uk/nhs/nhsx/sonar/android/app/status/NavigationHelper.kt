package uk.nhs.nhsx.sonar.android.app.status

import android.app.Activity
import uk.nhs.nhsx.sonar.android.app.status.CovidStatus.OK
import uk.nhs.nhsx.sonar.android.app.status.CovidStatus.POTENTIAL
import uk.nhs.nhsx.sonar.android.app.status.CovidStatus.RED

fun Activity.navigateTo(status: CovidStatus) {
    if (status == OK && this !is OkActivity) {
        OkActivity.start(this)
        finish()
    }

    if (status == POTENTIAL && this !is AtRiskActivity) {
        AtRiskActivity.start(this)
        finish()
    }

    if (status == RED && this !is IsolateActivity) {
        IsolateActivity.start(this)
        finish()
    }
}
