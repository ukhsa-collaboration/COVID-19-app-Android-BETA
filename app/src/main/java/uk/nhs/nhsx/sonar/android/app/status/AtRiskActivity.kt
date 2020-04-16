/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_at_risk.latest_advice_amber
import kotlinx.android.synthetic.main.activity_at_risk.status_not_feeling_well
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.util.LATEST_ADVICE_URL
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

class AtRiskActivity : BaseActivity() {

    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        BluetoothService.start(this)

        setContentView(R.layout.activity_at_risk)

        status_not_feeling_well.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }

        latest_advice_amber.setOnClickListener {
            openUrl(LATEST_ADVICE_URL)
        }
    }

    override fun onResume() {
        super.onResume()

        navigateTo(statusStorage.get())
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        fun getIntent(context: Context) =
            Intent(context, AtRiskActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
