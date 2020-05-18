/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import kotlinx.android.synthetic.main.activity_at_risk.follow_until
import kotlinx.android.synthetic.main.activity_at_risk.latest_advice_amber
import kotlinx.android.synthetic.main.activity_at_risk.registrationPanel
import kotlinx.android.synthetic.main.activity_at_risk.status_not_feeling_well
import kotlinx.android.synthetic.main.banner.toolbar_info
import kotlinx.android.synthetic.main.status_footer_view.medical_workers_card
import kotlinx.android.synthetic.main.status_footer_view.nhs_service
import kotlinx.android.synthetic.main.status_footer_view.reference_link_card
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeActivity
import uk.nhs.nhsx.sonar.android.app.tests.WorkplaceGuidanceActivity
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_LATEST_ADVICE_AMBER
import uk.nhs.nhsx.sonar.android.app.util.URL_SUPPORT_AMBER
import uk.nhs.nhsx.sonar.android.app.util.cardColourInversion
import uk.nhs.nhsx.sonar.android.app.util.cancelStatusNotification
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import javax.inject.Inject

class AtRiskActivity : BaseActivity() {

    @Inject
    protected lateinit var userStateStorage: UserStateStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        BluetoothService.start(this)
        setContentView(R.layout.activity_at_risk)

        follow_until.text = buildSpannedString {
            append(getString(R.string.follow_until))
            bold {
                append("  ${userStateStorage.get().until().toUiFormat()}")
            }
        }

        status_not_feeling_well.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }

        registrationPanel.setState(RegistrationState.Complete)

        latest_advice_amber.setOnClickListener {
            openUrl(URL_LATEST_ADVICE_AMBER)
        }

        nhs_service.setOnClickListener {
            openUrl(URL_SUPPORT_AMBER)
        }

        toolbar_info.setOnClickListener {
            openUrl(URL_INFO)
        }

        medical_workers_card.setOnClickListener {
            WorkplaceGuidanceActivity.start(this)
        }

        reference_link_card.setOnClickListener {
            ReferenceCodeActivity.start(this)
        }
    }

    override fun onResume() {
        super.onResume()

        val oldState = userStateStorage.get()
        if (oldState is AmberState) cancelStatusNotification()
        navigateTo(oldState)

        val newState = UserStateTransitions.expireAmberState(oldState)
        userStateStorage.set(newState)
        navigateTo(newState)
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        status_not_feeling_well.cardColourInversion(inversionModeEnabled)
        medical_workers_card.cardColourInversion(inversionModeEnabled)
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
