/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_at_risk.feelUnwell
import kotlinx.android.synthetic.main.activity_at_risk.latest_advice_exposed
import kotlinx.android.synthetic.main.activity_at_risk.registrationPanel
import kotlinx.android.synthetic.main.activity_ok.notificationPanel
import kotlinx.android.synthetic.main.activity_status.statusView
import kotlinx.android.synthetic.main.banner.toolbar_info
import kotlinx.android.synthetic.main.status_footer_view.nhsServiceFooter
import kotlinx.android.synthetic.main.status_footer_view.reference_link_card
import kotlinx.android.synthetic.main.status_footer_view.workplace_guidance_card
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.interstitials.CurrentAdviceActivity
import uk.nhs.nhsx.sonar.android.app.interstitials.WorkplaceGuidanceActivity
import uk.nhs.nhsx.sonar.android.app.notifications.cancelStatusNotification
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeActivity
import uk.nhs.nhsx.sonar.android.app.status.widgets.StatusView
import uk.nhs.nhsx.sonar.android.app.status.widgets.createTestResultDialog
import uk.nhs.nhsx.sonar.android.app.status.widgets.handleTestResult
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_NHS_LOCAL_SUPPORT
import uk.nhs.nhsx.sonar.android.app.util.cardColourInversion
import uk.nhs.nhsx.sonar.android.app.util.openAppSettings
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import javax.inject.Inject

class AtRiskActivity : BaseActivity() {

    @Inject
    protected lateinit var userStateStorage: UserStateStorage

    @Inject
    protected lateinit var userInbox: UserInbox

    private lateinit var testResultDialog: BottomDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_at_risk)
        BluetoothService.start(this)

        registrationPanel.setState(RegistrationState.Complete)

        setStatusView()

        latest_advice_exposed.setOnClickListener {
            CurrentAdviceActivity.start(this)
        }

        feelUnwell.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }

        nhsServiceFooter.setOnClickListener {
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

        notificationPanel.setOnClickListener {
            openAppSettings()
        }

        testResultDialog = createTestResultDialog(this, userInbox)
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

        notificationPanel.isVisible =
            !NotificationManagerCompat.from(this).areNotificationsEnabled()

        handleTestResult(userInbox, testResultDialog)
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        notificationPanel.cardColourInversion(inversionModeEnabled)

        latest_advice_exposed.cardColourInversion(inversionModeEnabled)
        feelUnwell.cardColourInversion(inversionModeEnabled)

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
