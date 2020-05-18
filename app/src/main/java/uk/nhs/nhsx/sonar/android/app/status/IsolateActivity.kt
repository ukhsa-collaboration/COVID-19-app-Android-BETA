/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_isolate.book_test_card
import kotlinx.android.synthetic.main.activity_isolate.follow_until
import kotlinx.android.synthetic.main.activity_isolate.latest_advice_red
import kotlinx.android.synthetic.main.activity_isolate.registrationPanel
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
import uk.nhs.nhsx.sonar.android.app.tests.ApplyForTestActivity
import uk.nhs.nhsx.sonar.android.app.tests.WorkplaceGuidanceActivity
import uk.nhs.nhsx.sonar.android.app.util.CheckInReminderNotification
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_LATEST_ADVICE_RED
import uk.nhs.nhsx.sonar.android.app.util.URL_SUPPORT_RED
import uk.nhs.nhsx.sonar.android.app.util.cardColourInversion
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.showExpanded
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
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

        val state = userStateStorage.get()

        registrationPanel.setState(RegistrationState.Complete)
        follow_until.text = buildSpannedString {
            bold {
                append(getString(R.string.follow_until_red_pre, state.until().toUiFormat()))
            }
            append(" ${getString(R.string.follow_until_red)}")
        }

        latest_advice_red.setOnClickListener {
            openUrl(URL_LATEST_ADVICE_RED)
        }

        nhs_service.setOnClickListener {
            openUrl(URL_SUPPORT_RED)
        }

        toolbar_info.setOnClickListener {
            openUrl(URL_INFO)
        }

        book_test_card.setOnClickListener {
            ApplyForTestActivity.start(this)
        }

        medical_workers_card.setOnClickListener {
            WorkplaceGuidanceActivity.start(this)
        }

        setUpdateSymptomsDialog()

        reference_link_card.setOnClickListener {
            ReferenceCodeActivity.start(this)
        }
    }

    private fun setUpdateSymptomsDialog() {
        updateSymptomsDialog = BottomSheetDialog(this, R.style.PersistentBottomSheet)
        updateSymptomsDialog.setContentView(
            layoutInflater.inflate(
                R.layout.bottom_sheet_isolate,
                null
            )
        )
        updateSymptomsDialog.behavior.isHideable = false

        updateSymptomsDialog.findViewById<Button>(R.id.no_symptoms)?.setOnClickListener {
            userStateStorage.set(DefaultState)
            navigateTo(userStateStorage.get())
            updateSymptomsDialog.cancel()
        }

        updateSymptomsDialog.setOnCancelListener {
            finish()
        }

        updateSymptomsDialog.findViewById<Button>(R.id.have_symptoms)?.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }
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
        book_test_card.cardColourInversion(inversionModeEnabled)
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
