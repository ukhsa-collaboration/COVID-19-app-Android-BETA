/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_at_risk.follow_until
import kotlinx.android.synthetic.main.activity_at_risk.latest_advice_amber
import kotlinx.android.synthetic.main.activity_at_risk.status_not_feeling_well
import kotlinx.android.synthetic.main.status_footer_view.nhs_service
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.util.LATEST_ADVICE_URL
import uk.nhs.nhsx.sonar.android.app.util.NHS_SUPPORT_PAGE
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import javax.inject.Inject

class AtRiskActivity : BaseActivity() {

    @Inject
    protected lateinit var stateStorage: StateStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        BluetoothService.start(this)
        setContentView(R.layout.activity_at_risk)

        follow_until.text = getString(R.string.follow_until, stateStorage.get().until.toUiFormat())

        status_not_feeling_well.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }

        latest_advice_amber.setOnClickListener {
            openUrl(LATEST_ADVICE_URL)
        }

        nhs_service.setOnClickListener {
            openUrl(NHS_SUPPORT_PAGE)
        }
    }

    override fun onResume() {
        super.onResume()

        val state = stateStorage.get()

        navigateTo(state)

        if (state.hasExpired()) {
            DefaultState().let {
                stateStorage.update(it)
                navigateTo(it)
            }
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        fun getIntent(context: Context) =
            Intent(context, AtRiskActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
