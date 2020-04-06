package com.example.colocate.status

import android.app.Activity
import com.example.colocate.AtRiskActivity
import com.example.colocate.OkActivity
import com.example.colocate.isolate.IsolateActivity
import com.example.colocate.status.CovidStatus.OK
import com.example.colocate.status.CovidStatus.POTENTIAL
import com.example.colocate.status.CovidStatus.RED

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
