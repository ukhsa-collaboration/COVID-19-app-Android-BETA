/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import kotlinx.android.synthetic.main.activity_at_risk.latest_advice_exposed
import kotlinx.android.synthetic.main.activity_at_risk.registrationPanel
import kotlinx.android.synthetic.main.activity_at_risk.status_not_feeling_well
import kotlinx.android.synthetic.main.activity_isolate.statusView
import kotlinx.android.synthetic.main.banner.toolbar_info
import kotlinx.android.synthetic.main.status_footer_view.nhs_service
import kotlinx.android.synthetic.main.status_footer_view.reference_link_card
import kotlinx.android.synthetic.main.status_footer_view.workplace_guidance_card
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.interstitials.CurrentAdviceActivity
import uk.nhs.nhsx.sonar.android.app.notifications.cancelStatusNotification
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeActivity
import uk.nhs.nhsx.sonar.android.app.status.widgets.StatusView
import uk.nhs.nhsx.sonar.android.app.tests.WorkplaceGuidanceActivity
import uk.nhs.nhsx.sonar.android.app.interstitials.WorkplaceGuidanceActivity
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_NHS_LOCAL_SUPPORT
import uk.nhs.nhsx.sonar.android.app.util.cardColourInversion
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

        setStatusView()

        status_not_feeling_well.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }

        registrationPanel.setState(RegistrationState.Complete)

        latest_advice_exposed.setOnClickListener {
            CurrentAdviceActivity.start(this)
        }

        nhs_service.setOnClickListener {
            openUrl(URL_NHS_LOCAL_SUPPORT)
        }

        toolbar_info.setOnClickListener {
            openUrl(URL_INFO)
        }

        workplace_guidance_card.setOnClickListener {
            WorkplaceGuidanceActivity.start(this)
        }

        reference_link_card.setOnClickListener {
            ReferenceCodeActivity.start(this)
        }
    }

    private fun setStatusView() {
        val statusDescription = buildSpannedString {
            append(getString(R.string.follow_until))
            bold {
                append("  ${userStateStorage.get().until().toUiFormat()}")
            }
        }
        statusView.setup(
            StatusView.Configuration(
                title = getString(R.string.status_exposed_title),
                description = statusDescription,
                statusColor = StatusView.Color.ORANGE
            )
        )
    }

    override fun onResume() {
        super.onResume()

        val oldState = userStateStorage.get()
        if (oldState is ExposedState) cancelStatusNotification()
        navigateTo(oldState)

        val newState = UserStateTransitions.expireExposedState(oldState)
        userStateStorage.set(newState)
        navigateTo(newState)
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        latest_advice_exposed.cardColourInversion(inversionModeEnabled)
        status_not_feeling_well.cardColourInversion(inversionModeEnabled)

        workplace_guidance_card.cardColourInversion(inversionModeEnabled)
        reference_link_card.cardColourInversion(inversionModeEnabled)
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
