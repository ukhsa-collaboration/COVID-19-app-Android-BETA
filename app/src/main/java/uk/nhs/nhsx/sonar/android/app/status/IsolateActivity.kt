/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_isolate.book_test_card
import kotlinx.android.synthetic.main.activity_isolate.latest_advice_symptomatic
import kotlinx.android.synthetic.main.activity_isolate.registrationPanel
import kotlinx.android.synthetic.main.banner.toolbar_info
import kotlinx.android.synthetic.main.status_footer_view.nhs_service
import kotlinx.android.synthetic.main.status_footer_view.reference_link_card
import kotlinx.android.synthetic.main.status_footer_view.workplace_guidance_card
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.notifications.CheckInReminderNotification
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeActivity
import uk.nhs.nhsx.sonar.android.app.status.widgets.StatusScreenFactory
import uk.nhs.nhsx.sonar.android.app.interstitials.CurrentAdviceActivity
import uk.nhs.nhsx.sonar.android.app.interstitials.WorkplaceGuidanceActivity
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_NHS_LOCAL_SUPPORT
import uk.nhs.nhsx.sonar.android.app.util.cardColourInversion
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.showExpanded
import javax.inject.Inject

class IsolateActivity : BaseActivity() {

    @Inject
    protected lateinit var userStateStorage: UserStateStorage

    @Inject
    protected lateinit var checkInReminderNotification: CheckInReminderNotification

    private lateinit var updateSymptomsDialog: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_isolate)
        BluetoothService.start(this)

        registrationPanel.setState(RegistrationState.Complete)

        val statusScreen = StatusScreenFactory.from(userStateStorage.get())
        statusScreen.setStatusScreen(this)

        latest_advice_symptomatic.setOnClickListener {
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

        setUpdateSymptomsDialog()

        reference_link_card.setOnClickListener {
            ReferenceCodeActivity.start(this)
        }
    }

    private fun setUpdateSymptomsDialog() {
        val configuration = BottomDialogConfiguration(
            isHideable = false,
            titleResId = R.string.status_today_feeling,
            textResId = R.string.update_symptoms_prompt,
            firstCtaResId = R.string.update_my_symptoms,
            secondCtaResId = R.string.no_symptoms
        )
        updateSymptomsDialog = BottomDialog(
            this, configuration,
            onCancel = {
                finish()
            },
            onFirstCtaClick = {
                DiagnoseTemperatureActivity.start(this)
            },
            onSecondCtaClick = {
                userStateStorage.set(DefaultState)
                navigateTo(userStateStorage.get())
            }
        )
    }

    override fun onResume() {
        super.onResume()

        val state = userStateStorage.get()
        navigateTo(state)

        if (state.hasExpired()) {
            updateSymptomsDialog.showExpanded()
            checkInReminderNotification.hide()
        } else {
            updateSymptomsDialog.dismiss()
        }
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        latest_advice_symptomatic.cardColourInversion(inversionModeEnabled)
        book_test_card.cardColourInversion(inversionModeEnabled)

        workplace_guidance_card.cardColourInversion(inversionModeEnabled)
        reference_link_card.cardColourInversion(inversionModeEnabled)
    }

    override fun onPause() {
        super.onPause()
        updateSymptomsDialog.dismiss()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, IsolateActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
